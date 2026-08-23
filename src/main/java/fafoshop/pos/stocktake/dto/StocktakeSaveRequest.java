package fafoshop.pos.stocktake.dto;

import java.util.List;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Lưu kiểm kê. Chi nhánh luôn theo user đăng nhập (KHÔNG nhận từ client).
 */
public class StocktakeSaveRequest extends AbstractRequest {

	public List<StocktakeSaveItemDto> items;
}
