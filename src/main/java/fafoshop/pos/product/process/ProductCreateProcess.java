package fafoshop.pos.product.process;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.SeqNoUtility;
import fafoshop.pos.product.dto.ProductCreateRequest;
import fafoshop.pos.product.dto.ProductCreateResponse;

/**
 * Tạo sản phẩm mới trên bảng product. product_code SINH TỰ ĐỘNG qua
 * SeqNoUtility (prefix "SP", xem .claude/seqno-convention.md) — dạng
 * "SP"+yyyyMMdd+4 số, theo đúng chuẩn chung sinh mã quản lý toàn hệ thống
 * (thay cho cơ chế timestamp cũ "PRD"+compactTimestamp()).
 */
public class ProductCreateProcess extends AbstractProcess {

	/**
	 * Mã chương trình ghi vào entry_program/update_program — cột này chỉ rộng
	 * VARCHAR(10); tên class đầy đủ (ProductCreateProcess, 20 ký tự) không vừa
	 * nên dùng mã rút gọn thay vì getClass().getSimpleName().
	 */
	private static final String PRG_CD = "PRDCT_CRT";

	/** Prefix đăng ký sẵn trong bảng seq_no cho product_code. */
	private static final String SEQ_PREFIX = "SP";

	public ProductCreateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new ProductCreateResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		ProductCreateRequest req = (ProductCreateRequest) request;
		ProductCreateResponse res = (ProductCreateResponse) response;

		ProductFieldValidator.validate(dba, req.name, req.price, req.barcode, null);
		ProductCategoryValidator.validate(dba, req.categoryCode);
		ProductSupplierValidator.validate(dba, req.suppliers);
		ProductUnitValidator.validate(req.productUnits);
		ProductFieldValidator.validateExpiryWarningDays(req.expiryWarningDays);

		String productCode = SeqNoUtility.generate(dba, SEQ_PREFIX, req.accessInfo.userCode, PRG_CD);

		DBStatement ps = null;

		try {
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO product ");
			sql.append("(product_code, name, short_name, barcode, category_code, unit_name, ");
			sql.append(" reduced_tax_rate_flg, price, min_stock_qty, expiry_warning_days, ");
			sql.append(" entry_user_code, entry_program, update_user_code, update_program) ");
			sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			ps = dba.prepareStatement(sql);
			ps.setString(1, productCode);
			ps.setString(2, req.name);
			ps.setString(3, req.shortName);
			ps.setString(4, req.barcode);
			ps.setString(5, req.categoryCode);
			ps.setString(6, req.unitName);
			ps.setString(7, req.reducedTaxRateFlg);
			ps.setBigDecimal(8, req.price);
			ps.setInt(9, req.minStockQty != null ? req.minStockQty : 0);
			ps.setInt(10, req.expiryWarningDays != null ? req.expiryWarningDays : 90);
			ps.setString(11, req.accessInfo.userCode);
			ps.setString(12, PRG_CD);
			ps.setString(13, req.accessInfo.userCode);
			ps.setString(14, PRG_CD);
			ps.executeUpdate();

			ProductSupplierWriter.insertAll(dba, productCode, req.suppliers, req.accessInfo.userCode, PRG_CD);
			ProductUnitWriter.insertAll(dba, productCode, req.productUnits, req.accessInfo.userCode, PRG_CD);

			res.productCode = productCode;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}
}
