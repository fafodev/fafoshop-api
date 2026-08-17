package fafoshop.pos.product.dto;

import java.math.BigDecimal;
import java.util.List;

import fafoshop.common.dto.request.AbstractRequest;

public class ProductUpdateRequest extends AbstractRequest {

	public String productCode;
	public String name;
	public String shortName;
	public String barcode;
	public String categoryCode;
	public String unitName;
	public String reducedTaxRateFlg;
	public BigDecimal price;
	public Integer minStockQty;
	/** Danh sách NCC gắn với sản phẩm — client gửi TOÀN BỘ danh sách mong muốn, ProductUpdateProcess thay hết dòng cũ. */
	public List<ProductSupplierDto> suppliers;
	/** Đơn vị đóng gói lớn hơn đơn vị lẻ — client gửi TOÀN BỘ danh sách mong muốn, thay hết dòng cũ (giống suppliers). */
	public List<ProductUnitDto> productUnits;
}
