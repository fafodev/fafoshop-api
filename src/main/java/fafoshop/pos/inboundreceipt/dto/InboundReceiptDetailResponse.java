package fafoshop.pos.inboundreceipt.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class InboundReceiptDetailResponse extends AbstractResponse {

	public String receiptNo;
	public String branchCode;
	public String supplierCode;
	public String supplierName;
	/** "yyyy-MM-dd". */
	public String receiptDate;
	public String note;
	public String einvoiceNo;
	public String einvoiceSeries;
	public String einvoiceIssueDate;
	public String einvoiceLookupCode;
	public String einvoiceUrl;
	public String receiptUserCode;
	public String receiptUserName;
	public String voidFlg;
	public BigDecimal totalAmount;
	public List<InboundReceiptDetailItemDto> items = new ArrayList<>();
}
