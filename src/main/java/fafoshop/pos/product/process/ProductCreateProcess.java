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
import fafoshop.pos.product.dto.ProductCreateRequest;
import fafoshop.pos.product.dto.ProductCreateResponse;

/**
 * Tạo sản phẩm mới trên bảng product.
 *
 * product_code sinh tạm bằng timestamp ("PRD" + currentTimeMillis) — quy tắc
 * sinh mã sản phẩm thật (theo nhóm hàng, theo NCC...) chưa có, giữ UNKNOWN
 * cho tới khi có yêu cầu cụ thể (xem retail-domain.md).
 */
public class ProductCreateProcess extends AbstractProcess {

	/**
	 * Mã chương trình ghi vào entry_program/update_program — cột này chỉ rộng
	 * VARCHAR(10); tên class đầy đủ (ProductCreateProcess, 20 ký tự) không vừa
	 * nên dùng mã rút gọn thay vì getClass().getSimpleName().
	 */
	private static final String PRG_CD = "PRDCT_CRT";

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

		DBStatement ps = null;

		try {
			String productCode = "PRD" + System.currentTimeMillis();

			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO product ");
			sql.append("(product_code, name, barcode, category_code, unit_name, price, ");
			sql.append(" entry_user_code, entry_program, update_user_code, update_program) ");
			sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			ps = dba.prepareStatement(sql);
			ps.setString(1, productCode);
			ps.setString(2, req.name);
			ps.setString(3, req.barcode);
			ps.setString(4, req.categoryCode);
			ps.setString(5, req.unitName);
			ps.setBigDecimal(6, req.price);
			ps.setString(7, req.accessInfo.userCode);
			ps.setString(8, PRG_CD);
			ps.setString(9, req.accessInfo.userCode);
			ps.setString(10, PRG_CD);
			ps.executeUpdate();

			res.productCode = productCode;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}
}
