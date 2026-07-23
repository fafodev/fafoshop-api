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
import fafoshop.pos.product.dto.ProductRowDto;
import fafoshop.pos.product.dto.ProductSearchRequest;
import fafoshop.pos.product.dto.ProductSearchResponse;

/**
 * Tìm kiếm sản phẩm — API mẫu chứng minh pattern vertical-slice
 * (dto/process/webservice) hoạt động đúng trên bảng product. JDBC thuần qua
 * DBAccessor/DBStatement, không ORM.
 */
public class ProductSearchProcess extends AbstractProcess {

	public ProductSearchProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new ProductSearchResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		ProductSearchRequest req = (ProductSearchRequest) request;
		ProductSearchResponse res = (ProductSearchResponse) response;

		ResultSet rs = null;
		DBStatement ps = null;

		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT product_code, name, barcode, category_code, unit_name, price ");
			sql.append("FROM product ");
			sql.append("WHERE del_flg = '0' ");
			boolean hasKeyword = req.keyword != null && !req.keyword.trim().isEmpty();
			if (hasKeyword) {
				sql.append("AND (name LIKE ? OR barcode = ?) ");
			}
			sql.append("ORDER BY name");

			ps = dba.prepareStatement(sql);
			if (hasKeyword) {
				ps.setString(1, "%" + req.keyword.trim() + "%");
				ps.setString(2, req.keyword.trim());
			}

			rs = ps.executeQuery();

			List<ProductRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				ProductRowDto row = new ProductRowDto();
				row.productCode = rs.getString("product_code");
				row.name = rs.getString("name");
				row.barcode = rs.getString("barcode");
				row.categoryCode = rs.getString("category_code");
				row.unitName = rs.getString("unit_name");
				row.price = rs.getBigDecimal("price");
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
