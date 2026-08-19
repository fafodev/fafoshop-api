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
import fafoshop.pos.product.dto.ProductUnitDto;
import fafoshop.pos.product.dto.ProductUnitListRequest;
import fafoshop.pos.product.dto.ProductUnitListResponse;

/**
 * Lấy danh sách đơn vị đóng gói (Lốc, Thùng...) đã gắn với 1 sản phẩm —
 * dùng cho CẢ form sửa sản phẩm (Product Master, nạp lại dòng đã lưu) LẪN
 * lúc quét/chọn sản phẩm ở POS/Nhập hàng (kiểm tra có cấu hình đơn vị nào
 * không để quyết định có hiện hộp chọn đơn vị hay không — xem
 * docs/pos-da-don-vi-tinh.md). Mirror ProductSupplierListProcess.
 */
public class ProductUnitListProcess extends AbstractProcess {

	public ProductUnitListProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new ProductUnitListResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		ProductUnitListRequest req = (ProductUnitListRequest) request;
		ProductUnitListResponse res = (ProductUnitListResponse) response;

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT unit_name, conversion_qty, unit_price, unit_cost FROM product_unit "
					+ "WHERE product_code = ? ORDER BY conversion_qty ASC";

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.productCode);
			rs = ps.executeQuery();

			List<ProductUnitDto> rows = new ArrayList<>();
			while (rs.next()) {
				ProductUnitDto row = new ProductUnitDto();
				row.unitName = rs.getString("unit_name");
				row.conversionQty = rs.getInt("conversion_qty");
				row.unitPrice = rs.getBigDecimal("unit_price");
				row.unitCost = rs.getBigDecimal("unit_cost");
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
