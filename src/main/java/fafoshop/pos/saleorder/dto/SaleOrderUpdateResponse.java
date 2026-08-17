package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;

import fafoshop.common.dto.response.AbstractResponse;

public class SaleOrderUpdateResponse extends AbstractResponse {

	public String saleOrderNo;

	public BigDecimal subtotal;

	public BigDecimal changeAmount;
}
