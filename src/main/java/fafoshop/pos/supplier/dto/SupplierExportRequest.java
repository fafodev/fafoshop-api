package fafoshop.pos.supplier.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class SupplierExportRequest extends AbstractRequest {

	public String keyword;
	public String statusFilter;
	public String sortField;
	public String sortDirection;

	/** "XLSX" | "CSV" — whitelist ở SupplierExportProcess */
	public String format;
}
