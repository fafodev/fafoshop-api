package fafoshop.pos.supplier.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class SupplierSearchRequest extends AbstractRequest {

	/** Từ khoá tìm theo tên hoặc tên rút gọn nhà cung cấp; rỗng = lấy tất cả */
	public String keyword;

	/** "ACTIVE" (mặc định, còn hiệu lực) | "DELETED" (đã xoá) | "ALL" (tất cả) */
	public String statusFilter;

	/** Trang hiện tại, 0-based (khớp pageIndex của Angular Material Paginator) */
	public int pageIndex;

	/** Số dòng mỗi trang */
	public int pageSize;

	/** Cột sắp xếp — whitelist ở SupplierQueryHelper, giá trị lạ sẽ dùng mặc định "name" */
	public String sortField;

	/** "ASC" | "DESC" — mặc định "ASC" nếu giá trị khác */
	public String sortDirection;
}
