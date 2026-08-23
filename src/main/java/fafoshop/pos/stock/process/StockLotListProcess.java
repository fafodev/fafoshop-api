package fafoshop.pos.stock.process;

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
import fafoshop.pos.stock.dto.StockLotListRequest;
import fafoshop.pos.stock.dto.StockLotListResponse;

/**
 * Danh sách lô còn hàng của 1 sản phẩm tại chi nhánh phiên — POS hỏi chọn
 * hạn dùng khi có từ 2 lô. Quyền SALE_CREAT (cùng lúc bán hàng).
 */
public class StockLotListProcess extends AbstractProcess {

	public StockLotListProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new StockLotListResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_CREAT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		StockLotListRequest req = (StockLotListRequest) request;
		StockLotListResponse res = (StockLotListResponse) response;

		if (req.productCode == null || req.productCode.trim().isEmpty()) {
			throwError("ME000061");
		}

		String branchCode = getUserBranchCode(dba, req.accessInfo.userCode);
		res.rows = StockLotHelper.listSellableLots(dba, branchCode, req.productCode.trim());
		return res;
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

	private void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
	}
}
