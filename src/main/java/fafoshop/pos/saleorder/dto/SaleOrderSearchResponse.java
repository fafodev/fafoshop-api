package fafoshop.pos.saleorder.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class SaleOrderSearchResponse extends AbstractResponse {

	public List<SaleOrderRowDto> rows = new ArrayList<>();

	/** Tổng số dòng khớp điều kiện lọc (không tính LIMIT/OFFSET) — dùng cho phân trang server-side. */
	public long totalCount;

	/** Tổng tiền hàng CỘNG DỒN trên TOÀN BỘ kết quả khớp điều kiện lọc (không chỉ trang hiện tại) — phục vụ ô tổng kết trên màn tra cứu. */
	public BigDecimal sumTotalAmount = BigDecimal.ZERO;
}
