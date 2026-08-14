package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class SaleOrderDetailResponse extends AbstractResponse {

	public String saleOrderNo;
	public String branchCode;
	public String customerName;

	/** Định dạng "yyyy-MM-dd HH:mm:ss". */
	public String saleDatetime;

	public String paymentMethod;
	public BigDecimal paidAmount;
	public BigDecimal changeAmount;
	public BigDecimal subtotal;
	public String cashierUserCode;
	public String cashierName;
	public String voidFlg;

	public List<SaleOrderDetailItemDto> items = new ArrayList<>();
}
