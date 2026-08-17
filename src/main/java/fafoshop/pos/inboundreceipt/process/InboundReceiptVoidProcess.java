package fafoshop.pos.inboundreceipt.process;

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
import fafoshop.pos.inboundreceipt.dto.InboundReceiptVoidRequest;
import fafoshop.pos.inboundreceipt.dto.InboundReceiptVoidResponse;

/**
 * Huỷ phiếu nhập — set `void_flg='1'` + hoàn tác tồn kho đã cộng lúc nhập
 * (trừ lại, floor 0). Mirror SaleOrderVoidProcess. Xem
 * docs/pos-sua-huy-don.md (gốc workspace).
 */
public class InboundReceiptVoidProcess extends AbstractProcess {

	private static final String PRG_CD = "INBND_VOID";

	public InboundReceiptVoidProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new InboundReceiptVoidResponse();
	}

	@Override
	protected String getFuncId() {
		return "INBND_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		InboundReceiptVoidRequest req = (InboundReceiptVoidRequest) request;
		InboundReceiptVoidResponse res = (InboundReceiptVoidResponse) response;

		validateReceiptNo(req.receiptNo);

		String branchCode = InboundReceiptEditGuard.resolveEligibleBranchCode(dba, req.receiptNo, req.accessInfo.userCode);

		Map<String, Integer> quantities = queryCurrentQuantities(dba, branchCode, req.receiptNo);
		// Hoàn tác toàn bộ = delta ÂM đúng bằng số lượng gốc (trừ lại hết, floor 0).
		Map<String, Integer> delta = new HashMap<>();
		for (Map.Entry<String, Integer> e : quantities.entrySet()) {
			delta.put(e.getKey(), -e.getValue());
		}
		InboundReceiptStockAdjuster.applyDelta(dba, branchCode, delta, req.accessInfo.userCode, PRG_CD);

		markVoid(dba, branchCode, req.receiptNo, req.accessInfo.userCode);

		res.receiptNo = req.receiptNo;
		return res;
	}

	private void validateReceiptNo(String receiptNo) throws ProcessCheckErrorException {
		if (receiptNo == null || receiptNo.trim().isEmpty()) {
			throwError("ME000125");
		}
	}

	private Map<String, Integer> queryCurrentQuantities(DBAccessor dba, String branchCode, String receiptNo)
			throws DBException {
		Map<String, Integer> result = new HashMap<>();
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT product_code, actual_qty FROM inbound_receipt_item "
					+ "WHERE branch_code = ? AND receipt_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			ps.setString(2, receiptNo);
			rs = ps.executeQuery();
			while (rs.next()) {
				result.merge(rs.getString("product_code"), rs.getInt("actual_qty"), Integer::sum);
			}
			return result;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void markVoid(DBAccessor dba, String branchCode, String receiptNo, String userCode) throws DBException {
		DBStatement ps = null;
		try {
			String sql = "UPDATE inbound_receipt SET void_flg = '1', update_user_code = ?, update_program = ? "
					+ "WHERE branch_code = ? AND receipt_no = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			ps.setString(2, PRG_CD);
			ps.setString(3, branchCode);
			ps.setString(4, receiptNo);
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
