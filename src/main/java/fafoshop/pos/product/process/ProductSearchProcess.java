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
 * Tìm kiếm sản phẩm (có phân trang/sắp xếp server-side, lọc theo danh
 * mục/trạng thái) — API cho màn hình Product Master. JDBC thuần qua
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

		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		ProductQueryHelper.buildWhereClause(req.keyword, req.categoryCode, req.statusFilter, where, params);

		res.totalCount = queryTotalCount(dba, where.toString(), params);
		res.rows = queryRows(dba, req, where.toString(), params);

		return res;
	}

	private long queryTotalCount(DBAccessor dba, String where, List<String> params) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COUNT(*) AS cnt FROM product p " + where;
			ps = dba.prepareStatement(sql);
			bindParams(ps, params);
			rs = ps.executeQuery();
			return rs.next() ? rs.getLong("cnt") : 0L;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private List<ProductRowDto> queryRows(DBAccessor dba, ProductSearchRequest req, String where,
			List<String> params) throws DBException {

		String sortColumn = ProductQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = ProductQueryHelper.resolveSortDirection(req.sortDirection);
		int pageSize = req.pageSize > 0 ? req.pageSize : 20;
		int pageIndex = Math.max(req.pageIndex, 0);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(ProductQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(ProductQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection).append(" ");
			sql.append("LIMIT ? OFFSET ?");

			ps = dba.prepareStatement(sql);
			int idx = bindParams(ps, params);
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, pageIndex * pageSize);

			rs = ps.executeQuery();

			List<ProductRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(ProductQueryHelper.mapRow(rs));
			}
			return rows;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private int bindParams(DBStatement ps, List<String> params) throws DBException {
		int idx = 1;
		for (String param : params) {
			ps.setString(idx++, param);
		}
		return idx;
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
