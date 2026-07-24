package fafoshop.pos.supplier.process;

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
import fafoshop.pos.supplier.dto.SupplierRestoreRequest;
import fafoshop.pos.supplier.dto.SupplierRestoreResponse;

/** Khôi phục nhà cung cấp đã xoá mềm (del_flg='1' -> '0') — ngược lại SupplierDeleteProcess. */
public class SupplierRestoreProcess extends AbstractProcess {

	private static final String PRG_CD = "SPLR_RST";

	public SupplierRestoreProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SupplierRestoreResponse();
	}

	@Override
	protected String getFuncId() {
		return "SPLR_DEL";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SupplierRestoreRequest req = (SupplierRestoreRequest) request;
		SupplierRestoreResponse res = (SupplierRestoreResponse) response;

		DBStatement ps = null;
		try {
			String sql = "UPDATE supplier SET del_flg = '0', update_user_code = ?, update_program = ? "
					+ "WHERE supplier_code = ? AND del_flg = '1'";

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.accessInfo.userCode);
			ps.setString(2, PRG_CD);
			ps.setString(3, req.supplierCode);

			int affected = ps.executeUpdate();
			if (affected == 0) {
				List<ErrorDto> errors = new ArrayList<>();
				ErrorDto error = new ErrorDto();
				error.errId = "ME000074";
				error.errMsg = MessageUtility.getSystemErrMsg("ME000074");
				errors.add(error);
				throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
			}

			res.supplierCode = req.supplierCode;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}
}
