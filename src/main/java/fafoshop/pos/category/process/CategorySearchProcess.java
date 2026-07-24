package fafoshop.pos.category.process;

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
import fafoshop.pos.category.dto.CategoryFullRowDto;
import fafoshop.pos.category.dto.CategorySearchRequest;
import fafoshop.pos.category.dto.CategorySearchResponse;

/**
 * Tìm kiếm danh mục (có phân trang/sắp xếp server-side, lọc theo loại/trạng
 * thái) — API cho màn hình Category Master. Tách biệt CategoryListProcess
 * (chỉ đọc, phục vụ dropdown Product Master).
 */
public class CategorySearchProcess extends AbstractProcess {

	public CategorySearchProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new CategorySearchResponse();
	}

	@Override
	protected String getFuncId() {
		return "CTGR_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		CategorySearchRequest req = (CategorySearchRequest) request;
		CategorySearchResponse res = (CategorySearchResponse) response;

		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		CategoryQueryHelper.buildWhereClause(req.keyword, req.categoryType, req.statusFilter, where, params);

		res.totalCount = queryTotalCount(dba, where.toString(), params);
		res.rows = queryRows(dba, req, where.toString(), params);

		return res;
	}

	private long queryTotalCount(DBAccessor dba, String where, List<String> params) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COUNT(*) AS cnt FROM category c " + where;
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

	private List<CategoryFullRowDto> queryRows(DBAccessor dba, CategorySearchRequest req, String where,
			List<String> params) throws DBException {

		String sortColumn = CategoryQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = CategoryQueryHelper.resolveSortDirection(req.sortDirection);
		int pageSize = req.pageSize > 0 ? req.pageSize : 20;
		int pageIndex = Math.max(req.pageIndex, 0);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(CategoryQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(CategoryQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection).append(" ");
			sql.append("LIMIT ? OFFSET ?");

			ps = dba.prepareStatement(sql);
			int idx = bindParams(ps, params);
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, pageIndex * pageSize);

			rs = ps.executeQuery();

			List<CategoryFullRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(CategoryQueryHelper.mapRow(rs));
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
