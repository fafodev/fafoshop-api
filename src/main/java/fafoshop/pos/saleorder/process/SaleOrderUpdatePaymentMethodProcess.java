package fafoshop.pos.saleorder.process;

import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.MessageUtility;
import fafoshop.pos.saleorder.dto.PaymentMethod;
import fafoshop.pos.saleorder.dto.SaleOrderUpdatePaymentMethodRequest;
import fafoshop.pos.saleorder.dto.SaleOrderUpdatePaymentMethodResponse;

/**
 * Sửa phương thức thanh toán của đơn bán VỪA TẠO (Phương án A trong
 * docs/pos-in-hoa-don.md) — vd khách chọn chuyển khoản, đã in bill kèm QR,
 * nhưng đổi ý trả tiền mặt ngay tại quầy. Sửa THẲNG trên đơn cũ (không tạo
 * đơn mới) để báo cáo doanh thu không bị đúp; frontend tự in lại bill (không
 * QR nếu đổi sang CASH) và đánh dấu "BẢN IN THAY THẾ".
 *
 * Chỉ cho sửa khi ĐỦ CẢ 3 điều kiện (chống sửa bậy đơn cũ qua gọi API trực
 * tiếp, không phải thao tác thật tại quầy): đơn còn hiệu lực (void_flg='0'),
 * do ĐÚNG thu ngân đang đăng nhập tạo, và còn trong vòng 15 phút kể từ lúc
 * tạo. Không đạt 1 trong 3 → coi như "không tìm thấy đơn hợp lệ để sửa"
 * (không phân biệt lý do cụ thể trong thông báo, tránh lộ thông tin đơn của
 * người khác).
 */
public class SaleOrderUpdatePaymentMethodProcess extends AbstractProcess {

	private static final String PRG_CD = "SALE_PAY";

	/** Số phút tối đa cho phép sửa phương thức thanh toán kể từ lúc tạo đơn. */
	private static final int EDIT_WINDOW_MINUTES = 15;

	public SaleOrderUpdatePaymentMethodProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderUpdatePaymentMethodResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_PAY";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderUpdatePaymentMethodRequest req = (SaleOrderUpdatePaymentMethodRequest) request;
		SaleOrderUpdatePaymentMethodResponse res = (SaleOrderUpdatePaymentMethodResponse) response;

		validateSaleOrderNo(req.saleOrderNo);
		validatePaymentMethod(req.paymentMethod);

		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE sale_order SET ");
			sql.append("payment_method = ?, update_user_code = ?, update_program = ? ");
			sql.append("WHERE sale_order_no = ? AND void_flg = '0' AND entry_user_code = ? ");
			sql.append("AND entry_datetime >= (NOW() - INTERVAL ").append(EDIT_WINDOW_MINUTES).append(" MINUTE)");

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.paymentMethod);
			ps.setString(2, req.accessInfo.userCode);
			ps.setString(3, PRG_CD);
			ps.setString(4, req.saleOrderNo);
			ps.setString(5, req.accessInfo.userCode);

			int affected = ps.executeUpdate();
			if (affected == 0) {
				throwError("ME000099");
			}

			res.saleOrderNo = req.saleOrderNo;
			res.paymentMethod = req.paymentMethod;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private void validateSaleOrderNo(String saleOrderNo) throws ProcessCheckErrorException {
		if (saleOrderNo == null || saleOrderNo.trim().isEmpty()) {
			// Trước đây dùng lại "ME000100" — TRÙNG key với DashboardSummaryProcess
			// (Properties chỉ giữ giá trị của dòng cuối cùng trong file, khiến màn
			// Tổng quan hiển thị nhầm thông báo này khi gõ sai định dạng ngày). Phát
			// hiện lúc làm màn tra cứu đơn bán, sửa sang mã riêng "ME000120".
			throwError("ME000120");
		}
	}

	private void validatePaymentMethod(String paymentMethod) throws ProcessCheckErrorException {
		if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
			throwError("ME000097");
		}
		if (!PaymentMethod.isValid(paymentMethod)) {
			throwError("ME000098");
		}
	}

	private void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}
}
