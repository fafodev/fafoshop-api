package fafoshop.pos.product.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/** 1 dòng nhà cung cấp trả về khi xem chi tiết sản phẩm — có JOIN sẵn supplierName để form hiển thị ngay, không cần tự tra cứu. */
public class ProductSupplierRowDto extends AbstractDto {

	public String supplierCode;
	public String supplierName;
	public String supplierProductCode;
	public BigDecimal purchasePrice;
}
