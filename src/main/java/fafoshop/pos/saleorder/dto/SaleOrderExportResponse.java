package fafoshop.pos.saleorder.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class SaleOrderExportResponse extends AbstractResponse {

	public byte[] fileBytes;
	public String fileName;
	public String contentType;
}
