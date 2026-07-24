package fafoshop.pos.category.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class CategoryExportRequest extends AbstractRequest {

	public String keyword;
	public String categoryType;
	public String statusFilter;
	public String sortField;
	public String sortDirection;

	/** "XLSX" | "CSV" — whitelist ở CategoryExportProcess */
	public String format;
}
