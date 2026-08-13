package fafoshop.pos.report.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/** 1 điểm dữ liệu doanh thu theo ngày — dùng cho biểu đồ xu hướng màn Tổng quan. */
public class DailyRevenuePointDto extends AbstractDto {

	/** "yyyy-MM-dd" */
	public String saleDate;

	public BigDecimal revenue;

	public long orderCount;
}
