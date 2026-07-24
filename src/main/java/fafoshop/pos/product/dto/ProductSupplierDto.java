package fafoshop.pos.product.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/** 1 dòng gắn nhà cung cấp gửi lên khi tạo/sửa sản phẩm (client gửi TOÀN BỘ danh sách mong muốn, xem ProductUpdateProcess). */
public class ProductSupplierDto extends AbstractDto {

	public String supplierCode;
	/** Mã hàng riêng của NCC cho sản phẩm này — khác product_code nội bộ. */
	public String supplierProductCode;
	/** Giá mua từ NCC này — quy tắc thuế/làm tròn khi mua: UNKNOWN (xem retail-domain.md). */
	public BigDecimal purchasePrice;
}
