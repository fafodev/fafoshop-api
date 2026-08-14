package fafoshop.pos.saleorder.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Tra cứu đơn bán hàng (POS) — có phân trang/sắp xếp server-side, lọc theo
 * khoảng ngày/PTTT/trạng thái/thu ngân, giống mẫu ProductSearchRequest.
 * Chi nhánh KHÔNG nhận từ client — luôn lấy theo main_branch_code của người
 * dùng đang đăng nhập (xem SaleOrderQueryHelper.resolveBranchCode), giống
 * đúng quy ước đã áp dụng ở DashboardSummaryProcess/SaleOrderCreateProcess.
 */
public class SaleOrderSearchRequest extends AbstractRequest {

	/** Từ khoá tìm theo số đơn bán hoặc tên khách hàng; rỗng = lấy tất cả */
	public String keyword;

	/** Ngày bán từ (yyyy-MM-dd); rỗng/null = không giới hạn cận dưới */
	public String dateFrom;

	/** Ngày bán đến (yyyy-MM-dd); rỗng/null = không giới hạn cận trên */
	public String dateTo;

	/** "CASH" | "TRANSFER"; rỗng/null/giá trị lạ = không lọc theo PTTT */
	public String paymentMethod;

	/** "VALID" (còn hiệu lực) | "VOID" (đã huỷ) | "ALL" (mặc định, tất cả) */
	public String statusFilter;

	/** Lọc theo tên thu ngân (khớp gần đúng); rỗng/null = không lọc */
	public String cashierKeyword;

	/** Trang hiện tại, 0-based (khớp pageIndex của Angular Material Paginator) */
	public int pageIndex;

	/** Số dòng mỗi trang */
	public int pageSize;

	/** Cột sắp xếp — whitelist ở SaleOrderQueryHelper, giá trị lạ dùng mặc định "saleDatetime" */
	public String sortField;

	/** "ASC" | "DESC" — mặc định "DESC" (đơn mới nhất trước) nếu giá trị khác */
	public String sortDirection;
}
