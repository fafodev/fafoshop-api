package fafoshop.pos.category.dto;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.dto.response.AbstractResponse;

public class CategorySearchResponse extends AbstractResponse {

	public List<CategoryFullRowDto> rows = new ArrayList<>();

	/** Tổng số dòng khớp điều kiện lọc (không tính LIMIT/OFFSET) — dùng cho phân trang server-side. */
	public long totalCount;
}
