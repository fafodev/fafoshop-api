package fafoshop.pos.product.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/**
 * 1 đơn vị đóng gói LỚN HƠN đơn vị nhỏ nhất của sản phẩm (vd "Lốc" = 4 đơn
 * vị lẻ) — khớp bảng product_unit. Dùng CHUNG cho cả chiều gửi lên (tạo/sửa
 * sản phẩm, client gửi TOÀN BỘ danh sách mong muốn — xem ProductUpdateProcess)
 * LẪN chiều trả về (ProductUnitListProcess) — không cần tách 2 class như
 * ProductSupplierDto/ProductSupplierRowDto vì không có field JOIN thêm nào
 * (khác NCC cần join lấy supplierName).
 */
public class ProductUnitDto extends AbstractDto {

	public String unitName;

	/** Số lượng đơn vị NHỎ NHẤT (lẻ) quy đổi ra 1 đơn vị này, vd Lốc=4. */
	public Integer conversionQty;

	/** Giá bán khi bán theo đơn vị này — nhập tay riêng, KHÔNG ép theo tỉ lệ. */
	public BigDecimal unitPrice;
}
