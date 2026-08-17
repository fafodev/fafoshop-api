package fafoshop.pos.inboundreceipt.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class InboundReceiptSearchResponse extends AbstractResponse {

	public List<InboundReceiptRowDto> rows = new ArrayList<>();
	public long totalCount;
	/** Tổng tiền nhập CỘNG DỒN toàn bộ kết quả khớp filter (không chỉ trang hiện tại) — mirror SaleOrderSearchResponse.sumTotalAmount. */
	public BigDecimal sumTotalAmount = BigDecimal.ZERO;
}
