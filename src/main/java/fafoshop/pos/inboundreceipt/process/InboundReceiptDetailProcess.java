package fafoshop.pos.inboundreceipt.process;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptDetailItemDto;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptDetailRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptDetailResponse;

/**
 * Xem chi tiết 1 phiếu nhập (header + danh sách dòng hàng) — mirror
 * SaleOrderDetailProcess. CHỈ ĐỌC. Giới hạn THEO CHI NHÁNH của người xem.
 */
public class InboundReceiptDetailProcess extends AbstractProcess {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public InboundReceiptDetailProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new InboundReceiptDetailResponse();
	}

	@Override
	protected String getFuncId() {
		return "INBND_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		InboundReceiptDetailRequest req = (InboundReceiptDetailRequest) request;
		InboundReceiptDetailResponse res = (InboundReceiptDetailResponse) response;

		if (req.receiptNo == null || req.receiptNo.trim().isEmpty()) {
			InboundReceiptQueryHelper.throwError("ME000125");
		}

		String branchCode = InboundReceiptQueryHelper.resolveBranchCode(dba, req.accessInfo.userCode);

		queryHeader(dba, req.receiptNo, branchCode, res);
		queryItems(dba, req.receiptNo, branchCode, res);

		return res;
	}

	private void queryHeader(DBAccessor dba, String receiptNo, String branchCode, InboundReceiptDetailResponse res)
			throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT ir.receipt_no, ir.branch_code, ir.supplier_code, s.name AS supplier_name, "
					+ "ir.receipt_date, ir.note, ir.einvoice_no, ir.einvoice_series, ir.einvoice_issue_date, "
					+ "ir.einvoice_lookup_code, ir.einvoice_url, ir.receipt_user_code, u.name AS receipt_user_name, "
					+ "ir.void_flg "
					+ "FROM inbound_receipt ir "
					+ "LEFT JOIN supplier s ON s.supplier_code = ir.supplier_code "
					+ "LEFT JOIN app_user u ON u.user_code = ir.receipt_user_code "
					+ "WHERE ir.receipt_no = ? AND ir.branch_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, receiptNo);
			ps.setString(2, branchCode);
			rs = ps.executeQuery();

			if (!rs.next()) {
				InboundReceiptQueryHelper.throwError("ME000125");
				return; // không bao giờ tới đây - throwError luôn ném exception
			}

			res.receiptNo = rs.getString("receipt_no");
			res.branchCode = rs.getString("branch_code");
			res.supplierCode = rs.getString("supplier_code");
			res.supplierName = rs.getString("supplier_name");
			Date receiptDate = rs.getDate("receipt_date");
			res.receiptDate = receiptDate != null ? receiptDate.toLocalDate().format(DATE_FMT) : null;
			res.note = rs.getString("note");
			res.einvoiceNo = rs.getString("einvoice_no");
			res.einvoiceSeries = rs.getString("einvoice_series");
			Date einvoiceIssueDate = rs.getDate("einvoice_issue_date");
			res.einvoiceIssueDate = einvoiceIssueDate != null ? einvoiceIssueDate.toLocalDate().format(DATE_FMT) : null;
			res.einvoiceLookupCode = rs.getString("einvoice_lookup_code");
			res.einvoiceUrl = rs.getString("einvoice_url");
			res.receiptUserCode = rs.getString("receipt_user_code");
			res.receiptUserName = rs.getString("receipt_user_name");
			res.voidFlg = rs.getString("void_flg");

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			InboundReceiptQueryHelper.closeQuietly(rs, ps);
		}
	}

	private void queryItems(DBAccessor dba, String receiptNo, String branchCode, InboundReceiptDetailResponse res)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT iri.line_no, iri.product_code, p.name AS product_name, p.barcode, "
					+ "iri.actual_qty, iri.unit_cost, iri.expiry_date "
					+ "FROM inbound_receipt_item iri LEFT JOIN product p ON p.product_code = iri.product_code "
					+ "WHERE iri.branch_code = ? AND iri.receipt_no = ? ORDER BY iri.line_no ASC";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setString(2, receiptNo);
			rs = ps.executeQuery();

			BigDecimal totalAmount = BigDecimal.ZERO;
			while (rs.next()) {
				InboundReceiptDetailItemDto item = new InboundReceiptDetailItemDto();
				item.lineNo = rs.getInt("line_no");
				item.productCode = rs.getString("product_code");
				item.productName = rs.getString("product_name");
				item.barcode = rs.getString("barcode");
				item.quantity = rs.getInt("actual_qty");
				item.unitCost = rs.getBigDecimal("unit_cost");
				item.lineAmount = item.unitCost.multiply(BigDecimal.valueOf(item.quantity));
				Date expiryDate = rs.getDate("expiry_date");
				item.expiryDate = expiryDate != null ? expiryDate.toLocalDate().format(DATE_FMT) : null;
				res.items.add(item);
				totalAmount = totalAmount.add(item.lineAmount);
			}
			res.totalAmount = totalAmount;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			InboundReceiptQueryHelper.closeQuietly(rs, ps);
		}
	}
}
