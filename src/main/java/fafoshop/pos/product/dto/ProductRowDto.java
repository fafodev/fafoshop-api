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

	/**
	 * Giá vốn BÌNH QUÂN GIA QUYỀN hiện tại của sản phẩm — tính CHUNG mọi chi
	 * nhánh (KHÁC `sale_order_item.unit_cost` vốn chụp lại THEO CHI NHÁNH tại
	 * thời điểm bán, xem retail-domain.md mục "Giá vốn & tiền lãi") — chỉ 1
	 * chi nhánh CN001 có dữ liệu hiện tại nên chưa khác biệt thực tế, đơn
	 * giản hoá có chủ đích vì field này CHỈ dùng làm CẢNH BÁO tham khảo ở màn
	 * Nhập hàng ("giá vốn nhập vào lệch bất thường"), KHÔNG phải số liệu tài
	 * chính chính thức nào. NULL nếu sản phẩm CHƯA TỪNG có phiếu nhập nào
	 * (không có gì để so sánh, không phải giá vốn = 0). NULLABLE (kế thừa
	 * AbstractDto, @JsonInclude(NON_NULL)) — frontend PHẢI check cả `null`
	 * lẫn `undefined`.
	 */
	public BigDecimal currentAvgCost;
}
