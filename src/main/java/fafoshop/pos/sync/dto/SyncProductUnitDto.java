package fafoshop.pos.sync.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/** 1 đơn vị đóng gói (Lốc/Thùng...) của sản phẩm — khớp bảng product_unit, không kèm giá vốn. */
public class SyncProductUnitDto extends AbstractDto {
	public String productCode;
	public String unitName;
	/** Số đơn vị lẻ quy đổi ra 1 đơn vị này, vd Lốc = 4. */
	public Integer conversionQty;
	/** Giá bán khi bán theo đơn vị này (nhập tay riêng, không ép theo tỉ lệ). */
	public BigDecimal unitPrice;
}
