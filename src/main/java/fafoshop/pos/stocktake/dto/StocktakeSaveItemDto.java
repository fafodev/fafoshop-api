package fafoshop.pos.stocktake.dto;

/**
 * 1 dòng kiểm kê gửi lên lúc lưu — chỉ các dòng đã đổi
 * (docs/pos-kiem-ke.md).
 */
public class StocktakeSaveItemDto {

	public String productCode;

	/** Mã lô (stock.stock_code). null = tạo lô mới. */
	public String stockCode;

	/**
	 * Số đếm theo đơn vị lẻ. Có mặt kể cả 0 = đã đếm. null/không gửi = không
	 * đếm số (chỉ đổi hạn dùng khi expiryChanged=true).
	 */
	public Integer countedQty;

	/**
	 * true = ghi hạn dùng mới (kể cả xóa thành trống). false/null = giữ nguyên
	 * hạn dùng hiện có.
	 */
	public Boolean expiryChanged;

	/** Định dạng "yyyy-MM-dd". Chỉ có ý nghĩa khi expiryChanged=true. */
	public String expiryDate;
}
