package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;
import java.util.List;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Sửa lại TOÀN BỘ danh sách dòng hàng của 1 đơn bán đã tạo — client gửi
 * TOÀN BỘ danh sách dòng MONG MUỐN (không phải danh sách thay đổi/diff),
 * server thay hết dòng cũ (giống chiến lược `pos.product` với NCC/đơn vị
 * bán). Xem điều kiện được phép sửa (đúng người tạo + trong 15 phút, HOẶC
 * có quyền `SALE_MGR`) trong `SaleOrderUpdateProcess` và
 * docs/pos-sua-huy-don.md (gốc workspace).
 *
 * KHÔNG sửa được `customerName`/`paymentMethod` qua action này — PTTT đã có
 * action riêng (`updatepaymentmethod`), tên khách hàng ngoài phạm vi đã
 * chốt.
 */
public class SaleOrderUpdateRequest extends AbstractRequest {

	public String saleOrderNo;

	public List<SaleOrderItemDto> items;

	/** Số tiền khách trả — server validate lại phải >= subtotal MỚI tính từ items, giống SaleOrderCreateRequest. */
	public BigDecimal paidAmount;
}
