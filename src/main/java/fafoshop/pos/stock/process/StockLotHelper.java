package fafoshop.pos.stock.process;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ConstantValue;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.ErrorDto;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.utility.MessageUtility;
import fafoshop.common.utility.SeqNoUtility;
import fafoshop.pos.stock.dto.StockLotRowDto;

/**
 * Thao tác lô tồn kho ({@code stock.stock_code}, prefix IS). Dùng chung
 * nhập/bán/kiểm kê — xem docs/pos-lo-ton-kho.md.
 */
public final class StockLotHelper {

	public static final String SEQ_PREFIX = "IS";

	public static final String DEFAULT_QUALITY_CODE = "01";

	private StockLotHelper() {
	}

	/**
	 * Lô còn hàng (stock_qty &gt; 0) của 1 sản phẩm tại chi nhánh — POS dùng
	 * để hỏi chọn hạn dùng khi có từ 2 lô trở lên.
	 */
	public static List<StockLotRowDto> listSellableLots(DBAccessor dba, String branchCode, String productCode)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT stock_code, expiry_date, stock_qty FROM stock "
					+ "WHERE branch_code = ? AND product_code = ? AND stock_qty > 0 "
					+ "ORDER BY expiry_date IS NULL ASC, expiry_date ASC, stock_code ASC";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setString(2, productCode);
			rs = ps.executeQuery();
			List<StockLotRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				StockLotRowDto row = new StockLotRowDto();
				row.stockCode = rs.getString("stock_code");
				Date expiry = rs.getDate("expiry_date");
				row.expiryDate = expiry == null ? null : expiry.toString();
				row.stockQty = rs.getInt("stock_qty");
				rows.add(row);
			}
			return rows;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	public static String findStockCode(DBAccessor dba, String branchCode, String productCode, Date expiryDate)
			throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT stock_code FROM stock WHERE branch_code = ? AND product_code = ? "
					+ "AND expiry_date <=> ? ORDER BY stock_code ASC LIMIT 1";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setString(2, productCode);
			ps.setDate(3, expiryDate);
			rs = ps.executeQuery();
			return rs.next() ? rs.getString("stock_code") : null;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	public static String findOrCreate(DBAccessor dba, String branchCode, String productCode, Date expiryDate,
			String userCode, String programCode) throws DBException, FatalException {
		String existing = findStockCode(dba, branchCode, productCode, expiryDate);
		if (existing != null) {
			return existing;
		}
		return insertLot(dba, branchCode, productCode, expiryDate, 0, userCode, programCode);
	}

	public static String insertLot(DBAccessor dba, String branchCode, String productCode, Date expiryDate, int qty,
			String userCode, String programCode) throws DBException, FatalException {
		String stockCode = SeqNoUtility.generate(dba, SEQ_PREFIX, userCode, programCode);
		DBStatement ps = null;
		try {
			String sql = "INSERT INTO stock "
					+ "(stock_code, branch_code, product_code, quality_code, expiry_date, stock_qty, available_qty, "
					+ " entry_user_code, entry_program, update_user_code, update_program) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = dba.prepareStatement(sql);
			ps.setString(1, stockCode);
			ps.setString(2, branchCode);
			ps.setString(3, productCode);
			ps.setString(4, DEFAULT_QUALITY_CODE);
			ps.setDate(5, expiryDate);
			ps.setInt(6, qty);
			ps.setInt(7, qty);
			ps.setString(8, userCode);
			ps.setString(9, programCode);
			ps.setString(10, userCode);
			ps.setString(11, programCode);
			ps.executeUpdate();
			return stockCode;
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	public static void addQty(DBAccessor dba, String stockCode, int qty, String userCode, String programCode)
			throws DBException {
		adjustQty(dba, stockCode, qty, false, userCode, programCode);
	}

	public static void subtractQtyFloor(DBAccessor dba, String stockCode, int qty, String userCode, String programCode)
			throws DBException {
		adjustQty(dba, stockCode, qty, true, userCode, programCode);
	}

	/**
	 * Cộng (delta &gt; 0) hoặc trừ floor-0 (delta &lt; 0) đúng lô tìm theo
	 * sản phẩm + hạn dùng. Không có lô thì tạo mới rồi áp delta.
	 */
	public static void applyDeltaByExpiry(DBAccessor dba, String branchCode, String productCode, Date expiryDate,
			int delta, String userCode, String programCode) throws DBException, FatalException {
		if (delta == 0) {
			return;
		}
		String stockCode = findOrCreate(dba, branchCode, productCode, expiryDate, userCode, programCode);
		if (delta > 0) {
			addQty(dba, stockCode, delta, userCode, programCode);
		} else {
			subtractQtyFloor(dba, stockCode, -delta, userCode, programCode);
		}
	}

	/**
	 * Trừ tồn lúc bán. stockCode bắt buộc khi sản phẩm có từ 2 lô còn hàng.
	 * 0 lô: tạo lô không hạn dùng rồi floor 0 (giữ hành vi bán khi chưa có
	 * tồn). 1 lô: dùng lô đó nếu client không gửi mã.
	 */
	public static void decrementForSale(DBAccessor dba, String branchCode, String productCode, String stockCode,
			int qty, String userCode, String programCode) throws DBException, FatalException, ProcessCheckErrorException {
		String resolved = resolveStockCodeForSale(dba, branchCode, productCode, stockCode, userCode, programCode);
		subtractQtyFloor(dba, resolved, qty, userCode, programCode);
	}

	public static String resolveStockCodeForSale(DBAccessor dba, String branchCode, String productCode,
			String stockCode, String userCode, String programCode)
			throws DBException, FatalException, ProcessCheckErrorException {
		if (stockCode != null && !stockCode.trim().isEmpty()) {
			assertStockBelongsToProduct(dba, branchCode, productCode, stockCode.trim());
			return stockCode.trim();
		}
		List<StockLotRowDto> lots = listSellableLots(dba, branchCode, productCode);
		if (lots.size() >= 2) {
			throwError("ME000138");
		}
		if (lots.size() == 1) {
			return lots.get(0).stockCode;
		}
		return findOrCreate(dba, branchCode, productCode, null, userCode, programCode);
	}

	public static Date readExpiry(DBAccessor dba, String stockCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT expiry_date FROM stock WHERE stock_code = ?");
			ps.setString(1, stockCode);
			rs = ps.executeQuery();
			if (!rs.next()) {
				return null;
			}
			return rs.getDate("expiry_date");
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private static void assertStockBelongsToProduct(DBAccessor dba, String branchCode, String productCode,
			String stockCode) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT product_code FROM stock WHERE stock_code = ? AND branch_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, stockCode);
			ps.setString(2, branchCode);
			rs = ps.executeQuery();
			if (!rs.next() || !productCode.equals(rs.getString("product_code"))) {
				throwError("ME000139");
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private static void adjustQty(DBAccessor dba, String stockCode, int qty, boolean subtractFloor, String userCode,
			String programCode) throws DBException {
		DBStatement ps = null;
		try {
			String sql = subtractFloor
					? "UPDATE stock SET stock_qty = GREATEST(stock_qty - ?, 0), "
							+ "available_qty = GREATEST(available_qty - ?, 0), "
							+ "update_user_code = ?, update_program = ? WHERE stock_code = ?"
					: "UPDATE stock SET stock_qty = stock_qty + ?, available_qty = available_qty + ?, "
							+ "update_user_code = ?, update_program = ? WHERE stock_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setInt(1, qty);
			ps.setInt(2, qty);
			ps.setString(3, userCode);
			ps.setString(4, programCode);
			ps.setString(5, stockCode);
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	private static void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}

	private static void closeQuietly(ResultSet rs, DBStatement ps) throws DBException {
		try {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
