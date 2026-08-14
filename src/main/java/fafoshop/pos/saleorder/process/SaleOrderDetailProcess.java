package fafoshop.pos.saleorder.process;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
import fafoshop.pos.saleorder.dto.SaleOrderDetailItemDto;
import fafoshop.pos.saleorder.dto.SaleOrderDetailRequest;
import fafoshop.pos.saleorder.dto.SaleOrderDetailResponse;

/**
 * Xem chi tiết 1 đơn bán (header + danh sách dòng hàng) — dùng khi bấm "Xem
 * chi tiết" trên màn tra cứu (SaleOrderSearchProcess). CHỈ ĐỌC. Giới hạn
 * THEO CHI NHÁNH của người xem (giống SaleOrderSearchProcess) — không cho
 * xem đơn của chi nhánh khác dù biết đúng số đơn, dù hiện tại chỉ có 1 chi
 * nhánh có dữ liệu (CN001).
 */
public class SaleOrderDetailProcess extends AbstractProcess {

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public SaleOrderDetailProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderDetailResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderDetailRequest req = (SaleOrderDetailRequest) request;
		SaleOrderDetailResponse res = (SaleOrderDetailResponse) response;

		if (req.saleOrderNo == null || req.saleOrderNo.trim().isEmpty()) {
			SaleOrderQueryHelper.throwError("ME000119");
		}

		String branchCode = SaleOrderQueryHelper.resolveBranchCode(dba, req.accessInfo.userCode);

		queryHeader(dba, req.saleOrderNo, branchCode, res);
		queryItems(dba, req.saleOrderNo, res);

		return res;
	}

	private void queryHeader(DBAccessor dba, String saleOrderNo, String branchCode, SaleOrderDetailResponse res)
			throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT so.sale_order_no, so.branch_code, so.customer_name, so.sale_datetime, "
					+ "so.payment_method, so.paid_amount, so.change_amount, so.cashier_user_code, "
					+ "u.name AS cashier_name, so.void_flg "
					+ "FROM sale_order so LEFT JOIN app_user u ON u.user_code = so.cashier_user_code "
					+ "WHERE so.sale_order_no = ? AND so.branch_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, saleOrderNo);
			ps.setString(2, branchCode);
			rs = ps.executeQuery();

			if (!rs.next()) {
				SaleOrderQueryHelper.throwError("ME000119");
				return; // không bao giờ tới đây - throwError luôn ném exception
			}

			res.saleOrderNo = rs.getString("sale_order_no");
			res.branchCode = rs.getString("branch_code");
			res.customerName = rs.getString("customer_name");
			Timestamp saleDatetime = rs.getTimestamp("sale_datetime");
			res.saleDatetime = saleDatetime != null ? saleDatetime.toLocalDateTime().format(DATETIME_FMT) : null;
			res.paymentMethod = rs.getString("payment_method");
			res.paidAmount = rs.getBigDecimal("paid_amount");
			res.changeAmount = rs.getBigDecimal("change_amount");
			res.cashierUserCode = rs.getString("cashier_user_code");
			res.cashierName = rs.getString("cashier_name");
			res.voidFlg = rs.getString("void_flg");

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			SaleOrderQueryHelper.closeQuietly(rs, ps);
		}
	}

	private void queryItems(DBAccessor dba, String saleOrderNo, SaleOrderDetailResponse res) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT soi.line_no, soi.product_code, p.name AS product_name, p.barcode, "
					+ "soi.unit_price, soi.quantity, soi.line_amount, soi.unit_cost "
					+ "FROM sale_order_item soi LEFT JOIN product p ON p.product_code = soi.product_code "
					+ "WHERE soi.sale_order_no = ? ORDER BY soi.line_no ASC";
			ps = dba.prepareStatement(sql);
			ps.setString(1, saleOrderNo);
			rs = ps.executeQuery();

			BigDecimal subtotal = BigDecimal.ZERO;
			BigDecimal profitAmount = BigDecimal.ZERO;
			boolean hasUnknownCost = false;
			while (rs.next()) {
				SaleOrderDetailItemDto item = new SaleOrderDetailItemDto();
				item.lineNo = rs.getInt("line_no");
				item.productCode = rs.getString("product_code");
				item.productName = rs.getString("product_name");
				item.barcode = rs.getString("barcode");
				item.unitPrice = rs.getBigDecimal("unit_price");
				item.quantity = rs.getInt("quantity");
				item.lineAmount = rs.getBigDecimal("line_amount");
				item.unitCost = rs.getBigDecimal("unit_cost");
				if (item.unitCost != null) {
					item.lineProfit = item.lineAmount.subtract(item.unitCost.multiply(BigDecimal.valueOf(item.quantity)));
					profitAmount = profitAmount.add(item.lineProfit);
				} else {
					hasUnknownCost = true; // sản phẩm dòng này chưa từng có phiếu nhập lúc bán — không tính được lãi TOÀN đơn
				}
				res.items.add(item);
				subtotal = subtotal.add(item.lineAmount);
			}
			res.subtotal = subtotal;
			// Chỉ trả tổng lãi khi ĐỦ giá vốn cho TẤT CẢ dòng — 1 dòng thiếu là
			// đủ để coi tổng lãi "chưa xác định" (NULL), tránh hiển thị số THIẾU
			// 1 phần chi phí mà không cảnh báo, cùng nguyên tắc với
			// SaleOrderQueryHelper.PROFIT_SUBQUERY_SQL.
			res.profitAmount = hasUnknownCost ? null : profitAmount;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			SaleOrderQueryHelper.closeQuietly(rs, ps);
		}
	}
}
