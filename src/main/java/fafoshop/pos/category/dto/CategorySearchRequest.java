package fafoshop.pos.category.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class CategorySearchRequest extends AbstractRequest {

	/** Từ khoá tìm theo tên hoặc mã danh mục; rỗng = lấy tất cả */
	public String keyword;

	/** Lọc theo category_type; rỗng/null = không lọc, lấy mọi loại danh mục */
	public String categoryType;

	/** "ACTIVE" (mặc định, còn hiệu lực) | "DELETED" (đã xoá) | "ALL" (tất cả) */
	public String statusFilter;

	/** Trang hiện tại, 0-based (khớp pageIndex của Angular Material Paginator) */
	public int pageIndex;

	/** Số dòng mỗi trang */
	public int pageSize;

	/** Cột sắp xếp — whitelist ở CategoryQueryHelper, giá trị lạ sẽ dùng mặc định "displayOrder" */
	public String sortField;

	/** "ASC" | "DESC" — mặc định "ASC" nếu giá trị khác */
	public String sortDirection;
}
