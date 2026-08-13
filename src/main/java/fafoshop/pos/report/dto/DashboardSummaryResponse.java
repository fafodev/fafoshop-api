package fafoshop.pos.report.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class DashboardSummaryResponse extends AbstractResponse {

	public String branchCode;

	/** "yyyy-MM-dd" — ngày thực tế đã dùng để tính (khớp request.selectedDate, hoặc hôm nay nếu request để trống). */
	public String selectedDate;

	// KPI "Doanh thu theo ngày" — theo selectedDate người dùng chọn (mặc định hôm nay)
	public BigDecimal selectedDateRevenue = BigDecimal.ZERO;
	public long selectedDateOrderCount;
	/** Doanh thu ngày liền trước selectedDate — dùng tính % so sánh phía frontend. */
	public BigDecimal previousDateRevenue = BigDecimal.ZERO;

	// KPI "Số đơn hôm nay" — LUÔN theo ngày server thật, không phụ thuộc selectedDate
	public long todayOrderCount;
	public BigDecimal todayAvgOrderValue = BigDecimal.ZERO;
	public long yesterdayOrderCount;

	// KPI "Doanh thu N ngày gần nhất" (N = rangeDays đã chọn, tính tới hôm nay)
	public String rangeFromDate;
	public String rangeToDate;
	public BigDecimal rangeRevenue = BigDecimal.ZERO;
	/** Tổng doanh thu N ngày liền trước rangeFromDate — dùng tính % so sánh. */
	public BigDecimal previousRangeRevenue = BigDecimal.ZERO;

	// KPI "Nhập hàng hôm nay"
	public int todayInboundReceiptCount;
	public BigDecimal todayInboundValue = BigDecimal.ZERO;

	/** Biểu đồ xu hướng doanh thu rangeDays ngày gần nhất — đã điền 0 cho ngày không phát sinh đơn nào. */
	public List<DailyRevenuePointDto> dailyTrend = new ArrayList<>();

	/** Top 5 sản phẩm bán chạy theo doanh thu trong rangeDays ngày gần nhất. */
	public List<TopProductDto> topProducts = new ArrayList<>();

	// Tỷ trọng thanh toán trong rangeDays ngày gần nhất
	public BigDecimal cashRevenue = BigDecimal.ZERO;
	public BigDecimal transferRevenue = BigDecimal.ZERO;

	/** Cảnh báo vận hành: sản phẩm tồn kho <= định mức tối thiểu (product.min_stock_qty > 0), tối đa 5 dòng, sắp theo mức thiếu hụt nhiều nhất trước. */
	public List<LowStockRowDto> lowStockProducts = new ArrayList<>();

	/** Cảnh báo vận hành: sản phẩm có hạn dùng trong vòng product.expiry_warning_days ngày tới, tối đa 5 dòng, sắp theo hạn gần nhất trước. */
	public List<ExpiringStockRowDto> expiringProducts = new ArrayList<>();
}
