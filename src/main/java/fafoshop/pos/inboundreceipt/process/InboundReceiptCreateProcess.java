package fafoshop.pos.inboundreceipt.process;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.MessageUtility;
import fafoshop.common.utility.SeqNoUtility;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptCreateRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptCreateResponse;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptItemDto;

/**
 * Tạo phiếu nhập hàng (màn hình Nhập hàng - quét liên tục kiểu POS, xem trao
 * đổi thiết kế). Phạm vi lần này CHỈ ghi nhận thực nhận (giống comment trên
 * bảng inbound_receipt trong db/schema.sql - không có luồng lập kế hoạch nhập
 * hàng), nên planned_qty được ghi lại BẰNG actual_qty (không có bước lập kế
 * hoạch riêng để so sánh chênh lệch). Lưu phiếu là CỘNG THẲNG vào bảng stock
 * ngay trong cùng transaction, không qua bước duyệt riêng - đúng tinh thần
 * "cơ động, tiện lợi" đã thống nhất, sai thì lập phiếu điều chỉnh bù sau.
 */
public class InboundReceiptCreateProcess extends AbstractProcess {

	private static final String PRG_CD = "INBND_CRT";

	private static final String SEQ_PREFIX = "PN";

	/** quality_code mặc định - màn hình chưa hỗ trợ chọn phẩm cấp/tình trạng hàng khác "hàng thường". */
	private static final String DEFAULT_QUALITY_CODE = "01";

	public InboundReceiptCreateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new InboundReceiptCreateResponse();
	}

	@Override
	protected String getFuncId() {
		return "INBND_CRT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		InboundReceiptCreateRequest req = (InboundReceiptCreateRequest) request;
		InboundReceiptCreateResponse res = (InboundReceiptCreateResponse) response;

		validateItems(req.items);
		List<Date> expiryDates = new ArrayList<>();
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (InboundReceiptItemDto item : req.items) {
			validateItemExists(dba, item.productCode);
			Date expiryDate = parseExpiryDate(item.expiryDate);
			expiryDates.add(expiryDate);
			totalAmount = totalAmount.add(item.unitCost.multiply(BigDecimal.valueOf(item.quantity)));
		}

		validateSupplierExists(dba, req.supplierCode);
		validateNote(req.note);
		validateEinvoiceFields(req);
		Date einvoiceIssueDate = parseEinvoiceIssueDate(req.einvoiceIssueDate);

		String branchCode = getUserBranchCode(dba, req.accessInfo.userCode);
		String receiptNo = SeqNoUtility.generate(dba, SEQ_PREFIX, req.accessInfo.userCode, PRG_CD);
		Date today = new Date(System.currentTimeMillis());

		insertReceiptHeader(dba, receiptNo, branchCode, req, today, einvoiceIssueDate, req.accessInfo.userCode);
		insertReceiptItems(dba, receiptNo, branchCode, req.items, expiryDates, req.accessInfo.userCode);
		upsertStock(dba, branchCode, req.items, expiryDates, req.accessInfo.userCode);
		updateProductPrices(dba, req.items, req.accessInfo.userCode);

		res.receiptNo = receiptNo;
		res.totalAmount = totalAmount;
		return res;
	}

	private void validateItems(List<InboundReceiptItemDto> items) throws ProcessCheckErrorException {
		if (items == null || items.isEmpty()) {
			throwError("ME000090");
		}
		for (InboundReceiptItemDto item : items) {
			if (item.productCode == null || item.productCode.trim().isEmpty()) {
				throwError("ME000061");
			}
			if (item.quantity == null || item.quantity <= 0) {
				throwError("ME000091");
			}
			if (item.unitCost == null || item.unitCost.signum() < 0) {
				throwError("ME000092");
			}
			if (item.price == null || item.price.signum() < 0) {
				throwError("ME000067");
			}
		}
	}

	private Date parseExpiryDate(String expiryDate) throws ProcessCheckErrorException {
		if (expiryDate == null || expiryDate.trim().isEmpty()) {
			return null;
		}
		try {
			return Date.valueOf(expiryDate.trim());
		} catch (IllegalArgumentException e) {
			throwError("ME000093");
			return null; // không bao giờ tới đây - throwError luôn ném exception
		}
	}

	/**
	 * note optional (VARCHAR(200) - xem db/schema.sql) - chặn sớm bằng lỗi
	 * nghiệp vụ rõ nghĩa thay vì để rơi xuống DBException lúc INSERT (cột quá
	 * ngắn) rồi bị AbstractProcess quy về lỗi hệ thống chung chung MC000001.
	 */
	private void validateNote(String note) throws ProcessCheckErrorException {
		if (isTooLong(note, 200)) {
			throwError("ME000096");
		}
	}

	/**
	 * Validate độ dài các field thông tin HĐĐT (tất cả optional, chỉ chặn khi
	 * vượt quá độ rộng cột DB - không tự đặt thêm quy tắc định dạng số hoá
	 * đơn/ký hiệu vì đây là dữ liệu tham chiếu tự do NCC cung cấp, không phải
	 * nghiệp vụ thuế cần kiểm chặt - retail-domain.md đánh dấu quy tắc thuế
	 * UNKNOWN).
	 */
	private void validateEinvoiceFields(InboundReceiptCreateRequest req) throws ProcessCheckErrorException {
		if (isTooLong(req.einvoiceNo, 20) || isTooLong(req.einvoiceSeries, 20)
				|| isTooLong(req.einvoiceLookupCode, 50) || isTooLong(req.einvoiceUrl, 500)) {
			throwError("ME000094");
		}
	}

	private boolean isTooLong(String value, int maxLength) {
		return value != null && value.length() > maxLength;
	}

	private Date parseEinvoiceIssueDate(String issueDate) throws ProcessCheckErrorException {
		if (issueDate == null || issueDate.trim().isEmpty()) {
			return null;
		}
		try {
			return Date.valueOf(issueDate.trim());
		} catch (IllegalArgumentException e) {
			throwError("ME000095");
			return null; // không bao giờ tới đây - throwError luôn ném exception
		}
	}

	private void validateItemExists(DBAccessor dba, String productCode)
			throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT product_code FROM product WHERE product_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, productCode);
			rs = ps.executeQuery();
			if (!rs.next()) {
				throwError("ME000061");
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void validateSupplierExists(DBAccessor dba, String supplierCode)
			throws DBException, ProcessCheckErrorException {
		if (supplierCode == null || supplierCode.trim().isEmpty()) {
			return;
		}
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT supplier_code FROM supplier WHERE supplier_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, supplierCode.trim());
			rs = ps.executeQuery();
			if (!rs.next()) {
				throwError("ME000082");
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/**
	 * Chi nhánh nhận hàng = main_branch_code của người lập phiếu đang đăng nhập
	 * (accessInfo hiện chưa mang branchCode) - KHÔNG nhận branchCode từ client,
	 * giống SaleOrderCreateProcess.getCashierBranchCode().
	 */
	private String getUserBranchCode(DBAccessor dba, String userCode) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT main_branch_code FROM app_user WHERE user_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			rs = ps.executeQuery();
			String branchCode = rs.next() ? rs.getString("main_branch_code") : null;
			if (branchCode == null || branchCode.trim().isEmpty()) {
				throwError("ME000088");
			}
			return branchCode;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void insertReceiptHeader(DBAccessor dba, String receiptNo, String branchCode,
			InboundReceiptCreateRequest req, Date receiptDate, Date einvoiceIssueDate, String userCode)
			throws DBException {

		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO inbound_receipt ");
			sql.append("(branch_code, receipt_no, supplier_code, planned_arrival_date, actual_arrival_date, ");
			sql.append(" receipt_date, receipt_user_code, note, ");
			sql.append(" einvoice_no, einvoice_series, einvoice_issue_date, einvoice_lookup_code, einvoice_url, ");
			sql.append(" del_flg, entry_user_code, entry_program, update_user_code, update_program) ");
			sql.append("VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0', ?, ?, ?, ?)");

			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setString(2, receiptNo);
			ps.setString(3, req.supplierCode);
			ps.setDate(4, receiptDate);
			ps.setDate(5, receiptDate);
			ps.setString(6, userCode);
			ps.setString(7, req.note);
			ps.setString(8, req.einvoiceNo);
			ps.setString(9, req.einvoiceSeries);
			ps.setDate(10, einvoiceIssueDate);
			ps.setString(11, req.einvoiceLookupCode);
			ps.setString(12, req.einvoiceUrl);
			ps.setString(13, userCode);
			ps.setString(14, PRG_CD);
			ps.setString(15, userCode);
			ps.setString(16, PRG_CD);
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private void insertReceiptItems(DBAccessor dba, String receiptNo, String branchCode,
			List<InboundReceiptItemDto> items, List<Date> expiryDates, String userCode) throws DBException {

		DBStatement ps = null;
		try {
			String sql = "INSERT INTO inbound_receipt_item "
					+ "(branch_code, receipt_no, line_no, product_code, quality_code, expiry_date, "
					+ " planned_qty, actual_qty, unit_cost, note, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?)";

			ps = dba.prepareStatement(sql);
			int lineNo = 1;
			for (int i = 0; i < items.size(); i++) {
				InboundReceiptItemDto item = items.get(i);

				ps.setString(1, branchCode);
				ps.setString(2, receiptNo);
				ps.setInt(3, lineNo++);
				ps.setString(4, item.productCode);
				ps.setString(5, DEFAULT_QUALITY_CODE);
				ps.setDate(6, expiryDates.get(i));
				ps.setInt(7, item.quantity);
				ps.setInt(8, item.quantity);
				ps.setBigDecimal(9, item.unitCost);
				ps.setString(10, userCode);
				ps.setString(11, PRG_CD);
				ps.setString(12, userCode);
				ps.setString(13, PRG_CD);
				ps.executeUpdate();
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	/**
	 * Cộng dồn tồn kho theo (branch_code, product_code) - khớp khoá chính bảng
	 * stock. LƯU Ý: stock KHÔNG theo dõi tồn theo lô/hạn dùng riêng (1 dòng duy
	 * nhất mỗi sản phẩm/chi nhánh, đúng thiết kế đơn giản hoá cho quy mô cửa
	 * hàng nhỏ - xem comment bảng stock trong db/schema.sql), nên expiry_date
	 * bị GHI ĐÈ bằng lô mới nhập gần nhất mỗi lần nhập thêm - không phải theo
	 * dõi FEFO (hết hạn trước xuất trước) theo từng lô, đây là giới hạn kế thừa
	 * từ thiết kế bảng stock, không phải phát sinh mới ở Process này.
	 */
	private void upsertStock(DBAccessor dba, String branchCode, List<InboundReceiptItemDto> items,
			List<Date> expiryDates, String userCode) throws DBException {

		DBStatement ps = null;
		try {
			String sql = "INSERT INTO stock "
					+ "(branch_code, product_code, quality_code, expiry_date, stock_qty, available_qty, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
					+ "ON DUPLICATE KEY UPDATE "
					+ "stock_qty = stock_qty + VALUES(stock_qty), "
					+ "available_qty = available_qty + VALUES(available_qty), "
					+ "expiry_date = VALUES(expiry_date), "
					+ "update_user_code = VALUES(update_user_code), "
					+ "update_program = VALUES(update_program)";

			ps = dba.prepareStatement(sql);
			for (int i = 0; i < items.size(); i++) {
				InboundReceiptItemDto item = items.get(i);

				ps.setString(1, branchCode);
				ps.setString(2, item.productCode);
				ps.setString(3, DEFAULT_QUALITY_CODE);
				ps.setDate(4, expiryDates.get(i));
				ps.setInt(5, item.quantity);
				ps.setInt(6, item.quantity);
				ps.setString(7, userCode);
				ps.setString(8, PRG_CD);
				ps.setString(9, userCode);
				ps.setString(10, PRG_CD);
				ps.executeUpdate();
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	/**
	 * Ghi lại giá bán đang hiển thị trên lưới màn Nhập hàng vào product.price -
	 * cho phép sửa giá bán ngay lúc nhập hàng thay vì phải mở riêng màn Sản
	 * phẩm (xem trao đổi thiết kế). Ghi lại KỂ CẢ khi giá không đổi (đơn giản,
	 * an toàn - ghi đè bằng đúng giá đang hiển thị không gây sai lệch).
	 */
	private void updateProductPrices(DBAccessor dba, List<InboundReceiptItemDto> items, String userCode)
			throws DBException {

		DBStatement ps = null;
		try {
			String sql = "UPDATE product SET price = ?, update_user_code = ?, update_program = ? "
					+ "WHERE product_code = ? AND del_flg = '0'";

			ps = dba.prepareStatement(sql);
			for (InboundReceiptItemDto item : items) {
				ps.setBigDecimal(1, item.price);
				ps.setString(2, userCode);
				ps.setString(3, PRG_CD);
				ps.setString(4, item.productCode);
				ps.executeUpdate();
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}

	private void closeQuietly(ResultSet rs, DBStatement ps) throws DBException {
		try {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
