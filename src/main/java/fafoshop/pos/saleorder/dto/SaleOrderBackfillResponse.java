package fafoshop.pos.saleorder.dto;

/**
 * Kết quả bổ sung đơn bán — cùng các field với tạo đơn POS thường (số đơn,
 * tổng tiền, tiền thối, cờ duplicate) + thời điểm bán ĐÃ GHI NHẬN (để in bill
 * đúng ngày giờ bán, không phải giờ in).
 */
public class SaleOrderBackfillResponse extends SaleOrderCreateResponse {

	/** sale_order.sale_datetime đã lưu, dạng `yyyy-MM-dd HH:mm:ss`. */
	public String saleDatetime;
}
