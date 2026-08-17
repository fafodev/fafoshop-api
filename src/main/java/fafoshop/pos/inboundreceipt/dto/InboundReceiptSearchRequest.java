package fafoshop.pos.inboundreceipt.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Tra cứu phiếu nhập hàng — mirror SaleOrderSearchRequest. Chi nhánh luôn
 * theo user đăng nhập (KHÔNG nhận từ client), xem InboundReceiptQueryHelper.
 */
public class InboundReceiptSearchRequest extends AbstractRequest {

	/** Tìm theo số phiếu hoặc tên NCC. */
	public String keyword;

	/** "yyyy-MM-dd" — lọc theo receipt_date. */
	public String dateFrom;
	public String dateTo;

	/** "VALID" (còn hiệu lực) | "VOID" (đã huỷ) | "ALL" (mặc định). */
	public String statusFilter;

	public int pageIndex;
	public int pageSize;
	public String sortField;
	public String sortDirection;
}
