package fafoshop.pos.product.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class ProductSearchRequest extends AbstractRequest {

	/** Từ khoá tìm theo tên sản phẩm hoặc mã vạch (JANCD); rỗng = lấy tất cả */
	public String keyword;

	/** Lọc theo mã danh mục; rỗng/null = không lọc theo danh mục */
	public String categoryCode;

	/** "ACTIVE" (mặc định, còn hiệu lực) | "DELETED" (đã xoá) | "ALL" (tất cả) */
	public String statusFilter;

	/** Trang hiện tại, 0-based (khớp pageIndex của Angular Material Paginator) */
	public int pageIndex;

	/** Số dòng mỗi trang */
	public int pageSize;

	/** Cột sắp xếp — whitelist ở ProductSearchProcess, giá trị lạ sẽ dùng mặc định "name" */
	public String sortField;

	/** "ASC" | "DESC" — mặc định "ASC" nếu giá trị khác */
	public String sortDirection;
}
