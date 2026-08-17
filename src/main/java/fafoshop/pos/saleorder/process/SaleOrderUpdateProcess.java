package fafoshop.pos.saleorder.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import fafoshop.pos.saleorder.dto.SaleOrderItemDto;
import fafoshop.pos.saleorder.dto.SaleOrderUpdateRequest;
import fafoshop.pos.saleorder.dto.SaleOrderUpdateResponse;

/**
 * Sửa lại TOÀN BỘ danh sách dòng hàng của 1 đơn bán đã tạo — chiến lược
 * "thay hết" (client gửi danh sách MONG MUỐN, server xoá dòng cũ + ghi lại
 * dòng mới), KHÔNG làm PATCH từng dòng riêng lẻ. Xem
 * docs/pos-sua-huy-don.md (gốc workspace) — quyết định đã chốt qua trao đổi
 * trực tiếp với người dùng.
 *
 * Điều kiện được sửa: xem {@link SaleOrderEditGuard}. Tồn kho điều chỉnh
 * theo DELTA (số lượng mới - số lượng cũ từng sản phẩm), xem
 * {@link SaleOrderStockAdjuster}. Giá vốn (`unit_cost`) của MỌI dòng — kể
 * cả dòng đã có từ trước, không chỉ dòng mới thêm — được tính lại theo bình
 * quân gia quyền TẠI THỜI ĐIỂM SỬA (nhất quán với chiến lược "thay hết",
 * không cố giữ snapshot cũ cho dòng không đổi).
 */
public class SaleOrderUpdateProcess extends AbstractProcess {

	private static final String PRG_CD = "SALE_EDT";

	public SaleOrderUpdateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderUpdateResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderUpdateRequest req = (SaleOrderUpdateRequest) request;
		SaleOrderUpdateResponse res = (SaleOrderUpdateResponse) response;

		validateSaleOrderNo(req.saleOrderNo);
		validateItems(req.items);

		BigDecimal subtotal = BigDecimal.ZERO;
		for (SaleOrderItemDto item : req.items) {
			validateItemExists(dba, item.productCode);
			subtotal = subtotal.add(item.unitPrice.multiply(BigDecimal.valueOf(item.quantity)));
		}
		validatePaidAmount(req.paidAmount, subtotal);

		String branchCode = SaleOrderEditGuard.resolveEligibleBranchCode(dba, req.saleOrderNo, req.accessInfo.userCode);

		Map<String, Integer> oldQtyByProduct = queryCurrentQuantities(dba, req.saleOrderNo);
		Map<String, Integer> newQtyByProduct = new HashMap<>();
		for (SaleOrderItemDto item : req.items) {
			newQtyByProduct.merge(item.productCode, item.quantity, Integer::sum);
		}

		Map<String, Integer> delta = new HashMap<>();
		for (Map.Entry<String, Integer> e : newQtyByProduct.entrySet()) {
			delta.merge(e.getKey(), e.getValue(), Integer::sum);
		}
		for (Map.Entry<String, Integer> e : oldQtyByProduct.entrySet()) {
			delta.merge(e.getKey(), -e.getValue(), Integer::sum);
		}

		SaleOrderStockAdjuster.applyDelta(dba, branchCode, delta, req.accessInfo.userCode, PRG_CD);

		replaceItems(dba, req.saleOrderNo, branchCode, req.items, req.accessInfo.userCode);

		BigDecimal changeAmount = req.paidAmount.subtract(subtotal);
		updateSaleOrder(dba, req.saleOrderNo, req.paidAmount, changeAmount, req.accessInfo.userCode);

		res.saleOrderNo = req.saleOrderNo;
		res.subtotal = subtotal;
		res.changeAmount = changeAmount;
		return res;
	}

	/** Giống hệt SaleOrderCreateProcess.validateItems() — cố ý KHÔNG tái dùng cross-class (Process khác, giữ độc lập để sửa 1 bên không ảnh hưởng bên kia đã test kỹ). */
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

	private void validateSaleOrderNo(String saleOrderNo) throws ProcessCheckErrorException {
		if (saleOrderNo == null || saleOrderNo.trim().isEmpty()) {
			throwError("ME000120");
		}
	}

	private Map<String, Integer> queryCurrentQuantities(DBAccessor dba, String saleOrderNo) throws DBException {
		Map<String, Integer> result = new HashMap<>();
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT product_code, quantity FROM sale_order_item WHERE sale_order_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, saleOrderNo);
			rs = ps.executeQuery();
			while (rs.next()) {
				result.merge(rs.getString("product_code"), rs.getInt("quantity"), Integer::sum);
			}
			return result;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void replaceItems(DBAccessor dba, String saleOrderNo, String branchCode, List<SaleOrderItemDto> items,
			String userCode) throws DBException {

		DBStatement deletePs = null;
		try {
			deletePs = dba.prepareStatement("DELETE FROM sale_order_item WHERE sale_order_no = ?");
			deletePs.setString(1, saleOrderNo);
			deletePs.executeUpdate();
		} finally {
			if (deletePs != null) {
				deletePs.close();
			}
		}

		DBStatement insertPs = null;
		try {
			String sql = "INSERT INTO sale_order_item "
					+ "(sale_order_no, line_no, product_code, unit_price, quantity, line_amount, unit_cost, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			insertPs = dba.prepareStatement(sql);
			int lineNo = 1;
			for (SaleOrderItemDto item : items) {
				BigDecimal lineAmount = item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));
				BigDecimal unitCost = queryWeightedAvgUnitCost(dba, branchCode, item.productCode);

				insertPs.setString(1, saleOrderNo);
				insertPs.setInt(2, lineNo++);
				insertPs.setString(3, item.productCode);
				insertPs.setBigDecimal(4, item.unitPrice);
				insertPs.setInt(5, item.quantity);
				insertPs.setBigDecimal(6, lineAmount);
				insertPs.setBigDecimal(7, unitCost);
				insertPs.setString(8, userCode);
				insertPs.setString(9, PRG_CD);
				insertPs.setString(10, userCode);
				insertPs.setString(11, PRG_CD);
				insertPs.executeUpdate();
			}
		} finally {
			if (insertPs != null) {
				insertPs.close();
			}
		}
	}

	/** Giống hệt SaleOrderCreateProcess.queryWeightedAvgUnitCost() — xem Javadoc ở đó cho công thức đầy đủ đã chốt. */
	private BigDecimal queryWeightedAvgUnitCost(DBAccessor dba, String branchCode, String productCode)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT SUM(actual_qty * unit_cost) AS total_cost, SUM(actual_qty) AS total_qty "
					+ "FROM inbound_receipt_item WHERE branch_code = ? AND product_code = ? AND actual_qty > 0";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setString(2, productCode);
			rs = ps.executeQuery();

			if (!rs.next()) {
				return null;
			}
			BigDecimal totalCost = rs.getBigDecimal("total_cost");
			BigDecimal totalQty = rs.getBigDecimal("total_qty");
			if (totalCost == null || totalQty == null || totalQty.signum() <= 0) {
				return null;
			}
			return totalCost.divide(totalQty, 2, RoundingMode.HALF_UP);
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void updateSaleOrder(DBAccessor dba, String saleOrderNo, BigDecimal paidAmount, BigDecimal changeAmount,
			String userCode) throws DBException {
		DBStatement ps = null;
		try {
			String sql = "UPDATE sale_order SET paid_amount = ?, change_amount = ?, "
					+ "update_user_code = ?, update_program = ? WHERE sale_order_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setBigDecimal(1, paidAmount);
			ps.setBigDecimal(2, changeAmount);
			ps.setString(3, userCode);
			ps.setString(4, PRG_CD);
			ps.setString(5, saleOrderNo);
			ps.executeUpdate();
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
