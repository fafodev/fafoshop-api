package fafoshop.pos.supplier.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class SupplierListResponse extends AbstractResponse {

	public List<SupplierListRowDto> rows = new ArrayList<>();
}
