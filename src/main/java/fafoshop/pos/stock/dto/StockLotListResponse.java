package fafoshop.pos.stock.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class StockLotListResponse extends AbstractResponse {

	public List<StockLotRowDto> rows = new ArrayList<>();
}
