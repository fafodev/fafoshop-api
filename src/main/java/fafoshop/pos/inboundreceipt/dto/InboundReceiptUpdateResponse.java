package fafoshop.pos.inboundreceipt.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.response.AbstractResponse;

public class InboundReceiptUpdateResponse extends AbstractResponse {

	public String receiptNo;
	public BigDecimal totalAmount;
}
