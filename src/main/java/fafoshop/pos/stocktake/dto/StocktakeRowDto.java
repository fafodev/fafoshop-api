package fafoshop.pos.stocktake.dto;

import fafoshop.common.dto.AbstractDto;

/**
 * 1 dòng lưới kiểm kê. CỐ Ý không có stockQty — màn hình không hiện tồn
 * hệ thống (docs/pos-kiem-ke.md). barcode/expiryDate/unitName nullable:
 * frontend phải check cả null lẫn undefined (AbstractDto lược field null).
 */
public class StocktakeRowDto extends AbstractDto {

	public String productCode;
	public String name;
	public String barcode;
	public String unitName;
	public String stockCode;
	/** Định dạng "yyyy-MM-dd". */
	public String expiryDate;
}
