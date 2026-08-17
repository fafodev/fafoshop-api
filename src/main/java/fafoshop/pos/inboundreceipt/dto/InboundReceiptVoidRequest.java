package fafoshop.pos.inboundreceipt.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Huỷ 1 phiếu nhập đã tạo (set `void_flg='1'`, KHÔNG xoá cứng) + hoàn tác
 * tồn kho đã cộng lúc nhập (trừ lại, floor 0 — xem
 * InboundReceiptStockAdjuster). Xem docs/pos-sua-huy-don.md (gốc workspace).
 */
public class InboundReceiptVoidRequest extends AbstractRequest {

	public String receiptNo;
}
