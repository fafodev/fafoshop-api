package fafoshop.pos.inboundreceipt.dto;

import java.util.List;

import fafoshop.common.dto.request.AbstractRequest;

/**
 * Tạo phiếu nhập hàng. branchCode/receiptUserCode KHÔNG nhận từ client -
 * branchCode tra theo main_branch_code của accessInfo.userCode (giống
 * SaleOrderCreateRequest), receiptUserCode = chính accessInfo.userCode.
 * supplierCode có thể để trống (chưa rõ NCC lúc hàng về, hoặc phiếu nạp tồn
 * kho đầu kỳ - xem trao đổi thiết kế màn Nhập hàng).
 */
public class InboundReceiptCreateRequest extends AbstractRequest {

	/** Mã nhà cung cấp - có thể để trống. */
	public String supplierCode;

	/** Ghi chú phiếu nhập - có thể để trống (vd "Nạp tồn kho đầu kỳ"). */
	public String note;

	/**
	 * Thông tin THAM CHIẾU hoá đơn điện tử (HĐĐT) do NCC cung cấp - tất cả có
	 * thể để trống (không phải phiếu nhập nào cũng có HĐĐT). CHỈ lưu link tra
	 * cứu + định danh hoá đơn, KHÔNG lưu file (chưa có hạ tầng lưu file, xem
	 * trao đổi thiết kế màn Nhập hàng).
	 */
	public String einvoiceNo;
	public String einvoiceSeries;
	/** Định dạng "yyyy-MM-dd". */
	public String einvoiceIssueDate;
	public String einvoiceLookupCode;
	public String einvoiceUrl;

	public List<InboundReceiptItemDto> items;
}
