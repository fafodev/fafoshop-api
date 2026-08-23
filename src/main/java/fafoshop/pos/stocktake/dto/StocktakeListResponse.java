package fafoshop.pos.stocktake.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class StocktakeListResponse extends AbstractResponse {

	public List<StocktakeRowDto> rows = new ArrayList<>();
}
