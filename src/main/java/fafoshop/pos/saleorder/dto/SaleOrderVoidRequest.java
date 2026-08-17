package fafoshop.pos.saleorder.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Huỷ 1 đơn bán đã tạo (set `void_flg='1'`, KHÔNG xoá cứng) + hoàn tác
 * tồn kho đã trừ lúc bán. Xem điều kiện được phép huỷ trong
 * `SaleOrderVoidProcess` và docs/pos-sua-huy-don.md (gốc workspace).
 */
public class SaleOrderVoidRequest extends AbstractRequest {

	public String saleOrderNo;
}
