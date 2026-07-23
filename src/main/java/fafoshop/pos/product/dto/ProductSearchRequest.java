package fafoshop.pos.product.dto;

import fafoshop.common.dto.request.AbstractRequest;

public class ProductSearchRequest extends AbstractRequest {

	/** Từ khoá tìm theo tên sản phẩm hoặc mã vạch (JANCD); rỗng = lấy tất cả */
	public String keyword;
}
