package fafoshop.pos.inboundreceipt.dto;

import java.util.List;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Sửa lại TOÀN BỘ danh sách dòng hàng + thông tin đầu phiếu của 1 phiếu
 * nhập đã tạo — mirror SaleOrderUpdateRequest (chiến lược "thay hết").
 * Xem điều kiện được sửa trong InboundReceiptUpdateProcess và
 * docs/pos-sua-huy-don.md (gốc workspace).
 */
public class InboundReceiptUpdateRequest extends AbstractRequest {

	public String receiptNo;

	public String supplierCode;
	public String note;
	public String einvoiceNo;
	public String einvoiceSeries;
	public String einvoiceIssueDate;
	public String einvoiceLookupCode;
	public String einvoiceUrl;

	public List<InboundReceiptItemDto> items;
}
