package fafoshop.pos.product.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class ProductExportRequest extends AbstractRequest {

	public String keyword;
	public String categoryCode;
	public String statusFilter;
	public String sortField;
	public String sortDirection;

	/** "XLSX" | "CSV" — whitelist ở ProductExportProcess */
	public String format;
}
