package fafoshop.pos.saleorder.dto;

/**
 * Hằng số phương thức thanh toán của đơn bán (sale_order.payment_method) —
 * dùng chung giữa SaleOrderCreateProcess và SaleOrderUpdatePaymentMethodProcess
 * để tránh gõ tay chuỗi ký tự rải rác nhiều nơi. Xem docs/pos-in-hoa-don.md
 * (gốc workspace) để biết đầy đủ thiết kế.
 */
public final class PaymentMethod {

	public static final String CASH = "CASH";

	public static final String TRANSFER = "TRANSFER";

	private PaymentMethod() {
	}

	public static boolean isValid(String value) {
		return CASH.equals(value) || TRANSFER.equals(value);
	}
}
