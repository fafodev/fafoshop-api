package fafoshop.pos.stocktake.process;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import fafoshop.pos.stock.process.StockLotHelper;
import fafoshop.pos.stocktake.dto.StocktakeSaveItemDto;
import fafoshop.pos.stocktake.dto.StocktakeSaveRequest;
import fafoshop.pos.stocktake.dto.StocktakeSaveResponse;

/**
 * Ghi đè tồn kho theo số đếm / hạn dùng kiểm kê. Không tạo phiếu lịch sử —
 * xem docs/pos-kiem-ke.md.
 */
public class StocktakeSaveProcess extends AbstractProcess {

	private static final String PRG_CD = "STK_SAVE";

	/** Trần cột stock.stock_qty INT(9). */
	private static final int MAX_COUNTED_QTY = 999999999;

	public StocktakeSaveProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new StocktakeSaveResponse();
	}

	@Override
	protected String getFuncId() {
		return "STK_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		StocktakeSaveRequest req = (StocktakeSaveRequest) request;
		StocktakeSaveResponse res = (StocktakeSaveResponse) response;

		validateItems(req.items);
		String branchCode = getUserBranchCode(dba, req.accessInfo.userCode);
		String userCode = req.accessInfo.userCode;

		int saved = 0;
		for (StocktakeSaveItemDto item : req.items) {
			validateProductExists(dba, item.productCode);
			Date expiryDate = Boolean.TRUE.equals(item.expiryChanged) ? parseExpiryDate(item.expiryDate) : null;
			saveLot(dba, branchCode, item, expiryDate, userCode);
			saved++;
		}

		res.savedCount = saved;
		return res;
	}

	private void validateItems(List<StocktakeSaveItemDto> items) throws ProcessCheckErrorException {
		if (items == null || items.isEmpty()) {
			throwError("ME000134");
		}
		Set<String> seenLots = new HashSet<>();
		for (StocktakeSaveItemDto item : items) {
			if (item.productCode == null || item.productCode.trim().isEmpty()) {
				throwError("ME000061");
			}
			item.productCode = item.productCode.trim();
			boolean counted = item.countedQty != null;
			boolean expiryChanged = Boolean.TRUE.equals(item.expiryChanged);
			if (!counted && !expiryChanged) {
				throwError("ME000136");
			}
			if (counted && (item.countedQty < 0 || item.countedQty > MAX_COUNTED_QTY)) {
				throwError("ME000135");
			}
			if (item.stockCode != null && !item.stockCode.trim().isEmpty()) {
				item.stockCode = item.stockCode.trim();
				if (!seenLots.add(item.stockCode)) {
					throwError("ME000140");
				}
			} else {
				String expiryKey = Boolean.TRUE.equals(item.expiryChanged) && item.expiryDate != null
						? item.expiryDate.trim()
						: "";
				if (!seenLots.add("NEW\t" + item.productCode + "\t" + expiryKey)) {
					throwError("ME000140");
				}
			}
		}
	}

	private Date parseExpiryDate(String expiryDate) throws ProcessCheckErrorException {
		if (expiryDate == null || expiryDate.trim().isEmpty()) {
			return null;
		}
		try {
			return Date.valueOf(expiryDate.trim());
		} catch (IllegalArgumentException e) {
			throwError("ME000093");
			return null;
		}
	}

	private void validateProductExists(DBAccessor dba, String productCode)
			throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT product_code FROM product WHERE product_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, productCode);
			rs = ps.executeQuery();
			if (!rs.next()) {
				throwError("ME000061");
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void saveLot(DBAccessor dba, String branchCode, StocktakeSaveItemDto item, Date expiryDate, String userCode)
			throws DBException, FatalException, ProcessCheckErrorException {
		if (item.stockCode == null || item.stockCode.isEmpty()) {
			int qty = item.countedQty != null ? item.countedQty.intValue() : 0;
			Date expiryToWrite = Boolean.TRUE.equals(item.expiryChanged) ? expiryDate : null;
			String existing = StockLotHelper.findStockCode(dba, branchCode, item.productCode, expiryToWrite);
			if (existing != null) {
				throwError("ME000141");
			}
			StockLotHelper.insertLot(dba, branchCode, item.productCode, expiryToWrite, qty, userCode, PRG_CD);
			return;
		}
		assertLotOfProduct(dba, branchCode, item.productCode, item.stockCode);
		updateLot(dba, item, expiryDate, userCode);
	}

	private void assertLotOfProduct(DBAccessor dba, String branchCode, String productCode, String stockCode)
			throws DBException, ProcessCheckErrorException {
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

	private void updateLot(DBAccessor dba, StocktakeSaveItemDto item, Date expiryDate, String userCode)
			throws DBException {
		boolean counted = item.countedQty != null;
		boolean expiryChanged = Boolean.TRUE.equals(item.expiryChanged);
		DBStatement ps = null;
		try {
			if (counted && expiryChanged) {
				ps = dba.prepareStatement("UPDATE stock SET stock_qty = ?, available_qty = ?, expiry_date = ?, "
						+ "update_user_code = ?, update_program = ? WHERE stock_code = ?");
				ps.setInt(1, item.countedQty.intValue());
				ps.setInt(2, item.countedQty.intValue());
				ps.setDate(3, expiryDate);
				ps.setString(4, userCode);
				ps.setString(5, PRG_CD);
				ps.setString(6, item.stockCode);
			} else if (counted) {
				ps = dba.prepareStatement("UPDATE stock SET stock_qty = ?, available_qty = ?, "
						+ "update_user_code = ?, update_program = ? WHERE stock_code = ?");
				ps.setInt(1, item.countedQty.intValue());
				ps.setInt(2, item.countedQty.intValue());
				ps.setString(3, userCode);
				ps.setString(4, PRG_CD);
				ps.setString(5, item.stockCode);
			} else {
				ps = dba.prepareStatement(
						"UPDATE stock SET expiry_date = ?, update_user_code = ?, update_program = ? WHERE stock_code = ?");
				ps.setDate(1, expiryDate);
				ps.setString(2, userCode);
				ps.setString(3, PRG_CD);
				ps.setString(4, item.stockCode);
			}
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
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
