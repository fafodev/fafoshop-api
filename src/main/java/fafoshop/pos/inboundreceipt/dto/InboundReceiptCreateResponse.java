package fafoshop.pos.inboundreceipt.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.response.AbstractResponse;

public class InboundReceiptCreateResponse extends AbstractResponse {

	/** Số phiếu nhập vừa tạo (inbound_receipt.receipt_no). */
	public String receiptNo;

	/** Tổng tiền nhập - server tính lại từ các dòng hàng, không tin số client gửi. */
	public BigDecimal totalAmount;
}
