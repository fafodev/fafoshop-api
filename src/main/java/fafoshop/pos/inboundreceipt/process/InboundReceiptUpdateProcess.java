package fafoshop.pos.inboundreceipt.process;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import fafoshop.pos.inboundreceipt.dto.InboundReceiptItemDto;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptUpdateRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptUpdateResponse;

/**
 * Sửa lại TOÀN BỘ danh sách dòng hàng + thông tin đầu phiếu của 1 phiếu
 * nhập đã tạo — mirror SaleOrderUpdateProcess (chiến lược "thay hết").
 * Tồn kho điều chỉnh theo DELTA (InboundReceiptStockAdjuster). Giá bán
 * (`product.price`) của MỌI dòng được ghi đè lại giống lúc tạo (xem
 * InboundReceiptCreateProcess.updateProductPrices). Xem
 * docs/pos-sua-huy-don.md (gốc workspace).
 */
public class InboundReceiptUpdateProcess extends AbstractProcess {

	private static final String PRG_CD = "INBND_EDT";

	private static final String DEFAULT_QUALITY_CODE = "01";

	public InboundReceiptUpdateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new InboundReceiptUpdateResponse();
	}

	@Override
	protected String getFuncId() {
		return "INBND_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		InboundReceiptUpdateRequest req = (InboundReceiptUpdateRequest) request;
		InboundReceiptUpdateResponse res = (InboundReceiptUpdateResponse) response;

		validateReceiptNo(req.receiptNo);
		validateItems(req.items);
		List<Date> expiryDates = new ArrayList<>();
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (InboundReceiptItemDto item : req.items) {
			validateItemExists(dba, item.productCode);
			expiryDates.add(parseDate(item.expiryDate, "ME000093"));
			totalAmount = totalAmount.add(effectiveLineAmount(item));
		}
		validateSupplierExists(dba, req.supplierCode);
		validateNote(req.note);
		validateEinvoiceFields(req);
		Date einvoiceIssueDate = parseDate(req.einvoiceIssueDate, "ME000095");

		String branchCode = InboundReceiptEditGuard.resolveEligibleBranchCode(dba, req.receiptNo, req.accessInfo.userCode);

		Map<String, Integer> oldQtyByProduct = queryCurrentQuantities(dba, branchCode, req.receiptNo);
		Map<String, Integer> newQtyByProduct = new HashMap<>();
		for (InboundReceiptItemDto item : req.items) {
			newQtyByProduct.merge(item.productCode, item.quantity, Integer::sum);
		}

		Map<String, Integer> delta = new HashMap<>();
		for (Map.Entry<String, Integer> e : newQtyByProduct.entrySet()) {
			delta.merge(e.getKey(), e.getValue(), Integer::sum);
		}
		for (Map.Entry<String, Integer> e : oldQtyByProduct.entrySet()) {
			delta.merge(e.getKey(), -e.getValue(), Integer::sum);
		}

		InboundReceiptStockAdjuster.applyDelta(dba, branchCode, delta, req.accessInfo.userCode, PRG_CD);

		updateHeader(dba, req, branchCode, einvoiceIssueDate, req.accessInfo.userCode);
		replaceItems(dba, req.receiptNo, branchCode, req.items, expiryDates, req.accessInfo.userCode);
		updateProductPrices(dba, req.items, req.accessInfo.userCode);

		res.receiptNo = req.receiptNo;
		res.totalAmount = totalAmount;
		return res;
	}

	private void validateReceiptNo(String receiptNo) throws ProcessCheckErrorException {
		if (receiptNo == null || receiptNo.trim().isEmpty()) {
			throwError("ME000125");
		}
	}

	/** Giống hệt InboundReceiptCreateProcess.validateItems() — cố ý KHÔNG tái dùng cross-class, xem lý do ở SaleOrderUpdateProcess. */
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
			if (item.unitName != null && !item.unitName.trim().isEmpty()
					&& (item.price == null || item.price.signum() <= 0)) {
				throwError("ME000128");
			}
			validateLineAmount(item);
		}
	}

	/** Giống hệt InboundReceiptCreateProcess.validateLineAmount()/effectiveLineAmount() — xem Javadoc ở đó. */
	private void validateLineAmount(InboundReceiptItemDto item) throws ProcessCheckErrorException {
		if (item.lineAmount == null) {
			return;
		}
		if (item.lineAmount.signum() < 0) {
			throwError("ME000127");
		}
		BigDecimal computed = item.unitCost.multiply(BigDecimal.valueOf(item.quantity));
		BigDecimal maxDrift = BigDecimal.valueOf(item.quantity);
		if (item.lineAmount.subtract(computed).abs().compareTo(maxDrift) > 0) {
			throwError("ME000127");
		}
	}

	private BigDecimal effectiveLineAmount(InboundReceiptItemDto item) {
		return item.lineAmount != null ? item.lineAmount : item.unitCost.multiply(BigDecimal.valueOf(item.quantity));
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

	private void validateNote(String note) throws ProcessCheckErrorException {
		if (isTooLong(note, 200)) {
			throwError("ME000096");
		}
	}

	private void validateEinvoiceFields(InboundReceiptUpdateRequest req) throws ProcessCheckErrorException {
		if (isTooLong(req.einvoiceNo, 20) || isTooLong(req.einvoiceSeries, 20)
				|| isTooLong(req.einvoiceLookupCode, 50) || isTooLong(req.einvoiceUrl, 500)) {
			throwError("ME000094");
		}
	}

	private boolean isTooLong(String value, int maxLength) {
		return value != null && value.length() > maxLength;
	}

	private Date parseDate(String value, String errIdIfInvalid) throws ProcessCheckErrorException {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		try {
			return Date.valueOf(value.trim());
		} catch (IllegalArgumentException e) {
			throwError(errIdIfInvalid);
			return null; // không bao giờ tới đây - throwError luôn ném exception
		}
	}

	private Map<String, Integer> queryCurrentQuantities(DBAccessor dba, String branchCode, String receiptNo)
			throws DBException {
		Map<String, Integer> result = new HashMap<>();
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT product_code, actual_qty FROM inbound_receipt_item "
					+ "WHERE branch_code = ? AND receipt_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setString(2, receiptNo);
			rs = ps.executeQuery();
			while (rs.next()) {
				result.merge(rs.getString("product_code"), rs.getInt("actual_qty"), Integer::sum);
			}
			return result;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void updateHeader(DBAccessor dba, InboundReceiptUpdateRequest req, String branchCode,
			Date einvoiceIssueDate, String userCode) throws DBException {
		DBStatement ps = null;
		try {
			String sql = "UPDATE inbound_receipt SET supplier_code = ?, note = ?, "
					+ "einvoice_no = ?, einvoice_series = ?, einvoice_issue_date = ?, "
					+ "einvoice_lookup_code = ?, einvoice_url = ?, "
					+ "update_user_code = ?, update_program = ? "
					+ "WHERE branch_code = ? AND receipt_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, req.supplierCode);
			ps.setString(2, req.note);
			ps.setString(3, req.einvoiceNo);
			ps.setString(4, req.einvoiceSeries);
			ps.setDate(5, einvoiceIssueDate);
			ps.setString(6, req.einvoiceLookupCode);
			ps.setString(7, req.einvoiceUrl);
			ps.setString(8, userCode);
			ps.setString(9, PRG_CD);
			ps.setString(10, branchCode);
			ps.setString(11, req.receiptNo);
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private void replaceItems(DBAccessor dba, String receiptNo, String branchCode, List<InboundReceiptItemDto> items,
			List<Date> expiryDates, String userCode) throws DBException {

		DBStatement deletePs = null;
		try {
			deletePs = dba.prepareStatement("DELETE FROM inbound_receipt_item WHERE branch_code = ? AND receipt_no = ?");
			deletePs.setString(1, branchCode);
			deletePs.setString(2, receiptNo);
			deletePs.executeUpdate();
		} finally {
			if (deletePs != null) {
				deletePs.close();
			}
		}

		DBStatement insertPs = null;
		try {
			String sql = "INSERT INTO inbound_receipt_item "
					+ "(branch_code, receipt_no, line_no, product_code, quality_code, expiry_date, "
					+ " planned_qty, actual_qty, unit_cost, unit_name, unit_qty, line_amount, price, note, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?)";

			insertPs = dba.prepareStatement(sql);
			int lineNo = 1;
			for (int i = 0; i < items.size(); i++) {
				InboundReceiptItemDto item = items.get(i);

				insertPs.setString(1, branchCode);
				insertPs.setString(2, receiptNo);
				insertPs.setInt(3, lineNo++);
				insertPs.setString(4, item.productCode);
				insertPs.setString(5, DEFAULT_QUALITY_CODE);
				insertPs.setDate(6, expiryDates.get(i));
				insertPs.setInt(7, item.quantity);
				insertPs.setInt(8, item.quantity);
				insertPs.setBigDecimal(9, item.unitCost);
				insertPs.setString(10, item.unitName);
				insertPs.setNullableInt(11, item.unitQty);
				insertPs.setBigDecimal(12, effectiveLineAmount(item));
				insertPs.setBigDecimal(13, item.price);
				insertPs.setString(14, userCode);
				insertPs.setString(15, PRG_CD);
				insertPs.setString(16, userCode);
				insertPs.setString(17, PRG_CD);
				insertPs.executeUpdate();
			}
		} finally {
			if (insertPs != null) {
				insertPs.close();
			}
		}
	}

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
