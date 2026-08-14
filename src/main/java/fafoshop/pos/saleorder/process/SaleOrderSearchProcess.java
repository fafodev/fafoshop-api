package fafoshop.pos.saleorder.process;

import java.math.BigDecimal;
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
import fafoshop.pos.saleorder.dto.SaleOrderRowDto;
import fafoshop.pos.saleorder.dto.SaleOrderSearchRequest;
import fafoshop.pos.saleorder.dto.SaleOrderSearchResponse;

/**
 * Tra cứu đơn bán hàng (POS) — màn hình xem lại lịch sử giao dịch, lọc theo
 * khoảng ngày/PTTT/trạng thái/thu ngân/từ khoá, có phân trang/sắp xếp
 * server-side (giống mẫu ProductSearchProcess). CHỈ ĐỌC, không ghi gì.
 * Chi nhánh luôn theo user đăng nhập — xem SaleOrderQueryHelper.resolveBranchCode.
 */
public class SaleOrderSearchProcess extends AbstractProcess {

	public SaleOrderSearchProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderSearchResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderSearchRequest req = (SaleOrderSearchRequest) request;
		SaleOrderSearchResponse res = (SaleOrderSearchResponse) response;

		String branchCode = SaleOrderQueryHelper.resolveBranchCode(dba, req.accessInfo.userCode);

		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		SaleOrderQueryHelper.buildWhereClause(branchCode, req.keyword, req.dateFrom, req.dateTo, req.paymentMethod,
				req.statusFilter, req.cashierKeyword, where, params);

		res.totalCount = queryTotalCount(dba, where.toString(), params);
		res.sumTotalAmount = querySumTotalAmount(dba, where.toString(), params);
		res.rows = queryRows(dba, req, where.toString(), params);

		return res;
	}

	private long queryTotalCount(DBAccessor dba, String where, List<String> params) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COUNT(*) AS cnt " + SaleOrderQueryHelper.FROM_JOIN_SQL + where;
			ps = dba.prepareStatement(sql);
			SaleOrderQueryHelper.bindParams(ps, params);
			rs = ps.executeQuery();
			return rs.next() ? rs.getLong("cnt") : 0L;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			SaleOrderQueryHelper.closeQuietly(rs, ps);
		}
	}

	/** Tổng tiền hàng CỘNG DỒN toàn bộ kết quả khớp filter — JOIN thêm sale_order_item (khác query đếm/lấy dòng, không cần subquery vì không phân trang theo dòng). */
	private BigDecimal querySumTotalAmount(DBAccessor dba, String where, List<String> params) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COALESCE(SUM(soi.line_amount), 0) AS sum_amount " + SaleOrderQueryHelper.FROM_JOIN_SQL
					+ "LEFT JOIN sale_order_item soi ON soi.sale_order_no = so.sale_order_no " + where;
			ps = dba.prepareStatement(sql);
			SaleOrderQueryHelper.bindParams(ps, params);
			rs = ps.executeQuery();
			return rs.next() ? rs.getBigDecimal("sum_amount") : BigDecimal.ZERO;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			SaleOrderQueryHelper.closeQuietly(rs, ps);
		}
	}

	private List<SaleOrderRowDto> queryRows(DBAccessor dba, SaleOrderSearchRequest req, String where,
			List<String> params) throws DBException {

		String sortColumn = SaleOrderQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = SaleOrderQueryHelper.resolveSortDirection(req.sortDirection);
		int pageSize = req.pageSize > 0 ? req.pageSize : 20;
		int pageIndex = Math.max(req.pageIndex, 0);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(SaleOrderQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(SaleOrderQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection).append(" ");
			sql.append("LIMIT ? OFFSET ?");

			ps = dba.prepareStatement(sql);
			int idx = SaleOrderQueryHelper.bindParams(ps, params);
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, pageIndex * pageSize);

			rs = ps.executeQuery();

			List<SaleOrderRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(SaleOrderQueryHelper.mapRow(rs));
			}
			return rows;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			SaleOrderQueryHelper.closeQuietly(rs, ps);
		}
	}
}
