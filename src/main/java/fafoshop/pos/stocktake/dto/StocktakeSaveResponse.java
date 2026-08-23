package fafoshop.pos.stocktake.dto;

import fafoshop.common.dto.response.AbstractResponse;

public class StocktakeSaveResponse extends AbstractResponse {

	/** Số dòng stock đã INSERT/UPDATE. */
	public Integer savedCount;
}
