package fafoshop.pos.report.dto;

import fafoshop.common.dto.request.AbstractRequest;

/** Xuất Sổ chi tiết doanh thu (Mẫu S1a-HKD) trong khoảng [dateFrom, dateTo] — cả 2 bắt buộc, định dạng "yyyy-MM-dd". */
public class RevenueS1aExportRequest extends AbstractRequest {

	public String dateFrom;
	public String dateTo;
}
