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
	/** Số ngày cảnh báo trước hạn sử dụng — dùng ở màn Nhập hàng để cảnh báo khi hạn dùng nhập vào còn quá gần. */
	public Integer expiryWarningDays;
	public String delFlg;

	/** Định dạng "yyyy-MM-dd HH:mm:ss" — dùng String để tránh phải thêm module Jackson JSR-310 chỉ cho 1 field. */
	public String updateDatetime;

	/**
	 * true nếu sản phẩm có cấu hình đơn vị đóng gói (product_unit, vd
	 * Lốc/Thùng) — FE (POS/Nhập hàng) dùng field NÀY để quyết định có cần gọi
	 * THÊM API pos/product/unit/list hay không lúc quét/chọn sản phẩm, tránh
	 * round-trip thừa cho ĐA SỐ sản phẩm không dùng đa đơn vị (xem
	 * docs/pos-da-don-vi-tinh.md). Kiểu `boolean` nguyên thuỷ (không phải
	 * `Boolean`) — luôn có giá trị thật (EXISTS() không bao giờ NULL), không
	 * thuộc diện field nullable phải lo undefined ở FE.
	 */
	public boolean hasUnits;
}
