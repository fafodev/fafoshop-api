package fafoshop.pos.report.dto;

import fafoshop.common.dto.AbstractDto;

/** 1 sản phẩm đang tồn kho có hạn dùng sắp tới (trong vòng product.expiry_warning_days ngày) — dùng cho cảnh báo vận hành màn Tổng quan. */
public class ExpiringStockRowDto extends AbstractDto {

	public String productCode;

	public String name;

	/** "yyyy-MM-dd" */
	public String expiryDate;

	/** Số ngày còn lại tới hạn dùng — có thể ÂM nếu đã quá hạn (vẫn hiển thị để cảnh báo gấp). */
	public long daysRemaining;
}
