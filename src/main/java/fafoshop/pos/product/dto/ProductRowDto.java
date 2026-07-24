package fafoshop.pos.product.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

public class ProductRowDto extends AbstractDto {

	public String productCode;
	public String name;
	public String shortName;
	public String barcode;
	public String categoryCode;
	public String categoryName;
	/** Tên các NCC đang gắn với sản phẩm này, nối chuỗi bằng ", " — sản phẩm có thể có NHIỀU NCC (xem bảng product_supplier). */
	public String supplierNames;
	public String unitName;
	public String reducedTaxRateFlg;
	public BigDecimal price;
	public Integer minStockQty;
	public String delFlg;

	/** Định dạng "yyyy-MM-dd HH:mm:ss" — dùng String để tránh phải thêm module Jackson JSR-310 chỉ cho 1 field. */
	public String updateDatetime;
}
