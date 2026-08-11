package fafoshop.pos.saleorder.process;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import fafoshop.pos.saleorder.dto.SaleOrderCreateRequest;
import fafoshop.pos.saleorder.dto.SaleOrderCreateResponse;
import fafoshop.pos.saleorder.dto.SaleOrderItemDto;

/**
 * Tạo đơn bán tại quầy (checkout POS thật — thay cho alert() trong
 * pos.component.ts, xem comment trên bảng sale_order trong db/schema.sql).
 *
 * Phạm vi lần này CHỈ lưu giao dịch xuống sale_order/sale_order_item làm nền
 * tảng cho báo cáo/xuất thuế SAU NÀY — KHÔNG tự tính thuế hay tạo mẫu hoá đơn
 * in cụ thể (retail-domain.md đánh dấu 2 phần đó UNKNOWN).
 */
public class SaleOrderCreateProcess extends AbstractProcess {

	private static final String PRG_CD = "SALE_CRT";

	private static final String SEQ_PREFIX = "HD";

	public SaleOrderCreateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderCreateResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_CREAT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderCreateRequest req = (SaleOrderCreateRequest) request;
		SaleOrderCreateResponse res = (SaleOrderCreateResponse) response;

		validateItems(req.items);

		BigDecimal subtotal = BigDecimal.ZERO;
		for (SaleOrderItemDto item : req.items) {
			validateItemExists(dba, item.productCode);
			subtotal = subtotal.add(item.unitPrice.multiply(BigDecimal.valueOf(item.quantity)));
		}

		validatePaidAmount(req.paidAmount, subtotal);

		String branchCode = getCashierBranchCode(dba, req.accessInfo.userCode);
		BigDecimal changeAmount = req.paidAmount.subtract(subtotal);

		String saleOrderNo = SeqNoUtility.generate(dba, SEQ_PREFIX, req.accessInfo.userCode, PRG_CD);
		Timestamp now = new Timestamp(System.currentTimeMillis());

		insertSaleOrder(dba, saleOrderNo, branchCode, req.customerName, now, req.paidAmount, changeAmount,
				req.accessInfo.userCode);
		insertSaleOrderItems(dba, saleOrderNo, req.items, req.accessInfo.userCode);

		res.saleOrderNo = saleOrderNo;
		res.subtotal = subtotal;
		res.changeAmount = changeAmount;
		return res;
	}

	private void validateItems(List<SaleOrderItemDto> items) throws ProcessCheckErrorException {
		if (items == null || items.isEmpty()) {
			throwError("ME000085");
		}
		for (SaleOrderItemDto item : items) {
			if (item.productCode == null || item.productCode.trim().isEmpty()) {
				throwError("ME000061");
			}
			if (item.quantity == null || item.quantity <= 0) {
				throwError("ME000086");
			}
			if (item.unitPrice == null || item.unitPrice.signum() < 0) {
				throwError("ME000067");
			}
		}
	}

	private void validateItemExists(DBAccessor dba, String productCode) throws DBException, ProcessCheckErrorException {
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

	private void validatePaidAmount(BigDecimal paidAmount, BigDecimal subtotal) throws ProcessCheckErrorException {
		if (paidAmount == null || paidAmount.compareTo(subtotal) < 0) {
			throwError("ME000087");
		}
	}

	/**
	 * Chi nhánh của đơn bán = main_branch_code của thu ngân đang đăng nhập
	 * (accessInfo hiện chưa mang branchCode, xem AccessInfoDto/AuthTokenFilter)
	 * — KHÔNG nhận branchCode từ client.
	 */
	private String getCashierBranchCode(DBAccessor dba, String userCode) throws DBException, ProcessCheckErrorException {
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

	private void insertSaleOrder(DBAccessor dba, String saleOrderNo, String branchCode, String customerName,
			Timestamp saleDatetime, BigDecimal paidAmount, BigDecimal changeAmount, String userCode) throws DBException {

		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO sale_order ");
			sql.append("(sale_order_no, branch_code, customer_code, customer_name, sale_datetime, ");
			sql.append(" paid_amount, change_amount, cashier_user_code, void_flg, ");
			sql.append(" entry_user_code, entry_program, update_user_code, update_program) ");
			sql.append("VALUES (?, ?, NULL, ?, ?, ?, ?, ?, '0', ?, ?, ?, ?)");

			ps = dba.prepareStatement(sql);
			ps.setString(1, saleOrderNo);
			ps.setString(2, branchCode);
			ps.setString(3, customerName);
			ps.setTimestamp(4, saleDatetime);
			ps.setBigDecimal(5, paidAmount);
			ps.setBigDecimal(6, changeAmount);
			ps.setString(7, userCode);
			ps.setString(8, userCode);
			ps.setString(9, PRG_CD);
			ps.setString(10, userCode);
			ps.setString(11, PRG_CD);
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private void insertSaleOrderItems(DBAccessor dba, String saleOrderNo, List<SaleOrderItemDto> items,
			String userCode) throws DBException {

		DBStatement ps = null;
		try {
			String sql = "INSERT INTO sale_order_item "
					+ "(sale_order_no, line_no, product_code, unit_price, quantity, line_amount, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			ps = dba.prepareStatement(sql);
			int lineNo = 1;
			for (SaleOrderItemDto item : items) {
				BigDecimal lineAmount = item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));

				ps.setString(1, saleOrderNo);
				ps.setInt(2, lineNo++);
				ps.setString(3, item.productCode);
				ps.setBigDecimal(4, item.unitPrice);
				ps.setInt(5, item.quantity);
				ps.setBigDecimal(6, lineAmount);
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
