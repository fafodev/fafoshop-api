package fafoshop.pos.sync.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/**
 * 1 sản phẩm rút gọn cho app bán hàng offline. CỐ Ý KHÔNG có giá vốn
 * (product.cost) — app trên điện thoại không cần và không được lộ giá vốn.
 * Field nullable bị lược khỏi JSON khi null (AbstractDto) — client phải coi
 * thiếu field = null.
 */
public class SyncProductDto extends AbstractDto {
	public String productCode;
	public String name;
	public String shortName;
	public String barcode;
	public String categoryCode;
	/** Tên đơn vị lẻ (đơn vị nhỏ nhất). */
	public String unitName;
	/** Giá bán theo đơn vị lẻ. */
	public BigDecimal price;
}
