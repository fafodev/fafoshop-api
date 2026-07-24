package fafoshop.pos.category.dto;

import fafoshop.common.dto.AbstractDto;

/**
 * Dòng dữ liệu ĐẦY ĐỦ cho màn Category Master (search/create/update/delete/
 * restore/export) — khác CategoryRowDto (chỉ categoryCode+name) vốn phục vụ
 * riêng dropdown chọn danh mục ở Product Master (CategoryListWebService),
 * không đổi để tránh ảnh hưởng luồng đó.
 */
public class CategoryFullRowDto extends AbstractDto {

	public String categoryCode;
	public String name;
	public String categoryType;
	public Integer displayOrder;
	public String delFlg;

	/** Định dạng "yyyy-MM-dd HH:mm:ss" — dùng String để tránh phải thêm module Jackson JSR-310 chỉ cho 1 field. */
	public String updateDatetime;
}
