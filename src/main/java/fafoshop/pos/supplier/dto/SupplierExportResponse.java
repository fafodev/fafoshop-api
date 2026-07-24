package fafoshop.pos.supplier.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fafoshop.common.dto.response.AbstractResponse;

/**
 * Response của luồng export — KHÔNG đi qua JSON body thông thường (xem
 * SupplierExportWebService). Các field dữ liệu file đánh dấu @JsonIgnore vì
 * chỉ webservice đọc trực tiếp trong Java, không serialize ra JSON.
 */
public class SupplierExportResponse extends AbstractResponse {

	@JsonIgnore
	public byte[] fileBytes;

	@JsonIgnore
	public String fileName;

	@JsonIgnore
	public String contentType;
}
