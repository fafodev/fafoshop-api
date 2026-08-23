package fafoshop.pos.saleorder.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import fafoshop.pos.saleorder.dto.SaleOrderVoidRequest;
import fafoshop.pos.saleorder.dto.SaleOrderVoidResponse;
import fafoshop.pos.stock.process.StockLotHelper;

/**
 * Huỷ đơn bán — set `void_flg='1'` (THAY cho xoá cứng, cột đã có sẵn từ đầu
 * trong schema nhưng trước đây CHƯA có webservice nào ghi) + hoàn tác toàn
 * bộ tồn kho đã trừ lúc bán (cộng lại đúng số lượng gốc từng dòng). Xem
 * điều kiện được huỷ: {@link SaleOrderEditGuard}. Xem
 * docs/pos-sua-huy-don.md (gốc workspace).
 */
public class SaleOrderVoidProcess extends AbstractProcess {

	private static final String PRG_CD = "SALE_VOID";

	public SaleOrderVoidProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderVoidResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderVoidRequest req = (SaleOrderVoidRequest) request;
		SaleOrderVoidResponse res = (SaleOrderVoidResponse) response;

		validateSaleOrderNo(req.saleOrderNo);

		String branchCode = SaleOrderEditGuard.resolveEligibleBranchCode(dba, req.saleOrderNo, req.accessInfo.userCode);

		Map<String, Integer> delta = new HashMap<>();
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT stock_code, product_code, expiry_date, quantity FROM sale_order_item "
					+ "WHERE sale_order_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, req.saleOrderNo);
			rs = ps.executeQuery();
			while (rs.next()) {
				String stockCode = rs.getString("stock_code");
				if (stockCode == null || stockCode.trim().isEmpty()) {
					stockCode = StockLotHelper.findOrCreate(dba, branchCode, rs.getString("product_code"),
							rs.getDate("expiry_date"), req.accessInfo.userCode, PRG_CD);
				}
				delta.merge(stockCode, -rs.getInt("quantity"), Integer::sum);
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
		SaleOrderStockAdjuster.applyDelta(dba, delta, req.accessInfo.userCode, PRG_CD);

		markVoid(dba, req.saleOrderNo, req.accessInfo.userCode);

		res.saleOrderNo = req.saleOrderNo;
		return res;
	}

	private void validateSaleOrderNo(String saleOrderNo) throws ProcessCheckErrorException {
		if (saleOrderNo == null || saleOrderNo.trim().isEmpty()) {
			throwError("ME000120");
		}
	}

	private void markVoid(DBAccessor dba, String saleOrderNo, String userCode) throws DBException {
		DBStatement ps = null;
		try {
			String sql = "UPDATE sale_order SET void_flg = '1', update_user_code = ?, update_program = ? "
					+ "WHERE sale_order_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			ps.setString(2, PRG_CD);
			ps.setString(3, saleOrderNo);
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
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

	private void closeQuietly(ResultSet rs, DBStatement ps) throws DBException {
		try {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
