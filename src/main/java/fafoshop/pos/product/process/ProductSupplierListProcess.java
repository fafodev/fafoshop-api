package fafoshop.pos.product.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.pos.product.dto.ProductSupplierListRequest;
import fafoshop.pos.product.dto.ProductSupplierListResponse;
import fafoshop.pos.product.dto.ProductSupplierRowDto;

/**
 * Lấy danh sách nhà cung cấp đã gắn với 1 sản phẩm — dùng riêng cho form
 * sửa sản phẩm (Product Master) nạp lại các dòng NCC đã lưu trước đó.
 * Tách khỏi ProductSearchProcess vì lưới danh sách chỉ cần tên NCC nối
 * chuỗi (ProductQueryHelper.SELECT_COLUMNS_SQL), không cần đủ field
 * (supplierProductCode/purchasePrice) như form sửa.
 */
public class ProductSupplierListProcess extends AbstractProcess {

	public ProductSupplierListProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new ProductSupplierListResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		ProductSupplierListRequest req = (ProductSupplierListRequest) request;
		ProductSupplierListResponse res = (ProductSupplierListResponse) response;

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT ps.supplier_code, s.name AS supplier_name, ps.supplier_product_code, "
					+ "ps.purchase_price FROM product_supplier ps "
					+ "JOIN supplier s ON s.supplier_code = ps.supplier_code "
					+ "WHERE ps.product_code = ? ORDER BY s.name";

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.productCode);
			rs = ps.executeQuery();

			List<ProductSupplierRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				ProductSupplierRowDto row = new ProductSupplierRowDto();
				row.supplierCode = rs.getString("supplier_code");
				row.supplierName = rs.getString("supplier_name");
				row.supplierProductCode = rs.getString("supplier_product_code");
				row.purchasePrice = rs.getBigDecimal("purchase_price");
				rows.add(row);
			}
			res.rows = rows;

			return res;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				throw new DBException(e);
			}
		}
	}
}
