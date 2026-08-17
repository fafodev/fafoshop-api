package fafoshop.pos.inboundreceipt.process;

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
import fafoshop.pos.inboundreceipt.dto.InboundReceiptRowDto;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptSearchRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptSearchResponse;

/**
 * Tra cứu phiếu nhập hàng — mirror SaleOrderSearchProcess. CHỈ ĐỌC. Chi
 * nhánh luôn theo user đăng nhập — xem InboundReceiptQueryHelper.resolveBranchCode.
 */
public class InboundReceiptSearchProcess extends AbstractProcess {

	public InboundReceiptSearchProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new InboundReceiptSearchResponse();
	}

	@Override
	protected String getFuncId() {
		return "INBND_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		InboundReceiptSearchRequest req = (InboundReceiptSearchRequest) request;
		InboundReceiptSearchResponse res = (InboundReceiptSearchResponse) response;

		String branchCode = InboundReceiptQueryHelper.resolveBranchCode(dba, req.accessInfo.userCode);

		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		InboundReceiptQueryHelper.buildWhereClause(branchCode, req.keyword, req.dateFrom, req.dateTo,
				req.statusFilter, where, params);

		res.totalCount = queryTotalCount(dba, where.toString(), params);
		res.sumTotalAmount = querySumTotalAmount(dba, where.toString(), params);
		res.rows = queryRows(dba, req, where.toString(), params);

		return res;
	}

	private long queryTotalCount(DBAccessor dba, String where, List<String> params) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COUNT(*) AS cnt " + InboundReceiptQueryHelper.FROM_JOIN_SQL + where;
			ps = dba.prepareStatement(sql);
			InboundReceiptQueryHelper.bindParams(ps, params);
			rs = ps.executeQuery();
			return rs.next() ? rs.getLong("cnt") : 0L;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			InboundReceiptQueryHelper.closeQuietly(rs, ps);
		}
	}

	/** Tổng tiền nhập CỘNG DỒN toàn bộ kết quả khớp filter — JOIN thêm inbound_receipt_item, mirror SaleOrderSearchProcess.querySumTotalAmount. */
	private BigDecimal querySumTotalAmount(DBAccessor dba, String where, List<String> params) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COALESCE(SUM(iri.unit_cost * iri.actual_qty), 0) AS sum_amount "
					+ InboundReceiptQueryHelper.FROM_JOIN_SQL
					+ "LEFT JOIN inbound_receipt_item iri ON iri.branch_code = ir.branch_code AND iri.receipt_no = ir.receipt_no "
					+ where;
			ps = dba.prepareStatement(sql);
			InboundReceiptQueryHelper.bindParams(ps, params);
			rs = ps.executeQuery();
			return rs.next() ? rs.getBigDecimal("sum_amount") : BigDecimal.ZERO;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			InboundReceiptQueryHelper.closeQuietly(rs, ps);
		}
	}

	private List<InboundReceiptRowDto> queryRows(DBAccessor dba, InboundReceiptSearchRequest req, String where,
			List<String> params) throws DBException {

		String sortColumn = InboundReceiptQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = InboundReceiptQueryHelper.resolveSortDirection(req.sortDirection);
		int pageSize = req.pageSize > 0 ? req.pageSize : 20;
		int pageIndex = Math.max(req.pageIndex, 0);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(InboundReceiptQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(InboundReceiptQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection).append(" ");
			sql.append("LIMIT ? OFFSET ?");

			ps = dba.prepareStatement(sql);
			int idx = InboundReceiptQueryHelper.bindParams(ps, params);
			ps.setInt(idx++, pageSize);
			ps.setInt(idx++, pageIndex * pageSize);

			rs = ps.executeQuery();

			List<InboundReceiptRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(InboundReceiptQueryHelper.mapRow(rs));
			}
			return rows;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			InboundReceiptQueryHelper.closeQuietly(rs, ps);
		}
	}
}
