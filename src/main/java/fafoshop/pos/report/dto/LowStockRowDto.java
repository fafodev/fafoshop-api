package fafoshop.pos.report.dto;

import fafoshop.common.dto.AbstractDto;

/** 1 sản phẩm đang tồn kho dưới định mức tối thiểu (product.min_stock_qty) — dùng cho cảnh báo vận hành màn Tổng quan. */
public class LowStockRowDto extends AbstractDto {

	public String productCode;

	public String name;

	public int stockQty;

	public int minStockQty;
}
