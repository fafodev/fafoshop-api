package fafoshop.pos.supplier.process;

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
import fafoshop.common.utility.CommonUtility;
import fafoshop.common.utility.MessageUtility;
import fafoshop.pos.supplier.dto.SupplierExportRequest;
import fafoshop.pos.supplier.dto.SupplierExportResponse;
import fafoshop.pos.supplier.dto.SupplierRowDto;

/**
 * Xuất danh sách nhà cung cấp ra Excel (.xlsx, Apache POI) hoặc CSV — dùng
 * lại đúng bộ lọc của SupplierSearchProcess (qua SupplierQueryHelper) nhưng
 * KHÔNG phân trang, lấy toàn bộ kết quả khớp filter. Cùng khuôn
 * ProductExportProcess.
 */
public class SupplierExportProcess extends AbstractProcess {

	/** Ký tự Excel/LibreOffice tự diễn giải thành công thức nếu đứng đầu ô. */
	private static final String FORMULA_TRIGGER_CHARS = "=+-@";

	private static final String[] HEADERS = {
			"Mã NCC", "Tên NCC", "Tên rút gọn", "Mã bưu chính", "Địa chỉ 1", "Địa chỉ 2", "Địa chỉ 3",
			"Điện thoại", "Fax", "Người liên hệ", "Email", "Ghi chú", "Trạng thái", "Cập nhật gần nhất"
	};

	public SupplierExportProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SupplierExportResponse();
	}

	@Override
	protected String getFuncId() {
		return "SPLR_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SupplierExportRequest req = (SupplierExportRequest) request;
		SupplierExportResponse res = (SupplierExportResponse) response;

		if (!"XLSX".equals(req.format) && !"CSV".equals(req.format)) {
			List<ErrorDto> errors = new ArrayList<>();
			ErrorDto error = new ErrorDto();
			error.errId = "ME000063";
			error.errMsg = MessageUtility.getSystemErrMsg("ME000063");
			errors.add(error);
			throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
		}

		List<SupplierRowDto> rows = queryAllRows(dba, req);

		String timestamp = CommonUtility.compactTimestamp();
		if ("CSV".equals(req.format)) {
			res.fileBytes = buildCsv(rows);
			res.fileName = "nha_cung_cap_" + timestamp + ".csv";
			res.contentType = "text/csv; charset=UTF-8";
		} else {
			res.fileBytes = buildXlsx(rows);
			res.fileName = "nha_cung_cap_" + timestamp + ".xlsx";
			res.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		}

		return res;
	}

	private List<SupplierRowDto> queryAllRows(DBAccessor dba, SupplierExportRequest req) throws DBException {
		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		SupplierQueryHelper.buildWhereClause(req.keyword, req.statusFilter, where, params);

		String sortColumn = SupplierQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = SupplierQueryHelper.resolveSortDirection(req.sortDirection);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(SupplierQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(SupplierQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection);

			ps = dba.prepareStatement(sql);
			int idx = 1;
			for (String param : params) {
				ps.setString(idx++, param);
			}

			rs = ps.executeQuery();

			List<SupplierRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(SupplierQueryHelper.mapRow(rs));
			}
			return rows;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				throw new DBException(e);
			}
		}
	}

	/**
	 * CSV UTF-8 kèm BOM (﻿) ở đầu — thiếu BOM, Excel (khác LibreOffice/
	 * trình duyệt) sẽ đoán sai encoding và hiển thị tiếng Việt có dấu bị lỗi
	 * dù nội dung file thực chất đã đúng UTF-8.
	 */
	private byte[] buildCsv(List<SupplierRowDto> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append('﻿');
		sb.append(String.join(",", escapeCsvRow(HEADERS))).append("\r\n");

		for (SupplierRowDto row : rows) {
			String[] values = {
					row.supplierCode, row.name, row.shortName, row.zipCode, row.address1, row.address2,
					row.address3, row.tel, row.fax, row.contactName, row.email, row.note,
					"1".equals(row.delFlg) ? "Đã xoá" : "Còn hiệu lực",
					row.updateDatetime
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
	 * Chống CSV Injection (Formula Injection, CWE-1236): nếu ô bắt đầu bằng
	 * ký tự Excel/LibreOffice diễn giải thành công thức ('=', '+', '-', '@'),
	 * thêm tiền tố dấu nháy đơn để buộc ô đó được đọc như văn bản thuần thay
	 * vì công thức — theo khuyến nghị OWASP CSV Injection Cheat Sheet. Dữ
	 * liệu tên/địa chỉ/ghi chú... do người dùng có quyền SPLR_EDIT nhập tự
	 * do, không thể coi là input tin cậy khi ghi thẳng vào file xuất ra
	 * ngoài hệ thống (cùng lỗ hổng đã phát hiện ở Product Master export).
	 */
	private String neutralizeFormulaTrigger(String value) {
		if (!value.isEmpty() && FORMULA_TRIGGER_CHARS.indexOf(value.charAt(0)) >= 0) {
			return "'" + value;
		}
		return value;
	}

	/**
	 * KHÔNG dùng Sheet.autoSizeColumn() — cần font-metrics thật (AWT), rủi ro
	 * trên môi trường server headless. Dùng độ rộng cột cố định hợp lý thay
	 * thế (đơn vị POI: 1/256 ký tự).
	 */
	private byte[] buildXlsx(List<SupplierRowDto> rows) throws FatalException {
		try (Workbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Nhà cung cấp");

			Font headerFont = wb.createFont();
			headerFont.setBold(true);
			CellStyle headerStyle = wb.createCellStyle();
			headerStyle.setFont(headerFont);

			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < HEADERS.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(HEADERS[i]);
				cell.setCellStyle(headerStyle);
				sheet.setColumnWidth(i, 18 * 256);
			}

			int rowIndex = 1;
			for (SupplierRowDto row : rows) {
				Row dataRow = sheet.createRow(rowIndex++);
				setCell(dataRow, 0, row.supplierCode);
				setCell(dataRow, 1, row.name);
				setCell(dataRow, 2, row.shortName);
				setCell(dataRow, 3, row.zipCode);
				setCell(dataRow, 4, row.address1);
				setCell(dataRow, 5, row.address2);
				setCell(dataRow, 6, row.address3);
				setCell(dataRow, 7, row.tel);
				setCell(dataRow, 8, row.fax);
				setCell(dataRow, 9, row.contactName);
				setCell(dataRow, 10, row.email);
				setCell(dataRow, 11, row.note);
				setCell(dataRow, 12, "1".equals(row.delFlg) ? "Đã xoá" : "Còn hiệu lực");
				setCell(dataRow, 13, row.updateDatetime);
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			return bos.toByteArray();

		} catch (IOException e) {
			throw new FatalException(e);
		}
	}

	/**
	 * Cùng phòng thủ CSV Injection ở trên áp dụng cho XLSX theo chiều sâu —
	 * xem giải thích đầy đủ ở ProductExportProcess.setCell().
	 */
	private void setCell(Row row, int index, String value) {
		row.createCell(index).setCellValue(value != null ? neutralizeFormulaTrigger(value) : "");
	}
}
