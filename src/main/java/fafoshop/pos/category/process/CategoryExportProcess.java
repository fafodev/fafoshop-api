package fafoshop.pos.category.process;

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
import fafoshop.pos.category.dto.CategoryExportRequest;
import fafoshop.pos.category.dto.CategoryExportResponse;
import fafoshop.pos.category.dto.CategoryFullRowDto;

/**
 * Xuất danh sách danh mục ra Excel (.xlsx, Apache POI) hoặc CSV — dùng lại
 * đúng bộ lọc của CategorySearchProcess (qua CategoryQueryHelper) nhưng
 * KHÔNG phân trang, lấy toàn bộ kết quả khớp filter. Cùng khuôn
 * ProductExportProcess.
 */
public class CategoryExportProcess extends AbstractProcess {

	/** Ký tự Excel/LibreOffice tự diễn giải thành công thức nếu đứng đầu ô. */
	private static final String FORMULA_TRIGGER_CHARS = "=+-@";

	private static final String[] HEADERS = {
			"Mã danh mục", "Tên danh mục", "Loại danh mục", "Thứ tự hiển thị", "Trạng thái", "Cập nhật gần nhất"
	};

	public CategoryExportProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new CategoryExportResponse();
	}

	@Override
	protected String getFuncId() {
		return "CTGR_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		CategoryExportRequest req = (CategoryExportRequest) request;
		CategoryExportResponse res = (CategoryExportResponse) response;

		if (!"XLSX".equals(req.format) && !"CSV".equals(req.format)) {
			List<ErrorDto> errors = new ArrayList<>();
			ErrorDto error = new ErrorDto();
			error.errId = "ME000063";
			error.errMsg = MessageUtility.getSystemErrMsg("ME000063");
			errors.add(error);
			throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
		}

		List<CategoryFullRowDto> rows = queryAllRows(dba, req);

		String timestamp = CommonUtility.compactTimestamp();
		if ("CSV".equals(req.format)) {
			res.fileBytes = buildCsv(rows);
			res.fileName = "danh_muc_" + timestamp + ".csv";
			res.contentType = "text/csv; charset=UTF-8";
		} else {
			res.fileBytes = buildXlsx(rows);
			res.fileName = "danh_muc_" + timestamp + ".xlsx";
			res.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		}

		return res;
	}

	private List<CategoryFullRowDto> queryAllRows(DBAccessor dba, CategoryExportRequest req) throws DBException {
		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		CategoryQueryHelper.buildWhereClause(req.keyword, req.categoryType, req.statusFilter, where, params);

		String sortColumn = CategoryQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = CategoryQueryHelper.resolveSortDirection(req.sortDirection);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(CategoryQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(CategoryQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection);

			ps = dba.prepareStatement(sql);
			int idx = 1;
			for (String param : params) {
				ps.setString(idx++, param);
			}

			rs = ps.executeQuery();

			List<CategoryFullRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(CategoryQueryHelper.mapRow(rs));
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
	private byte[] buildCsv(List<CategoryFullRowDto> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append('﻿');
		sb.append(String.join(",", escapeCsvRow(HEADERS))).append("\r\n");

		for (CategoryFullRowDto row : rows) {
			String[] values = {
					row.categoryCode, row.name, row.categoryType,
					row.displayOrder != null ? row.displayOrder.toString() : "0",
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
	 * Chống CSV Injection (Formula Injection, CWE-1236) — cùng cơ chế đã áp
	 * dụng cho Product/Supplier export, xem giải thích đầy đủ ở
	 * ProductExportProcess.
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
	private byte[] buildXlsx(List<CategoryFullRowDto> rows) throws FatalException {
		try (Workbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Danh mục");

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
			for (CategoryFullRowDto row : rows) {
				Row dataRow = sheet.createRow(rowIndex++);
				setCell(dataRow, 0, row.categoryCode);
				setCell(dataRow, 1, row.name);
				setCell(dataRow, 2, row.categoryType);
				dataRow.createCell(3).setCellValue(row.displayOrder != null ? row.displayOrder : 0);
				setCell(dataRow, 4, "1".equals(row.delFlg) ? "Đã xoá" : "Còn hiệu lực");
				setCell(dataRow, 5, row.updateDatetime);
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
