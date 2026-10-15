package fafoshop.pos.sync.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Yêu cầu kéo toàn bộ dữ liệu bán hàng offline cho app iOS FaFoMart POS.
 * Không có tham số: luôn trả FULL danh mục sản phẩm đang hiệu lực + tài khoản
 * ngân hàng của chi nhánh thu ngân đang đăng nhập (chi nhánh tra theo
 * accessInfo.userCode, KHÔNG nhận từ client). Xem docs/pos-ios-app.md.
 */
public class SyncPullRequest extends AbstractRequest {
}
