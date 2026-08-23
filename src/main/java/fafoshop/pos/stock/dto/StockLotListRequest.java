package fafoshop.pos.stock.dto;

import fafoshop.common.dto.request.AbstractRequest;

/** Chi nhánh luôn theo user đăng nhập, không nhận từ client. */
public class StockLotListRequest extends AbstractRequest {

	public String productCode;
}
