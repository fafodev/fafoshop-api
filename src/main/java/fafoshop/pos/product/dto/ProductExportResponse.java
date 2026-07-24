package fafoshop.pos.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import fafoshop.common.dto.response.AbstractResponse;

/**
 * Response của luồng export — KHÔNG đi qua JSON body thông thường (xem
 * ProductExportWebService). Các field dữ liệu file đánh dấu @JsonIgnore vì
 * chỉ webservice đọc trực tiếp trong Java, không serialize ra JSON.
 */
public class ProductExportResponse extends AbstractResponse {

	@JsonIgnore
	public byte[] fileBytes;

	@JsonIgnore
	public String fileName;

	@JsonIgnore
	public String contentType;
}
