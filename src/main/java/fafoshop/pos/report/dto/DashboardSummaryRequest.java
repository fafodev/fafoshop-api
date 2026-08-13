package fafoshop.pos.report.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class DashboardSummaryRequest extends AbstractRequest {

	/** Ngày xem doanh thu cho KPI "Doanh thu theo ngày" ("yyyy-MM-dd") — rỗng/null = hôm nay. */
	public String selectedDate;

	/** Số ngày lấy cho biểu đồ xu hướng/top sản phẩm/tỷ trọng thanh toán: 7 hoặc 30 — giá trị khác dùng mặc định 7. */
	public int rangeDays;
}
