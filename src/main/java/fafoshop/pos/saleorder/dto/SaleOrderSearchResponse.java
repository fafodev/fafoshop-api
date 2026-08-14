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

	/**
	 * Tổng LÃI cộng dồn — CHỈ tính trên các đơn ĐÃ XÁC ĐỊNH được đầy đủ giá
	 * vốn (profitAmount != null ở SaleOrderRowDto), KHÔNG tính đơn nào có giá
	 * vốn = 0 để bù vào (tránh phóng đại lãi). Xem unknownCostOrderCount để
	 * biết số đơn KHÔNG được tính vào tổng này.
	 */
	public BigDecimal sumProfitAmount = BigDecimal.ZERO;

	/** Số đơn (trong TOÀN BỘ kết quả khớp filter) chưa xác định được lãi vì có ít nhất 1 dòng hàng chưa từng có phiếu nhập — hiển thị cho người dùng biết sumProfitAmount KHÔNG bao gồm các đơn này. */
	public long unknownCostOrderCount;
}
