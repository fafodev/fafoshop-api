package fafoshop.pos.product.process;

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
import fafoshop.pos.product.dto.ProductExportRequest;
import fafoshop.pos.product.dto.ProductExportResponse;
import fafoshop.pos.product.dto.ProductRowDto;

/**
 * Xuất danh sách sản phẩm ra Excel (.xlsx, Apache POI) hoặc CSV — dùng lại
 * đúng bộ lọc của ProductSearchProcess (qua ProductQueryHelper) nhưng KHÔNG
 * phân trang, lấy toàn bộ kết quả khớp filter.
 */
public class ProductExportProcess extends AbstractProcess {

	/** Ký tự Excel/LibreOffice tự diễn giải thành công thức nếu đứng đầu ô. */
	private static final String FORMULA_TRIGGER_CHARS = "=+-@";

	private static final String[] HEADERS = {
			"Mã sản phẩm", "Tên sản phẩm", "Tên rút gọn", "Mã vạch", "Mã danh mục", "Tên danh mục",
			"Mã NCC", "Tên NCC", "Đơn vị tính", "Thuế ưu đãi", "Giá bán", "Định mức tồn tối thiểu",
			"Trạng thái", "Cập nhật gần nhất"
	};

	public ProductExportProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new ProductExportResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		ProductExportRequest req = (ProductExportRequest) request;
		ProductExportResponse res = (ProductExportResponse) response;

		if (!"XLSX".equals(req.format) && !"CSV".equals(req.format)) {
			List<ErrorDto> errors = new ArrayList<>();
			ErrorDto error = new ErrorDto();
			error.errId = "ME000063";
			error.errMsg = MessageUtility.getSystemErrMsg("ME000063");
			errors.add(error);
			throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
		}

		List<ProductRowDto> rows = queryAllRows(dba, req);

		String timestamp = CommonUtility.compactTimestamp();
		if ("CSV".equals(req.format)) {
			res.fileBytes = buildCsv(rows);
			res.fileName = "san_pham_" + timestamp + ".csv";
			res.contentType = "text/csv; charset=UTF-8";
		} else {
			res.fileBytes = buildXlsx(rows);
			res.fileName = "san_pham_" + timestamp + ".xlsx";
			res.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		}

		return res;
	}

	private List<ProductRowDto> queryAllRows(DBAccessor dba, ProductExportRequest req) throws DBException {
		StringBuilder where = new StringBuilder();
		List<String> params = new ArrayList<>();
		ProductQueryHelper.buildWhereClause(req.keyword, req.categoryCode, req.statusFilter, where, params);

		String sortColumn = ProductQueryHelper.resolveSortColumn(req.sortField);
		String sortDirection = ProductQueryHelper.resolveSortDirection(req.sortDirection);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT ").append(ProductQueryHelper.SELECT_COLUMNS_SQL);
			sql.append(ProductQueryHelper.FROM_JOIN_SQL);
			sql.append(where);
			sql.append("ORDER BY ").append(sortColumn).append(" ").append(sortDirection);

			ps = dba.prepareStatement(sql);
			int idx = 1;
			for (String param : params) {
				ps.setString(idx++, param);
			}

			rs = ps.executeQuery();

			List<ProductRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				rows.add(ProductQueryHelper.mapRow(rs));
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
	private byte[] buildCsv(List<ProductRowDto> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append('﻿');
		sb.append(String.join(",", escapeCsvRow(HEADERS))).append("\r\n");

		for (ProductRowDto row : rows) {
			String[] values = {
					row.productCode, row.name, row.shortName, row.barcode, row.categoryCode, row.categoryName,
					row.supplierCode, row.supplierName, row.unitName,
					"1".equals(row.reducedTaxRateFlg) ? "Có" : "Không",
					row.price != null ? row.price.toPlainString() : "",
					row.minStockQty != null ? row.minStockQty.toString() : "0",
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
	 * liệu tên sản phẩm/tên rút gọn... do người dùng có quyền PRDCT_EDIT nhập
	 * tự do, không thể coi là input tin cậy khi ghi thẳng vào file xuất ra
	 * ngoài hệ thống (phát hiện qua test thủ công: tên "=CMD(calc)" bị ghi
	 * nguyên văn vào CSV).
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
	private byte[] buildXlsx(List<ProductRowDto> rows) throws FatalException {
		try (Workbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Sản phẩm");

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
			for (ProductRowDto row : rows) {
				Row dataRow = sheet.createRow(rowIndex++);
				setCell(dataRow, 0, row.productCode);
				setCell(dataRow, 1, row.name);
				setCell(dataRow, 2, row.shortName);
				setCell(dataRow, 3, row.barcode);
				setCell(dataRow, 4, row.categoryCode);
				setCell(dataRow, 5, row.categoryName);
				setCell(dataRow, 6, row.supplierCode);
				setCell(dataRow, 7, row.supplierName);
				setCell(dataRow, 8, row.unitName);
				setCell(dataRow, 9, "1".equals(row.reducedTaxRateFlg) ? "Có" : "Không");
				dataRow.createCell(10).setCellValue(row.price != null ? row.price.doubleValue() : 0d);
				dataRow.createCell(11).setCellValue(row.minStockQty != null ? row.minStockQty : 0);
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
	 * Cùng phòng thủ CSV Injection ở trên áp dụng cho XLSX theo chiều sâu:
	 * POI ghi cell dạng chuỗi tường minh (kiểu STRING trong OOXML) nên rủi ro
	 * thấp hơn CSV thuần văn bản, nhưng vẫn trung hoà để nhất quán 2 định
	 * dạng xuất, phòng trường hợp phần mềm bảng tính nào đó vẫn tự suy đoán
	 * công thức từ nội dung ô.
	 */
	private void setCell(Row row, int index, String value) {
		row.createCell(index).setCellValue(value != null ? neutralizeFormulaTrigger(value) : "");
	}
}
