package fafoshop.pos.bankaccount.dto;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Lấy tài khoản ngân hàng nhận tiền của chi nhánh THU NGÂN ĐANG ĐĂNG NHẬP
 * (không nhận branchCode từ client) — dùng để build mã QR chuyển khoản lúc
 * in hoá đơn POS. Không có field nào khác ngoài accessInfo (kế thừa từ
 * AbstractRequest).
 */
public class BankAccountGetRequest extends AbstractRequest {
}
