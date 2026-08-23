package fafoshop.pos.stock.dto;

import fafoshop.common.dto.AbstractDto;

/**
 * 1 lô tồn còn hàng của sản phẩm. stockQty luôn có (int) — không nullable.
 * expiryDate nullable (AbstractDto lược null).
 */
public class StockLotRowDto extends AbstractDto {

	public String stockCode;
	/** Định dạng yyyy-MM-dd. */
	public String expiryDate;
	public int stockQty;
}
