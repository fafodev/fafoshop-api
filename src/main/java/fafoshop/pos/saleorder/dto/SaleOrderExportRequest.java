package fafoshop.pos.saleorder.dto;

import fafoshop.common.dto.request.AbstractRequest;

/** Cùng bộ lọc với SaleOrderSearchRequest nhưng KHÔNG phân trang — xuất toàn bộ kết quả khớp filter. */
public class SaleOrderExportRequest extends AbstractRequest {

	public String keyword;
	public String dateFrom;
	public String dateTo;
	public String paymentMethod;
	public String statusFilter;
	public String cashierKeyword;
	public String sortField;
	public String sortDirection;

	/** "XLSX" | "CSV" */
	public String format;
}
