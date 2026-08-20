package fafoshop.pos.hkdinfo.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Lấy thông tin hộ kinh doanh (tên/địa chỉ/mã số thuế) của chi nhánh NGƯỜI
 * ĐANG ĐĂNG NHẬP (không nhận branchCode từ client) — dùng để in ở đầu Sổ
 * S1a-HKD khi xuất báo cáo doanh thu (RevenueS1aExportProcess). Không có
 * field nào khác ngoài accessInfo (kế thừa từ AbstractRequest).
 */
public class HkdInfoGetRequest extends AbstractRequest {
}
