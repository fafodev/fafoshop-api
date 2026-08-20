package fafoshop.pos.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fafoshop.common.dto.response.AbstractResponse;

/**
 * Response của luồng export Sổ S1a-HKD — KHÔNG đi qua JSON body thông
 * thường (xem RevenueS1aExportWebService), giống ProductExportResponse.
 */
public class RevenueS1aExportResponse extends AbstractResponse {

	@JsonIgnore
	public byte[] fileBytes;

	@JsonIgnore
	public String fileName;

	@JsonIgnore
	public String contentType;
}
