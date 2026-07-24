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
import fafoshop.pos.supplier.dto.SupplierUpdateRequest;
import fafoshop.pos.supplier.dto.SupplierUpdateResponse;

/**
 * Sửa thông tin nhà cung cấp đã có (không sửa được NCC đã xoá mềm — phải
 * khôi phục trước, xem SupplierRestoreProcess).
 */
public class SupplierUpdateProcess extends AbstractProcess {

	/** Mã chương trình ghi vào update_program — cột chỉ rộng VARCHAR(10). */
	private static final String PRG_CD = "SPLR_UPD";

	public SupplierUpdateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SupplierUpdateResponse();
	}

	@Override
	protected String getFuncId() {
		return "SPLR_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SupplierUpdateRequest req = (SupplierUpdateRequest) request;
		SupplierUpdateResponse res = (SupplierUpdateResponse) response;

		SupplierFieldValidator.validate(req.name, req.shortName);

		DBStatement ps = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("UPDATE supplier SET ");
			sql.append("name = ?, short_name = ?, zip_code = ?, address1 = ?, address2 = ?, address3 = ?, ");
			sql.append("tel = ?, fax = ?, contact_name = ?, email = ?, note = ?, ");
			sql.append("update_user_code = ?, update_program = ? ");
			sql.append("WHERE supplier_code = ? AND del_flg = '0'");

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.name);
			ps.setString(2, req.shortName);
			ps.setString(3, req.zipCode);
			ps.setString(4, req.address1);
			ps.setString(5, req.address2);
			ps.setString(6, req.address3);
			ps.setString(7, req.tel);
			ps.setString(8, req.fax);
			ps.setString(9, req.contactName);
			ps.setString(10, req.email);
			ps.setString(11, req.note);
			ps.setString(12, req.accessInfo.userCode);
			ps.setString(13, PRG_CD);
			ps.setString(14, req.supplierCode);

			int affected = ps.executeUpdate();
			if (affected == 0) {
				List<ErrorDto> errors = new ArrayList<>();
				ErrorDto error = new ErrorDto();
				error.errId = "ME000073";
				error.errMsg = MessageUtility.getSystemErrMsg("ME000073");
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
