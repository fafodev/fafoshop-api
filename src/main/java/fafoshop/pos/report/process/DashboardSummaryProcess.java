package fafoshop.pos.report.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import fafoshop.pos.report.dto.DailyRevenuePointDto;
import fafoshop.pos.report.dto.DashboardSummaryRequest;
import fafoshop.pos.report.dto.DashboardSummaryResponse;
import fafoshop.pos.report.dto.ExpiringStockRowDto;
import fafoshop.pos.report.dto.LowStockRowDto;
import fafoshop.pos.report.dto.TopProductDto;

/**
 * Tổng hợp số liệu cho màn hình Tổng quan (dashboard) — CHỈ đọc dữ liệu,
 * không ghi gì. Dùng lại 2 view có sẵn v_daily_revenue/v_item_revenue
 * (db/schema.sql) — trước Process này chưa có process/webservice nào đọc
 * tới 2 view đó. Tỷ trọng thanh toán (cash/transfer) chưa có view riêng nên
 * query thẳng sale_order/sale_order_item.
 *
 * Công thức doanh thu chi tiết hơn (theo ca làm việc, theo nhân viên, trừ
 * hàng trả lại, thuế/làm tròn...) NGOÀI PHẠM VI — retail-domain.md đánh dấu
 * UNKNOWN, Process này chỉ dùng đúng khung tổng hợp cơ bản 2 view đã cung
 * cấp (tổng tiền/số lượng theo ngày, đã lọc void_flg='0').
 */
public class DashboardSummaryProcess extends AbstractProcess {

	private static final int DEFAULT_RANGE_DAYS = 7;

	public DashboardSummaryProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new DashboardSummaryResponse();
	}

	@Override
	protected String getFuncId() {
		return "RPT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		DashboardSummaryRequest req = (DashboardSummaryRequest) request;
		DashboardSummaryResponse res = (DashboardSummaryResponse) response;

		String branchCode = getUserBranchCode(dba, req.accessInfo.userCode);
		LocalDate today = LocalDate.now();
		LocalDate selectedDate = parseSelectedDate(req.selectedDate, today);
		int rangeDays = (req.rangeDays == 7 || req.rangeDays == 30) ? req.rangeDays : DEFAULT_RANGE_DAYS;

		res.branchCode = branchCode;
		res.selectedDate = selectedDate.toString();

		DailyRevenuePointDto selectedPoint = queryDailyRevenue(dba, branchCode, selectedDate);
		res.selectedDateRevenue = selectedPoint.revenue;
		res.selectedDateOrderCount = selectedPoint.orderCount;
		res.previousDateRevenue = queryDailyRevenue(dba, branchCode, selectedDate.minusDays(1)).revenue;

		DailyRevenuePointDto todayPoint = queryDailyRevenue(dba, branchCode, today);
		res.todayOrderCount = todayPoint.orderCount;
		res.todayAvgOrderValue = todayPoint.orderCount > 0
				? todayPoint.revenue.divide(BigDecimal.valueOf(todayPoint.orderCount), 0, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;
		res.yesterdayOrderCount = queryDailyRevenue(dba, branchCode, today.minusDays(1)).orderCount;

		LocalDate rangeFrom = today.minusDays(rangeDays - 1);
		LocalDate previousRangeFrom = rangeFrom.minusDays(rangeDays);
		LocalDate previousRangeTo = rangeFrom.minusDays(1);
		res.rangeFromDate = rangeFrom.toString();
		res.rangeToDate = today.toString();
		res.rangeRevenue = querySumRevenue(dba, branchCode, rangeFrom, today);
		res.previousRangeRevenue = querySumRevenue(dba, branchCode, previousRangeFrom, previousRangeTo);

		queryInboundSummary(dba, res, branchCode, today);
		res.dailyTrend = queryDailyTrend(dba, branchCode, rangeFrom, today);
		res.topProducts = queryTopProducts(dba, branchCode, rangeFrom, today);
		queryPaymentSplit(dba, res, branchCode, rangeFrom, today);
		res.lowStockProducts = queryLowStock(dba, branchCode);
		res.expiringProducts = queryExpiringStock(dba, branchCode, today);

		return res;
	}

	private LocalDate parseSelectedDate(String selectedDate, LocalDate today) throws ProcessCheckErrorException {
		if (selectedDate == null || selectedDate.trim().isEmpty()) {
			return today;
		}
		try {
			return Date.valueOf(selectedDate.trim()).toLocalDate();
		} catch (IllegalArgumentException e) {
			throwError("ME000100");
			return null; // không bao giờ tới đây - throwError luôn ném exception
		}
	}

	/**
	 * Chi nhánh của người đang xem = main_branch_code của user đăng nhập
	 * (accessInfo hiện chưa mang branchCode) — giống hệt cách
	 * SaleOrderCreateProcess/InboundReceiptCreateProcess xác định chi nhánh,
	 * KHÔNG nhận branchCode từ client.
	 */
	private String getUserBranchCode(DBAccessor dba, String userCode) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT main_branch_code FROM app_user WHERE user_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			rs = ps.executeQuery();
			String branchCode = rs.next() ? rs.getString("main_branch_code") : null;
			if (branchCode == null || branchCode.trim().isEmpty()) {
				throwError("ME000088");
			}
			return branchCode;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private DailyRevenuePointDto queryDailyRevenue(DBAccessor dba, String branchCode, LocalDate date)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT total_revenue, order_count FROM v_daily_revenue "
					+ "WHERE branch_code = ? AND sale_date = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(date));
			rs = ps.executeQuery();

			DailyRevenuePointDto point = new DailyRevenuePointDto();
			point.saleDate = date.toString();
			if (rs.next()) {
				point.revenue = rs.getBigDecimal("total_revenue");
				point.orderCount = rs.getLong("order_count");
			} else {
				point.revenue = BigDecimal.ZERO;
				point.orderCount = 0L;
			}
			return point;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private BigDecimal querySumRevenue(DBAccessor dba, String branchCode, LocalDate from, LocalDate to)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COALESCE(SUM(total_revenue), 0) AS total FROM v_daily_revenue "
					+ "WHERE branch_code = ? AND sale_date BETWEEN ? AND ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(from));
			ps.setDate(3, Date.valueOf(to));
			rs = ps.executeQuery();
			return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void queryInboundSummary(DBAccessor dba, DashboardSummaryResponse res, String branchCode,
			LocalDate today) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT COUNT(DISTINCT ir.receipt_no) AS receipt_count, "
					+ "COALESCE(SUM(iri.actual_qty * iri.unit_cost), 0) AS total_value "
					+ "FROM inbound_receipt ir "
					+ "JOIN inbound_receipt_item iri ON iri.branch_code = ir.branch_code AND iri.receipt_no = ir.receipt_no "
					+ "WHERE ir.branch_code = ? AND ir.receipt_date = ? AND ir.del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(today));
			rs = ps.executeQuery();
			if (rs.next()) {
				res.todayInboundReceiptCount = rs.getInt("receipt_count");
				res.todayInboundValue = rs.getBigDecimal("total_value");
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/** Điền đủ mọi ngày trong khoảng [from, to] — ngày không có đơn bán nào thì doanh thu/số đơn = 0 (view chỉ trả dòng có đơn thật). */
	private List<DailyRevenuePointDto> queryDailyTrend(DBAccessor dba, String branchCode, LocalDate from,
			LocalDate to) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		Map<LocalDate, DailyRevenuePointDto> byDate = new HashMap<>();
		try {
			String sql = "SELECT sale_date, total_revenue, order_count FROM v_daily_revenue "
					+ "WHERE branch_code = ? AND sale_date BETWEEN ? AND ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(from));
			ps.setDate(3, Date.valueOf(to));
			rs = ps.executeQuery();
			while (rs.next()) {
				DailyRevenuePointDto point = new DailyRevenuePointDto();
				LocalDate saleDate = rs.getDate("sale_date").toLocalDate();
				point.saleDate = saleDate.toString();
				point.revenue = rs.getBigDecimal("total_revenue");
				point.orderCount = rs.getLong("order_count");
				byDate.put(saleDate, point);
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}

		List<DailyRevenuePointDto> trend = new ArrayList<>();
		for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
			DailyRevenuePointDto point = byDate.get(d);
			if (point == null) {
				point = new DailyRevenuePointDto();
				point.saleDate = d.toString();
				point.revenue = BigDecimal.ZERO;
				point.orderCount = 0L;
			}
			trend.add(point);
		}
		return trend;
	}

	private List<TopProductDto> queryTopProducts(DBAccessor dba, String branchCode, LocalDate from, LocalDate to)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT vir.product_code, p.name, SUM(vir.total_quantity) AS quantity, "
					+ "SUM(vir.total_revenue) AS revenue "
					+ "FROM v_item_revenue vir "
					+ "JOIN product p ON p.product_code = vir.product_code "
					+ "WHERE vir.branch_code = ? AND vir.sale_date BETWEEN ? AND ? "
					+ "GROUP BY vir.product_code, p.name "
					+ "ORDER BY revenue DESC "
					+ "LIMIT 5";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(from));
			ps.setDate(3, Date.valueOf(to));
			rs = ps.executeQuery();

			List<TopProductDto> rows = new ArrayList<>();
			while (rs.next()) {
				TopProductDto row = new TopProductDto();
				row.productCode = rs.getString("product_code");
				row.name = rs.getString("name");
				row.quantity = rs.getLong("quantity");
				row.revenue = rs.getBigDecimal("revenue");
				rows.add(row);
			}
			return rows;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void queryPaymentSplit(DBAccessor dba, DashboardSummaryResponse res, String branchCode, LocalDate from,
			LocalDate to) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT so.payment_method, COALESCE(SUM(soi.line_amount), 0) AS revenue "
					+ "FROM sale_order so "
					+ "JOIN sale_order_item soi ON soi.sale_order_no = so.sale_order_no "
					+ "WHERE so.branch_code = ? AND so.void_flg = '0' AND DATE(so.sale_datetime) BETWEEN ? AND ? "
					+ "GROUP BY so.payment_method";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(from));
			ps.setDate(3, Date.valueOf(to));
			rs = ps.executeQuery();
			while (rs.next()) {
				String paymentMethod = rs.getString("payment_method");
				BigDecimal revenue = rs.getBigDecimal("revenue");
				if ("TRANSFER".equals(paymentMethod)) {
					res.transferRevenue = revenue;
				} else {
					res.cashRevenue = revenue;
				}
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/**
	 * Sản phẩm tồn kho <= định mức tối thiểu (product.min_stock_qty, xem
	 * comment cột trong db/schema.sql: "dùng để xác định sản phẩm dưới định
	 * mức tồn - cần nhập thêm"). min_stock_qty = 0 nghĩa là CHƯA cấu hình định
	 * mức (không tính là thiếu hàng) — loại khỏi danh sách.
	 */
	private List<LowStockRowDto> queryLowStock(DBAccessor dba, String branchCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT s.product_code, p.name, s.stock_qty, p.min_stock_qty "
					+ "FROM stock s "
					+ "JOIN product p ON p.product_code = s.product_code "
					+ "WHERE s.branch_code = ? AND p.del_flg = '0' AND p.min_stock_qty > 0 "
					+ "AND s.stock_qty <= p.min_stock_qty "
					+ "ORDER BY (p.min_stock_qty - s.stock_qty) DESC "
					+ "LIMIT 5";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			rs = ps.executeQuery();

			List<LowStockRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				LowStockRowDto row = new LowStockRowDto();
				row.productCode = rs.getString("product_code");
				row.name = rs.getString("name");
				row.stockQty = rs.getInt("stock_qty");
				row.minStockQty = rs.getInt("min_stock_qty");
				rows.add(row);
			}
			return rows;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/**
	 * Sản phẩm có hạn dùng (stock.expiry_date) trong vòng
	 * product.expiry_warning_days ngày tới (kể cả đã quá hạn — vẫn cảnh báo
	 * gấp hơn, xem ExpiringStockRowDto.daysRemaining có thể âm). LƯU Ý theo
	 * đúng giới hạn thiết kế bảng stock (xem retail-domain.md): expiry_date bị
	 * ghi đè bằng lô nhập gần nhất, không phải theo dõi từng lô riêng.
	 */
	private List<ExpiringStockRowDto> queryExpiringStock(DBAccessor dba, String branchCode, LocalDate today)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT s.product_code, p.name, s.expiry_date "
					+ "FROM stock s "
					+ "JOIN product p ON p.product_code = s.product_code "
					+ "WHERE s.branch_code = ? AND p.del_flg = '0' AND s.expiry_date IS NOT NULL "
					+ "AND s.stock_qty > 0 "
					+ "AND s.expiry_date <= DATE_ADD(?, INTERVAL p.expiry_warning_days DAY) "
					+ "ORDER BY s.expiry_date ASC "
					+ "LIMIT 5";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(today));
			rs = ps.executeQuery();

			List<ExpiringStockRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				ExpiringStockRowDto row = new ExpiringStockRowDto();
				row.productCode = rs.getString("product_code");
				row.name = rs.getString("name");
				LocalDate expiryDate = rs.getDate("expiry_date").toLocalDate();
				row.expiryDate = expiryDate.toString();
				row.daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
				rows.add(row);
			}
			return rows;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
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
