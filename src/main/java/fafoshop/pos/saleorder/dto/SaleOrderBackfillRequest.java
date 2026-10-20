package fafoshop.pos.saleorder.dto;

/**
 * Bổ sung 1 đơn bán ĐÃ bán thật ở 1 thời điểm trước đó nhưng quên/không kịp
 * nhập (màn "Bổ sung đơn bán", xem docs/pos-bo-sung-don-ban.md). Cùng nội
 * dung với tạo đơn POS thường (kế thừa {@link SaleOrderCreateRequest}: khách,
 * tiền khách trả, PTTT, dòng hàng, idempotencyKey) + thời điểm bán thật.
 *
 * `items[].stockCode` bị BỎ QUA — đơn bổ sung không trừ tồn kho nên không
 * chọn lô.
 */
public class SaleOrderBackfillRequest extends SaleOrderCreateRequest {

	/**
	 * Thời điểm bán thật, dạng `yyyy-MM-dd HH:mm:ss` (giờ địa phương máy chủ).
	 * Không được sau thời điểm hiện tại; không giới hạn lùi về quá khứ.
	 */
	public String saleDatetime;
}
