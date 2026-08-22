package fafoshop.pos.product.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Ghi đè giá bán + giá vốn cho ĐÚNG một đơn vị của sản phẩm — dùng khi sửa
 * đơn bán và người dùng đồng ý cập nhật luôn Product Master (không gửi cả
 * form sản phẩm, tránh ghi đè tên/mã vạch/NCC...). Xem
 * docs/pos-dong-bo-gia.md: giá theo từng ĐVT, không quy đổi chéo lẻ ↔ đóng
 * gói.
 *
 * {@code unitName} null/trống = đơn vị lẻ ({@code product.price}/
 * {@code product.cost}). Có giá trị (vd "Vỉ") = {@code product_unit.unit_price}/
 * {@code product_unit.unit_cost} của đúng dòng đó. {@code unitPrice}/
 * {@code unitCost} là giá THEO ĐÚNG đơn vị đó, không phải per-lẻ.
 */
public class ProductSyncPriceRequest extends AbstractRequest {

	public String productCode;

	/** null/trống = đơn vị lẻ. */
	public String unitName;

	/** Giá bán theo đúng đơn vị đang sửa — bắt buộc, không âm. */
	public BigDecimal unitPrice;

	/**
	 * Giá vốn theo đúng đơn vị đang sửa — không bắt buộc. null = giữ nguyên
	 * giá vốn đang có trên Master, chỉ ghi giá bán.
	 */
	public BigDecimal unitCost;
}
