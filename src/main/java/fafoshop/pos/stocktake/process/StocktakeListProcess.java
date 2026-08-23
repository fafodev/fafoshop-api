package fafoshop.pos.stocktake.process;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import fafoshop.pos.stocktake.dto.StocktakeListRequest;
import fafoshop.pos.stocktake.dto.StocktakeListResponse;
import fafoshop.pos.stocktake.dto.StocktakeRowDto;

/**
 * Tải lưới kiểm kê: mọi sản phẩm còn hiệu lực, LEFT JOIN stock theo chi
 * nhánh phiên. Không trả số lượng tồn hệ thống — xem docs/pos-kiem-ke.md.
 */
public class StocktakeListProcess extends AbstractProcess {

	public StocktakeListProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new StocktakeListResponse();
	}

	@Override
	protected String getFuncId() {
		return "STK_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		StocktakeListRequest req = (StocktakeListRequest) request;
		StocktakeListResponse res = (StocktakeListResponse) response;

		String branchCode = getUserBranchCode(dba, req.accessInfo.userCode);
		res.rows = queryRows(dba, branchCode);
		return res;
	}

	private List<StocktakeRowDto> queryRows(DBAccessor dba, String branchCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT p.product_code, p.name, p.barcode, p.unit_name, s.stock_code, s.expiry_date "
					+ "FROM product p "
					+ "LEFT JOIN stock s ON s.product_code = p.product_code AND s.branch_code = ? "
					+ "WHERE p.del_flg = '0' "
					+ "ORDER BY p.name, p.product_code, s.expiry_date IS NULL ASC, s.expiry_date ASC, s.stock_code";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			rs = ps.executeQuery();

			List<StocktakeRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				StocktakeRowDto row = new StocktakeRowDto();
				row.productCode = rs.getString("product_code");
				row.name = rs.getString("name");
				row.barcode = rs.getString("barcode");
				row.unitName = rs.getString("unit_name");
				row.stockCode = rs.getString("stock_code");
				Date expiry = rs.getDate("expiry_date");
				row.expiryDate = expiry == null ? null : expiry.toString();
				rows.add(row);
			}
			return rows;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
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
