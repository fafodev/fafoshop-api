package fafoshop.pos.category.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class CategoryUpdateRequest extends AbstractRequest {

	/** categoryCode là khoá chính, KHÔNG đổi được sau khi tạo — chỉ dùng để xác định dòng cần sửa */
	public String categoryCode;
	public String name;
	public String categoryType;
	public Integer displayOrder;
}
