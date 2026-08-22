package fafoshop.pos.product.process;

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
import fafoshop.pos.product.dto.ProductSyncPriceRequest;
import fafoshop.pos.product.dto.ProductSyncPriceResponse;

/**
 * Ghi đè giá bán/giá vốn cho ĐÚNG một đơn vị trên Product Master — đối xứng
 * {@code InboundReceiptCostWriter} nhưng nhận 1 cặp giá (không phải danh
 * sách dòng phiếu nhập). Quyền {@code PRDCT_EDIT} giống form sửa sản phẩm.
 */
public class ProductSyncPriceProcess extends AbstractProcess {

	private static final String PRG_CD = "PRC_SYNC";

	public ProductSyncPriceProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new ProductSyncPriceResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		ProductSyncPriceRequest req = (ProductSyncPriceRequest) request;
		ProductSyncPriceResponse res = (ProductSyncPriceResponse) response;

		if (req.productCode == null || req.productCode.trim().isEmpty()) {
			throwError("ME000061");
		}
		ProductFieldValidator.validatePrice(req.unitPrice);
		ProductFieldValidator.validateCost(req.unitCost);

		boolean isBaseUnit = req.unitName == null || req.unitName.trim().isEmpty();
		if (isBaseUnit) {
			updateBaseUnit(dba, req);
		} else {
			updatePackUnit(dba, req);
		}

		res.productCode = req.productCode;
		res.unitName = isBaseUnit ? null : req.unitName;
		return res;
	}

	private void updateBaseUnit(DBAccessor dba, ProductSyncPriceRequest req)
			throws DBException, ProcessCheckErrorException {
		DBStatement ps = null;
		try {
			String sql = req.unitCost != null
					? "UPDATE product SET price = ?, cost = ?, update_user_code = ?, update_program = ? "
							+ "WHERE product_code = ? AND del_flg = '0'"
					: "UPDATE product SET price = ?, update_user_code = ?, update_program = ? "
							+ "WHERE product_code = ? AND del_flg = '0'";

			ps = dba.prepareStatement(sql);
			int i = 1;
			ps.setBigDecimal(i++, req.unitPrice);
			if (req.unitCost != null) {
				ps.setBigDecimal(i++, req.unitCost);
			}
			ps.setString(i++, req.accessInfo.userCode);
			ps.setString(i++, PRG_CD);
			ps.setString(i++, req.productCode);

			if (ps.executeUpdate() == 0) {
				throwError("ME000061");
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private void updatePackUnit(DBAccessor dba, ProductSyncPriceRequest req)
			throws DBException, ProcessCheckErrorException {
		ensureProductExists(dba, req.productCode);

		DBStatement ps = null;
		try {
			String sql = req.unitCost != null
					? "UPDATE product_unit SET unit_price = ?, unit_cost = ?, update_user_code = ?, update_program = ? "
							+ "WHERE product_code = ? AND unit_name = ?"
					: "UPDATE product_unit SET unit_price = ?, update_user_code = ?, update_program = ? "
							+ "WHERE product_code = ? AND unit_name = ?";

			ps = dba.prepareStatement(sql);
			int i = 1;
			ps.setBigDecimal(i++, req.unitPrice);
			if (req.unitCost != null) {
				ps.setBigDecimal(i++, req.unitCost);
			}
			ps.setString(i++, req.accessInfo.userCode);
			ps.setString(i++, PRG_CD);
			ps.setString(i++, req.productCode);
			ps.setString(i++, req.unitName);

			if (ps.executeUpdate() == 0) {
				throwError("ME000133");
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private void ensureProductExists(DBAccessor dba, String productCode)
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
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
