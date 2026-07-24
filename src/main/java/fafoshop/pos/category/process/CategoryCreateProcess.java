package fafoshop.pos.category.process;

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
import fafoshop.pos.category.dto.CategoryCreateRequest;
import fafoshop.pos.category.dto.CategoryCreateResponse;

/**
 * Tạo danh mục mới trên bảng category. category_code SINH TỰ ĐỘNG qua
 * SeqNoUtility (prefix "DM", xem .claude/seqno-convention.md) — KHÔNG còn
 * nhận từ client. category_code đã được nới rộng lên VARCHAR(20) (migration
 * migration_add_seqno_and_widen_category_code.sql) để đủ chỗ chứa mã tự
 * sinh dạng "DM"+yyyyMMdd+4 số (14 ký tự).
 */
public class CategoryCreateProcess extends AbstractProcess {

	/** Mã chương trình ghi vào entry_program/update_program — cột chỉ rộng VARCHAR(10). */
	private static final String PRG_CD = "CTGR_CRT";

	/** Prefix đăng ký sẵn trong bảng seq_no cho category_code. */
	private static final String SEQ_PREFIX = "DM";

	/** category_type mặc định khi request để trống — nghiệp vụ hiện tại chủ yếu dùng loại này. */
	private static final String DEFAULT_CATEGORY_TYPE = "PRODUCT";

	public CategoryCreateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new CategoryCreateResponse();
	}

	@Override
	protected String getFuncId() {
		return "CTGR_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		CategoryCreateRequest req = (CategoryCreateRequest) request;
		CategoryCreateResponse res = (CategoryCreateResponse) response;

		CategoryFieldValidator.validateName(req.name);

		String categoryType = req.categoryType != null && !req.categoryType.trim().isEmpty()
				? req.categoryType.trim()
				: DEFAULT_CATEGORY_TYPE;
		int displayOrder = req.displayOrder != null ? req.displayOrder : 0;

		String categoryCode = SeqNoUtility.generate(dba, SEQ_PREFIX, req.accessInfo.userCode, PRG_CD);

		DBStatement ps = null;

		try {
			StringBuilder sql = new StringBuilder();
			sql.append("INSERT INTO category ");
			sql.append("(category_code, category_type, name, display_order, ");
			sql.append(" entry_user_code, entry_program, update_user_code, update_program) ");
			sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

			ps = dba.prepareStatement(sql);
			ps.setString(1, categoryCode);
			ps.setString(2, categoryType);
			ps.setString(3, req.name);
			ps.setInt(4, displayOrder);
			ps.setString(5, req.accessInfo.userCode);
			ps.setString(6, PRG_CD);
			ps.setString(7, req.accessInfo.userCode);
			ps.setString(8, PRG_CD);
			ps.executeUpdate();

			res.categoryCode = categoryCode;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}
}
