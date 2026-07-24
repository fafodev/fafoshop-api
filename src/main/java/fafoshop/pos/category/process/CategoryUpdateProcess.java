package fafoshop.pos.category.process;

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
import fafoshop.pos.category.dto.CategoryUpdateRequest;
import fafoshop.pos.category.dto.CategoryUpdateResponse;

/**
 * Sửa thông tin danh mục đã có (không sửa được danh mục đã xoá mềm — phải
 * khôi phục trước, xem CategoryRestoreProcess). category_code là khoá
 * chính, KHÔNG cho sửa (chỉ dùng để xác định dòng).
 */
public class CategoryUpdateProcess extends AbstractProcess {

	private static final String PRG_CD = "CTGR_UPD";
	private static final String DEFAULT_CATEGORY_TYPE = "PRODUCT";

	public CategoryUpdateProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new CategoryUpdateResponse();
	}

	@Override
	protected String getFuncId() {
		return "CTGR_EDIT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		CategoryUpdateRequest req = (CategoryUpdateRequest) request;
		CategoryUpdateResponse res = (CategoryUpdateResponse) response;

		CategoryFieldValidator.validateName(req.name);

		String categoryType = req.categoryType != null && !req.categoryType.trim().isEmpty()
				? req.categoryType.trim()
				: DEFAULT_CATEGORY_TYPE;
		int displayOrder = req.displayOrder != null ? req.displayOrder : 0;

		DBStatement ps = null;
		try {
			String sql = "UPDATE category SET name = ?, category_type = ?, display_order = ?, "
					+ "update_user_code = ?, update_program = ? WHERE category_code = ? AND del_flg = '0'";

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.name);
			ps.setString(2, categoryType);
			ps.setInt(3, displayOrder);
			ps.setString(4, req.accessInfo.userCode);
			ps.setString(5, PRG_CD);
			ps.setString(6, req.categoryCode);

			int affected = ps.executeUpdate();
			if (affected == 0) {
				List<ErrorDto> errors = new ArrayList<>();
				ErrorDto error = new ErrorDto();
				error.errId = "ME000080";
				error.errMsg = MessageUtility.getSystemErrMsg("ME000080");
				errors.add(error);
				throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
			}

			res.categoryCode = req.categoryCode;
			return res;

		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}
}
