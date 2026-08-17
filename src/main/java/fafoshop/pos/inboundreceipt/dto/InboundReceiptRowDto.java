package fafoshop.pos.inboundreceipt.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.AbstractDto;

/** 1 dòng kết quả tra cứu phiếu nhập — khớp bảng inbound_receipt + tổng hợp từ inbound_receipt_item. */
public class InboundReceiptRowDto extends AbstractDto {

	public String receiptNo;
	public String branchCode;
	public String supplierCode;
	public String supplierName;
	/** Định dạng "yyyy-MM-dd". */
	public String receiptDate;
	public String note;
	public String receiptUserCode;
	public String receiptUserName;
	public String voidFlg;
	public int itemCount;
	public BigDecimal totalAmount;
}
