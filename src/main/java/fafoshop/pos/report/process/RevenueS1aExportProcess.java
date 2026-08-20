package fafoshop.pos.report.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
import fafoshop.pos.report.dto.RevenueS1aExportRequest;
import fafoshop.pos.report.dto.RevenueS1aExportResponse;

/**
 * Xuất Sổ chi tiết doanh thu bán hàng hóa, dịch vụ (Mẫu S1a-HKD, kèm theo
 * Thông tư 152/2025/TT-BTC) — phục vụ hộ kinh doanh thông báo doanh thu và
 * đối chiếu khi cơ quan thuế yêu cầu (nhóm doanh thu ≤1 tỷ/năm, KHÔNG bắt
 * buộc nộp sổ này, chỉ cần có sẵn để đối chiếu — xem
 * Thue/KY_KHAI_BAO_THUE_HKD_DUOI_1_TY.md mục 3b ở gốc workspace).
 *
 * Cấu trúc cột đã thống nhất (mục 3c cùng tài liệu): A=Ngày tháng,
 * B=Diễn giải, 1=Số tiền (tổng, bắt buộc nhà nước) = Tiền mặt + Chuyển
 * khoản, 2=Tiền mặt, 3=Chuyển khoản (2 cột tự thêm hợp lệ). Ghi THEO NGÀY
 * (1 dòng = tổng doanh thu 1 ngày), KHÔNG ghi theo từng hoá đơn — hợp lệ vì
 * hướng dẫn ghi cột Diễn giải cho phép ghi theo nghiệp vụ HOẶC theo
 * ngày/tháng. Chỉ liệt kê những ngày THỰC SỰ có doanh thu (revenue &gt; 0).
 *
 * Nguồn doanh thu: SUM(sale_order_item.line_amount), loại đơn đã huỷ
 * (void_flg='1') — giống DashboardSummaryProcess.queryPaymentSplit, mở rộng
 * thêm GROUP BY theo ngày.
 */
public class RevenueS1aExportProcess extends AbstractProcess {

	private static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private static final short COL_WIDTH_A = 16 * 256;
	private static final short COL_WIDTH_B = 34 * 256;
	private static final short COL_WIDTH_NUMBER = 14 * 256;

	public RevenueS1aExportProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new RevenueS1aExportResponse();
	}

	@Override
	protected String getFuncId() {
		return "RPT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		RevenueS1aExportRequest req = (RevenueS1aExportRequest) request;
		RevenueS1aExportResponse res = (RevenueS1aExportResponse) response;

		LocalDate from = parseRequiredDate(req.dateFrom);
		LocalDate to = parseRequiredDate(req.dateTo);
		if (from.isAfter(to)) {
			throwError("ME000118");
		}

		String branchCode = getUserBranchCode(dba, req.accessInfo.userCode);
		HkdInfoRow hkdInfo = queryHkdInfo(dba, branchCode);
		Map<LocalDate, BigDecimal[]> dailyRevenue = queryDailyPaymentSplit(dba, branchCode, from, to);

		res.fileBytes = buildXlsx(hkdInfo, from, to, dailyRevenue);
		res.fileName = "so_doanh_thu_s1a_hkd_" + from.format(DateTimeFormatter.BASIC_ISO_DATE) + "_"
				+ to.format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
		res.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

		return res;
	}

	private LocalDate parseRequiredDate(String value) throws ProcessCheckErrorException {
		if (value == null || value.trim().isEmpty()) {
			throwError("ME000116");
		}
		try {
			return LocalDate.parse(value.trim(), ISO_DATE_FMT);
		} catch (Exception e) {
			throwError("ME000117");
			return null; // không bao giờ tới đây - throwError luôn ném exception
		}
	}

	private String getUserBranchCode(DBAccessor dba, String userCode) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT main_branch_code FROM app_user WHERE user_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			rs = ps.executeQuery();
			String branchCode = rs.next() ? rs.getString("main_branch_code") : null;
			if (branchCode == null || branchCode.trim().isEmpty()) {
				throwError("ME000088");
			}
			return branchCode;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/** configured=false nếu chi nhánh CHƯA cấu hình hkd_info — không phải lỗi, sổ vẫn xuất được với phần đầu để trống. */
	private HkdInfoRow queryHkdInfo(DBAccessor dba, String branchCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT hkd_name, address, tax_code, phone, email FROM hkd_info "
					+ "WHERE branch_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			rs = ps.executeQuery();

			HkdInfoRow row = new HkdInfoRow();
			if (rs.next()) {
				row.hkdName = rs.getString("hkd_name");
				row.address = rs.getString("address");
				row.taxCode = rs.getString("tax_code");
				row.phone = rs.getString("phone");
				row.email = rs.getString("email");
			}
			return row;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private Map<LocalDate, BigDecimal[]> queryDailyPaymentSplit(DBAccessor dba, String branchCode, LocalDate from,
			LocalDate to) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		Map<LocalDate, BigDecimal[]> result = new TreeMap<>();
		try {
			String sql = "SELECT DATE(so.sale_datetime) AS sale_date, so.payment_method, "
					+ "COALESCE(SUM(soi.line_amount), 0) AS revenue "
					+ "FROM sale_order so "
					+ "JOIN sale_order_item soi ON soi.sale_order_no = so.sale_order_no "
					+ "WHERE so.branch_code = ? AND so.void_flg = '0' AND DATE(so.sale_datetime) BETWEEN ? AND ? "
					+ "GROUP BY DATE(so.sale_datetime), so.payment_method "
					+ "ORDER BY sale_date";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setDate(2, Date.valueOf(from));
			ps.setDate(3, Date.valueOf(to));
			rs = ps.executeQuery();

			while (rs.next()) {
				LocalDate saleDate = rs.getDate("sale_date").toLocalDate();
				String paymentMethod = rs.getString("payment_method");
				BigDecimal revenue = rs.getBigDecimal("revenue");

				BigDecimal[] pair = result.computeIfAbsent(saleDate, d -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
				if ("TRANSFER".equals(paymentMethod)) {
					pair[1] = revenue;
				} else {
					pair[0] = revenue;
				}
			}
			return result;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/**
	 * Dựng file Excel theo đúng thiết kế đã thống nhất
	 * (Thue/MAU_SO_S1A-HKD_THIET_KE_FAFOSHOP.xlsx ở gốc workspace) — khác
	 * file mẫu ở chỗ đây là báo cáo TỰ ĐỘNG điền số liệu thật (không phải
	 * template để điền tay) nên không tô màu xanh dương đánh dấu "ô cần tự
	 * điền" như file mẫu gốc.
	 */
	private byte[] buildXlsx(HkdInfoRow hkdInfo, LocalDate from, LocalDate to,
			Map<LocalDate, BigDecimal[]> dailyRevenue) throws FatalException {
		try (Workbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("So doanh thu S1a-HKD");
			sheet.setColumnWidth(0, COL_WIDTH_A);
			sheet.setColumnWidth(1, COL_WIDTH_B);
			sheet.setColumnWidth(2, COL_WIDTH_NUMBER);
			sheet.setColumnWidth(3, COL_WIDTH_NUMBER);
			sheet.setColumnWidth(4, COL_WIDTH_NUMBER);

			Font normalFont = createFont(wb, false, false, (short) 11);
			Font boldFont = createFont(wb, true, false, (short) 11);
			Font italicSmallFont = createFont(wb, false, true, (short) 9);

			CellStyle plainStyle = wb.createCellStyle();
			plainStyle.setFont(normalFont);

			CellStyle boldRightStyle = wb.createCellStyle();
			boldRightStyle.setFont(boldFont);
			boldRightStyle.setAlignment(HorizontalAlignment.RIGHT);
			boldRightStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle italicRightWrapStyle = wb.createCellStyle();
			italicRightWrapStyle.setFont(italicSmallFont);
			italicRightWrapStyle.setAlignment(HorizontalAlignment.RIGHT);
			italicRightWrapStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			italicRightWrapStyle.setWrapText(true);

			CellStyle titleStyle = wb.createCellStyle();
			titleStyle.setFont(boldFont);
			titleStyle.setAlignment(HorizontalAlignment.CENTER);
			titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle headerStyle = createBorderedStyle(wb, boldFont, HorizontalAlignment.CENTER, true);
			CellStyle bodyDateStyle = createBorderedStyle(wb, normalFont, HorizontalAlignment.CENTER, false);
			CellStyle bodyTextStyle = createBorderedStyle(wb, normalFont, HorizontalAlignment.LEFT, false);
			CellStyle bodyNumberStyle = createBorderedStyle(wb, normalFont, HorizontalAlignment.RIGHT, false);
			CellStyle totalLabelStyle = createBorderedStyle(wb, boldFont, HorizontalAlignment.RIGHT, false);
			CellStyle totalNumberStyle = createBorderedStyle(wb, boldFont, HorizontalAlignment.RIGHT, false);

			// Định dạng số tiền có dấu phân cách nghìn (VND không có phần lẻ thập
			// phân) — áp cho các cột Số tiền/Tiền mặt/Chuyển khoản và dòng Tổng cộng.
			short vndFormat = wb.createDataFormat().getFormat("#,##0");
			bodyNumberStyle.setDataFormat(vndFormat);
			totalNumberStyle.setDataFormat(vndFormat);

			CellStyle signDateStyle = wb.createCellStyle();
			signDateStyle.setFont(italicSmallFont);
			signDateStyle.setAlignment(HorizontalAlignment.CENTER);
			signDateStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			CellStyle signLabelStyle = wb.createCellStyle();
			signLabelStyle.setFont(boldFont);
			signLabelStyle.setAlignment(HorizontalAlignment.CENTER);
			signLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			signLabelStyle.setWrapText(true);

			CellStyle noteStyle = wb.createCellStyle();
			noteStyle.setFont(italicSmallFont);

			int r = 0;
			createCell(sheet.createRow(r), 0, "HỘ, CÁ NHÂN KINH DOANH: " + emptyIfNull(hkdInfo.hkdName), plainStyle);
			// "Mẫu số ..." đặt trong khối gộp C:E (không phải chỉ 1 cột C rộng 14 ký
			// tự) — đúng quy ước trình bày góc phải trên cùng của mẫu biểu kế toán/
			// hành chính (khối "Mẫu số .../Ban hành kèm theo ..." luôn chiếm 1 vùng
			// đủ rộng, không dồn hết vào 1 cột hẹp khiến câu chú thích vỡ thành 6-7
			// dòng như bản trước — phát hiện lúc xem lại bản in thử).
			createCell(sheet.getRow(r), 2, "Mẫu số S1a-HKD", boldRightStyle);
			sheet.addMergedRegion(new CellRangeAddress(r, r, 2, 4));
			r++;

			// POI KHÔNG tự tính lại chiều cao dòng cho ô wrap-text (khác Excel khi
			// người dùng gõ tay) — nếu không set tường minh, dòng chú thích 2 dòng
			// này bị cắt/tràn đè lên dòng bên dưới khi mở file (lỗi đã gặp thực tế:
			// mất dòng thứ 2 "tháng 12 năm 2025..."). Set cao 28pt đủ cho 2 dòng
			// chữ nghiêng cỡ 9 — cùng cách headerRow1 bên dưới đã làm đúng cho ô
			// "Số tiền\n(tổng)".
			Row addressRow = sheet.createRow(r);
			addressRow.setHeightInPoints(28f);
			createCell(addressRow, 0, "Địa chỉ: " + emptyIfNull(hkdInfo.address), plainStyle);
			createCell(addressRow, 2,
					"(Kèm theo Thông tư số 152/2025/TT-BTC ngày 31 tháng 12 năm 2025 của Bộ trưởng Bộ Tài chính)",
					italicRightWrapStyle);
			sheet.addMergedRegion(new CellRangeAddress(r, r, 2, 4));
			r++;

			createCell(sheet.createRow(r), 0, "Mã số thuế: " + emptyIfNull(hkdInfo.taxCode), plainStyle);
			r++;

			// Điện thoại/email KHÔNG thuộc mẫu gốc nhà nước (chỉ có tên/địa chỉ/MST) —
			// hộ kinh doanh tự bổ sung để dễ liên hệ khi cần, chỉ in dòng nào có giá trị.
			if (hasText(hkdInfo.phone)) {
				createCell(sheet.createRow(r), 0, "Điện thoại: " + hkdInfo.phone, plainStyle);
				r++;
			}
			if (hasText(hkdInfo.email)) {
				createCell(sheet.createRow(r), 0, "Email: " + hkdInfo.email, plainStyle);
				r++;
			}

			r++; // dòng trống (giống file mẫu: dòng trống trước tiêu đề)
			r++;

			Row titleRow = sheet.createRow(r);
			createCell(titleRow, 0, "SỔ CHI TIẾT DOANH THU BÁN HÀNG HÓA, DỊCH VỤ", titleStyle);
			sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 4));
			r++;

			createCell(sheet.createRow(r), 0, "Địa điểm kinh doanh: " + emptyIfNull(hkdInfo.address), plainStyle);
			r++;

			createCell(sheet.createRow(r), 0,
					"Kỳ kê khai: Từ " + from.format(DISPLAY_DATE_FMT) + " đến " + to.format(DISPLAY_DATE_FMT),
					plainStyle);
			r++;

			r++; // dòng trống trước bảng

			Row headerRow1 = sheet.createRow(r);
			headerRow1.setHeightInPoints(30f);
			createCell(headerRow1, 0, "Ngày, tháng", headerStyle);
			createCell(headerRow1, 1, "Diễn giải", headerStyle);
			createCell(headerRow1, 2, "Số tiền\n(tổng)", headerStyle);
			createCell(headerRow1, 3, "Tiền mặt", headerStyle);
			createCell(headerRow1, 4, "Chuyển khoản", headerStyle);
			r++;

			Row headerRow2 = sheet.createRow(r);
			createCell(headerRow2, 0, "A", headerStyle);
			createCell(headerRow2, 1, "B", headerStyle);
			createCell(headerRow2, 2, "1", headerStyle);
			createCell(headerRow2, 3, "2", headerStyle);
			createCell(headerRow2, 4, "3", headerStyle);
			r++;

			int firstDataRow = r;
			for (Map.Entry<LocalDate, BigDecimal[]> entry : dailyRevenue.entrySet()) {
				LocalDate date = entry.getKey();
				BigDecimal cash = entry.getValue()[0];
				BigDecimal transfer = entry.getValue()[1];
				if (cash.signum() == 0 && transfer.signum() == 0) {
					continue; // ngày không phát sinh doanh thu thật (chỉ có dòng payment_method rỗng) - bỏ qua
				}

				Row dataRow = sheet.createRow(r);
				createCell(dataRow, 0, date.format(DISPLAY_DATE_FMT), bodyDateStyle);
				createCell(dataRow, 1, "Doanh thu bán hàng ngày " + date.format(DISPLAY_DATE_FMT), bodyTextStyle);
				Cell totalCell = dataRow.createCell(2);
				totalCell.setCellFormula("D" + (r + 1) + "+E" + (r + 1));
				totalCell.setCellStyle(bodyNumberStyle);
				dataRow.createCell(3).setCellValue(cash.doubleValue());
				dataRow.getCell(3).setCellStyle(bodyNumberStyle);
				dataRow.createCell(4).setCellValue(transfer.doubleValue());
				dataRow.getCell(4).setCellStyle(bodyNumberStyle);
				r++;
			}
			int lastDataRow = r - 1;

			Row totalRow = sheet.createRow(r);
			createCell(totalRow, 0, "", totalLabelStyle);
			createCell(totalRow, 1, "Tổng cộng", totalLabelStyle);
			if (lastDataRow >= firstDataRow) {
				Cell totalC = totalRow.createCell(2);
				totalC.setCellFormula("SUM(C" + (firstDataRow + 1) + ":C" + (lastDataRow + 1) + ")");
				totalC.setCellStyle(totalNumberStyle);
				Cell totalD = totalRow.createCell(3);
				totalD.setCellFormula("SUM(D" + (firstDataRow + 1) + ":D" + (lastDataRow + 1) + ")");
				totalD.setCellStyle(totalNumberStyle);
				Cell totalE = totalRow.createCell(4);
				totalE.setCellFormula("SUM(E" + (firstDataRow + 1) + ":E" + (lastDataRow + 1) + ")");
				totalE.setCellStyle(totalNumberStyle);
			} else {
				createCell(totalRow, 2, "0", totalNumberStyle);
				createCell(totalRow, 3, "0", totalNumberStyle);
				createCell(totalRow, 4, "0", totalNumberStyle);
			}
			r++;

			if (lastDataRow < firstDataRow) {
				createCell(sheet.createRow(r), 0,
						"Không phát sinh doanh thu trong kỳ đã chọn.", noteStyle);
				r++;
			}

			r += 2; // 2 dòng trống trước phần ký tên

			createCell(sheet.createRow(r), 3, "Ngày ... tháng ... năm ...", signDateStyle);
			r++;

			Row signRow = sheet.createRow(r);
			// Tương tự dòng chú thích Thông tư ở trên — nhãn ký tên xuống 2 dòng
			// ("NGƯỜI ĐẠI DIỆN HỘ KINH DOANH/" + "CÁ NHÂN KINH DOANH"), phải set cao
			// tường minh nếu không dòng thứ 2 bị mất khi mở file.
			signRow.setHeightInPoints(30f);
			createCell(signRow, 3, "NGƯỜI ĐẠI DIỆN HỘ KINH DOANH/\nCÁ NHÂN KINH DOANH", signLabelStyle);
			sheet.addMergedRegion(new CellRangeAddress(r, r, 3, 4));
			int lastUsedRow = r;

			configurePrintLayout(wb, sheet, lastUsedRow, firstDataRow);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			return bos.toByteArray();
		} catch (IOException e) {
			throw new FatalException(e);
		}
	}

	/**
	 * GIỮ khổ dọc (portrait) — đúng quy ước trình bày mẫu biểu/sổ sách kế
	 * toán hành chính (luôn A4 dọc để lưu hồ sơ/đóng tập), KHÔNG tự đổi qua
	 * khổ ngang. Chỉ dùng "co vừa 1 trang theo chiều ngang" (fitWidth=1,
	 * Excel tự tính % scale in khi 5 cột vượt khổ giấy) để khắc phục việc cột
	 * "Chuyển khoản" và phần ký tên (D19:E19) bị đẩy sang trang in thứ 2 —
	 * đây là cách sửa tràn trang KHÔNG cần đổi hướng giấy. fitHeight=0 (tự
	 * động, KHÔNG ép 1 trang) để bảng nhiều ngày vẫn tự xuống trang tiếp theo
	 * bình thường, chỉ ép chiều ngang. Đặt printArea đúng bằng vùng có nội
	 * dung thật (không tính hết khổ giấy) để tránh in ra trang trắng dư.
	 */
	private void configurePrintLayout(Workbook wb, Sheet sheet, int lastUsedRow, int firstDataRow) {
		PrintSetup printSetup = sheet.getPrintSetup();
		printSetup.setLandscape(false);
		printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
		printSetup.setFitWidth((short) 1);
		printSetup.setFitHeight((short) 0);
		sheet.setFitToPage(true);
		sheet.setHorizontallyCenter(true);

		sheet.setMargin(Sheet.LeftMargin, 0.5);
		sheet.setMargin(Sheet.RightMargin, 0.5);
		sheet.setMargin(Sheet.TopMargin, 0.6);
		sheet.setMargin(Sheet.BottomMargin, 0.6);
		sheet.setMargin(Sheet.HeaderMargin, 0.3);
		sheet.setMargin(Sheet.FooterMargin, 0.3);

		int sheetIndex = wb.getSheetIndex(sheet);
		wb.setPrintArea(sheetIndex, 0, 4, 0, lastUsedRow);

		// Lặp lại 2 dòng tiêu đề cột (header/A-B-1-2-3) ở đầu MỖI trang in, phòng
		// trường hợp bảng dài quá 1 trang (nhiều ngày phát sinh doanh thu).
		sheet.setRepeatingRows(new CellRangeAddress(firstDataRow - 2, firstDataRow - 1, 0, 4));
	}

	private CellStyle createBorderedStyle(Workbook wb, Font font, HorizontalAlignment alignment, boolean wrap) {
		CellStyle style = wb.createCellStyle();
		style.setFont(font);
		style.setAlignment(alignment);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setWrapText(wrap);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	private Font createFont(Workbook wb, boolean bold, boolean italic, short sizePoints) {
		Font font = wb.createFont();
		font.setFontName("Arial");
		font.setBold(bold);
		font.setItalic(italic);
		font.setFontHeightInPoints(sizePoints);
		return font;
	}

	private void createCell(Row row, int colIndex, String value, CellStyle style) {
		Cell cell = row.createCell(colIndex);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private String emptyIfNull(String value) {
		return value != null ? value : "";
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}

	private void closeQuietly(ResultSet rs, DBStatement ps) throws DBException {
		try {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}

	private static class HkdInfoRow {
		String hkdName;
		String address;
		String taxCode;
		String phone;
		String email;
	}
}
