package fafoshop.pos.saleorder.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.CommonUtility;
import fafoshop.pos.saleorder.dto.PaymentMethod;
import fafoshop.pos.saleorder.dto.SaleOrderExportRequest;
import fafoshop.pos.saleorder.dto.SaleOrderExportResponse;
import fafoshop.pos.saleorder.dto.SaleOrderRowDto;

/**
 * Xuất kết quả tra cứu đơn bán ra Excel (.xlsx, Apache POI) hoặc CSV — dùng
 * lại đúng bộ lọc của SaleOrderSearchProcess (qua SaleOrderQueryHelper)
 * nhưng KHÔNG phân trang, lấy toàn bộ kết quả khớp filter. Theo đúng mẫu
 * ProductExportProcess, bao gồm cả phòng thủ CSV Injection.
 */
public class SaleOrderExportProcess extends AbstractProcess {

	/** Ký tự Excel/LibreOffice tự diễn giải thành công thức nếu đứng đầu ô. */
	private static final String FORMULA_TRIGGER_CHARS = "=+-@";

	private static final String[] HEADERS = {
			"Số đơn bán", "Ngày giờ bán", "Khách hàng", "Thu ngân", "Phương thức thanh toán",
			"Số dòng hàng", "Tổng tiền hàng", "Khách trả", "Tiền thối", "Trạng thái"
	};

	public SaleOrderExportProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderExportResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderExportRequest req = (SaleOrderExportRequest) request;
		SaleOrderExportResponse res = (SaleOrderExportResponse) response;

		if (!"XLSX".equals(req.format) && !"CSV".equals(req.format)) {
			SaleOrderQueryHelper.throwError("ME000063");
		}

		List<SaleOrderRowDto> rows = queryAllRows(dba, req);

		String timestamp = CommonUtility.compactTimestamp();
		if ("CSV".equals(req.format)) {
			res.fileBytes = buildCsv(rows);
			res.fileName = "tra_cuu_ban_hang_" + timestamp + ".csv";
			res.contentType = "text/csv; charset=UTF-8";
		} else {
			res.fileBytes = buildXlsx(rows);
			res.fileName = "tra_cuu_ban_hang_" + timestamp + ".xlsx";
			res.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		}

		return res;
	}

	private List<SaleOrderRowDto> queryAllRows(DBAccessor dba, SaleOrderExportRequest req)
			throws DBException, ProcessCheckErrorException {

		String branchCode = SaleOrderQueryHelper.resolveBranchCode(dba, req.accessInfo.userCode);

		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		SaleOrderQueryHelper.buildWhereClause(branchCode, req.keyword, req.dateFrom, req.dateTo, req.paymentMethod,
				req.statusFilter, req.cashierKeyword, where, params);

		String sortColumn = SaleOrderQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = SaleOrderQueryHelper.resolveSortDirection(req.sortDirection);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(SaleOrderQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(SaleOrderQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection);

			ps = dba.prepareStatement(sql);
			SaleOrderQueryHelper.bindParams(ps, params);

			rs = ps.executeQuery();

			List<SaleOrderRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(SaleOrderQueryHelper.mapRow(rs));
			}
			return rows;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			SaleOrderQueryHelper.closeQuietly(rs, ps);
		}
	}

	private String paymentMethodLabel(String value) {
		if (PaymentMethod.CASH.equals(value)) return "Tiền mặt";
		if (PaymentMethod.TRANSFER.equals(value)) return "Chuyển khoản";
		return value != null ? value : "";
	}

	/**
	 * CSV UTF-8 kèm BOM (﻿) ở đầu — thiếu BOM, Excel (khác LibreOffice/trình
	 * duyệt) sẽ đoán sai encoding và hiển thị tiếng Việt có dấu bị lỗi dù nội
	 * dung file thực chất đã đúng UTF-8. Theo đúng mẫu ProductExportProcess.
	 */
	private byte[] buildCsv(List<SaleOrderRowDto> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append('﻿');
		sb.append(String.join(",", escapeCsvRow(HEADERS))).append("\r\n");

		for (SaleOrderRowDto row : rows) {
			String[] values = {
					row.saleOrderNo, row.saleDatetime, row.customerName, row.cashierName,
					paymentMethodLabel(row.paymentMethod),
					String.valueOf(row.itemCount),
					row.totalAmount != null ? row.totalAmount.toPlainString() : "",
					row.paidAmount != null ? row.paidAmount.toPlainString() : "",
					row.changeAmount != null ? row.changeAmount.toPlainString() : "",
					"1".equals(row.voidFlg) ? "Đã huỷ" : "Còn hiệu lực"
			};
			sb.append(String.join(",", escapeCsvRow(values))).append("\r\n");
		}

		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	private String[] escapeCsvRow(String[] values) {
		String[] escaped = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			escaped[i] = escapeCsvField(values[i]);
		}
		return escaped;
	}

	private String escapeCsvField(String value) {
		if (value == null) {
			return "";
		}
		String sanitized = neutralizeFormulaTrigger(value);
		if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")
				|| sanitized.contains("\r")) {
			return "\"" + sanitized.replace("\"", "\"\"") + "\"";
		}
		return sanitized;
	}

	/**
	 * Chống CSV Injection (Formula Injection, CWE-1236) — customer_name do
	 * khách/thu ngân nhập tự do lúc bán (sale_order.customer_name), không thể
	 * coi là input tin cậy khi ghi thẳng vào file xuất ra ngoài hệ thống. Cùng
	 * biện pháp OWASP CSV Injection Cheat Sheet đã áp dụng ở ProductExportProcess.
	 */
	private String neutralizeFormulaTrigger(String value) {
		if (!value.isEmpty() && FORMULA_TRIGGER_CHARS.indexOf(value.charAt(0)) >= 0) {
			return "'" + value;
		}
		return value;
	}

	/** KHÔNG dùng Sheet.autoSizeColumn() — cần font-metrics thật (AWT), rủi ro trên môi trường server headless. */
	private byte[] buildXlsx(List<SaleOrderRowDto> rows) throws FatalException {
		try (Workbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Tra cứu bán hàng");

			Font headerFont = wb.createFont();
			headerFont.setBold(true);
			CellStyle headerStyle = wb.createCellStyle();
			headerStyle.setFont(headerFont);

			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < HEADERS.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(HEADERS[i]);
				cell.setCellStyle(headerStyle);
				sheet.setColumnWidth(i, 20 * 256);
			}

			int rowIndex = 1;
			for (SaleOrderRowDto row : rows) {
				Row dataRow = sheet.createRow(rowIndex++);
				setCell(dataRow, 0, row.saleOrderNo);
				setCell(dataRow, 1, row.saleDatetime);
				setCell(dataRow, 2, row.customerName);
				setCell(dataRow, 3, row.cashierName);
				setCell(dataRow, 4, paymentMethodLabel(row.paymentMethod));
				dataRow.createCell(5).setCellValue(row.itemCount);
				dataRow.createCell(6).setCellValue(row.totalAmount != null ? row.totalAmount.doubleValue() : 0d);
				dataRow.createCell(7).setCellValue(row.paidAmount != null ? row.paidAmount.doubleValue() : 0d);
				dataRow.createCell(8).setCellValue(row.changeAmount != null ? row.changeAmount.doubleValue() : 0d);
				setCell(dataRow, 9, "1".equals(row.voidFlg) ? "Đã huỷ" : "Còn hiệu lực");
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			return bos.toByteArray();

		} catch (IOException e) {
			throw new FatalException(e);
		}
	}

	/** Cùng phòng thủ CSV Injection ở trên áp dụng cho XLSX theo chiều sâu — xem ProductExportProcess.setCell. */
	private void setCell(Row row, int index, String value) {
		row.createCell(index).setCellValue(value != null ? neutralizeFormulaTrigger(value) : "");
	}
}
