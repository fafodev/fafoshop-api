package fafoshop.pos.supplier.process;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.utility.SeqNoUtility;
import fafoshop.pos.supplier.dto.SupplierCreateRequest;
import fafoshop.pos.supplier.dto.SupplierCreateResponse;

/**
 * Tạo nhà cung cấp mới trên bảng supplier. supplier_code SINH TỰ ĐỘNG qua
 * SeqNoUtility (prefix "NCC", xem .claude/seqno-convention.md) — dạng
 * "NCC"+yyyyMMdd+4 số, theo đúng chuẩn chung sinh mã quản lý toàn hệ thống.
 */
public class SupplierCreateProcess extends AbstractProcess {

	/** Mã chương trình ghi vào entry_program/update_program — cột chỉ rộng VARCHAR(10). */
	private static final String PRG_CD = "SPLR_CRT";

	/** Prefix đăng ký sẵn trong bảng seq_no cho supplier_code. */
	private static final String SEQ_PREFIX = "NCC";

	public SupplierCreateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SupplierCreateResponse();
	}

	@Override
	protected String getFuncId() {
		return "SPLR_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SupplierCreateRequest req = (SupplierCreateRequest) request;
		SupplierCreateResponse res = (SupplierCreateResponse) response;

		SupplierFieldValidator.validate(req.name, req.shortName);

		DBStatement ps = null;

		String supplierCode = SeqNoUtility.generate(dba, SEQ_PREFIX, req.accessInfo.userCode, PRG_CD);

		try {
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO supplier ");
			sql.append("(supplier_code, name, short_name, zip_code, address1, address2, address3, tel, fax, ");
			sql.append(" contact_name, email, note, ");
			sql.append(" entry_user_code, entry_program, update_user_code, update_program) ");
			sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			ps = dba.prepareStatement(sql);
			ps.setString(1, supplierCode);
			ps.setString(2, req.name);
			ps.setString(3, req.shortName);
			ps.setString(4, req.zipCode);
			ps.setString(5, req.address1);
			ps.setString(6, req.address2);
			ps.setString(7, req.address3);
			ps.setString(8, req.tel);
			ps.setString(9, req.fax);
			ps.setString(10, req.contactName);
			ps.setString(11, req.email);
			ps.setString(12, req.note);
			ps.setString(13, req.accessInfo.userCode);
			ps.setString(14, PRG_CD);
			ps.setString(15, req.accessInfo.userCode);
			ps.setString(16, PRG_CD);
			ps.executeUpdate();

			res.supplierCode = supplierCode;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}
}
