package fafoshop.pos.product.dto;

import java.math.BigDecimal;
import java.util.List;

import fafoshop.common.dto.request.AbstractRequest;

public class ProductCreateRequest extends AbstractRequest {

	public String name;
	public String shortName;
	public String barcode;
	public String categoryCode;
	public String unitName;
	public String reducedTaxRateFlg;
	public BigDecimal price;
	/** Giá vốn hiện hành đơn vị lẻ — KHÔNG bắt buộc (sản phẩm mới có thể chưa nhập hàng lần nào), xem docs/pos-dong-bo-gia.md. */
	public BigDecimal cost;
	public Integer minStockQty;
	/** Số ngày cảnh báo trước hạn sử dụng — null thì Process tự áp mặc định 90 ngày. */
	public Integer expiryWarningDays;
	/** Danh sách NCC gắn với sản phẩm — sản phẩm có thể có NHIỀU NCC (xem bảng product_supplier). */
	public List<ProductSupplierDto> suppliers;
	/** Đơn vị đóng gói lớn hơn đơn vị lẻ (Lốc, Thùng...) — xem docs/pos-da-don-vi-tinh.md, bảng product_unit. */
	public List<ProductUnitDto> productUnits;
}
