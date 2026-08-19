package fafoshop.pos.product.process;

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
import fafoshop.pos.product.dto.ProductUpdateRequest;
import fafoshop.pos.product.dto.ProductUpdateResponse;

/**
 * Sửa thông tin sản phẩm đã có (không sửa được sản phẩm đã xoá mềm — phải
 * khôi phục trước, xem ProductRestoreProcess).
 */
public class ProductUpdateProcess extends AbstractProcess {

	/** Mã chương trình ghi vào update_program — cột chỉ rộng VARCHAR(10). */
	private static final String PRG_CD = "PRDCT_UPD";

	public ProductUpdateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new ProductUpdateResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		ProductUpdateRequest req = (ProductUpdateRequest) request;
		ProductUpdateResponse res = (ProductUpdateResponse) response;

		ProductFieldValidator.validate(dba, req.name, req.price, req.barcode, req.productCode);
		ProductFieldValidator.validateCost(req.cost);
		ProductCategoryValidator.validate(dba, req.categoryCode);
		ProductSupplierValidator.validate(dba, req.suppliers);
		ProductUnitValidator.validate(req.productUnits);

		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE product SET ");
			sql.append("name = ?, short_name = ?, barcode = ?, category_code = ?, ");
			sql.append("unit_name = ?, reduced_tax_rate_flg = ?, price = ?, cost = ?, min_stock_qty = ?, ");
			sql.append("update_user_code = ?, update_program = ? ");
			sql.append("WHERE product_code = ? AND del_flg = '0'");

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.name);
			ps.setString(2, req.shortName);
			ps.setString(3, req.barcode);
			ps.setString(4, req.categoryCode);
			ps.setString(5, req.unitName);
			ps.setString(6, req.reducedTaxRateFlg);
			ps.setBigDecimal(7, req.price);
			ps.setBigDecimal(8, req.cost);
			ps.setInt(9, req.minStockQty != null ? req.minStockQty : 0);
			ps.setString(10, req.accessInfo.userCode);
			ps.setString(11, PRG_CD);
			ps.setString(12, req.productCode);

			int affected = ps.executeUpdate();
			if (affected == 0) {
				List<ErrorDto> errors = new ArrayList<>();
				ErrorDto error = new ErrorDto();
				error.errId = "ME000061";
				error.errMsg = MessageUtility.getSystemErrMsg("ME000061");
				errors.add(error);
				throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
			}

			replaceSuppliers(dba, req);
			replaceUnits(dba, req);

			res.productCode = req.productCode;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	/**
	 * Chiến lược "thay hết": xoá toàn bộ dòng product_supplier cũ của sản
	 * phẩm này rồi ghi lại đúng danh sách client gửi lên — đơn giản, đúng vì
	 * client LUÔN gửi toàn bộ danh sách NCC mong muốn (không phải danh sách
	 * thay đổi/diff), không cần so sánh thêm/bớt/sửa từng dòng.
	 */
	private void replaceSuppliers(DBAccessor dba, ProductUpdateRequest req) throws DBException {
		DBStatement deletePs = null;
		try {
			deletePs = dba.prepareStatement("DELETE FROM product_supplier WHERE product_code = ?");
			deletePs.setString(1, req.productCode);
			deletePs.executeUpdate();
		} finally {
			if (deletePs != null) {
				deletePs.close();
			}
		}

		ProductSupplierWriter.insertAll(dba, req.productCode, req.suppliers, req.accessInfo.userCode, PRG_CD);
	}

	/** Chiến lược "thay hết" — mirror replaceSuppliers(), xem docs/pos-da-don-vi-tinh.md. */
	private void replaceUnits(DBAccessor dba, ProductUpdateRequest req) throws DBException {
		DBStatement deletePs = null;
		try {
			deletePs = dba.prepareStatement("DELETE FROM product_unit WHERE product_code = ?");
			deletePs.setString(1, req.productCode);
			deletePs.executeUpdate();
		} finally {
			if (deletePs != null) {
				deletePs.close();
			}
		}

		ProductUnitWriter.insertAll(dba, req.productCode, req.productUnits, req.accessInfo.userCode, PRG_CD);
	}
}
