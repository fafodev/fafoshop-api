package fafoshop.pos.category.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.pos.category.dto.CategoryTypeListResponse;

/**
 * Lấy danh sách category_type đang có dữ liệu (DISTINCT, chưa xoá mềm) —
 * phục vụ gợi ý autocomplete ở ô "Loại danh mục" trên form Category Master
 * (người dùng vẫn gõ tự do, đây chỉ là gợi ý từ dữ liệu đã tồn tại).
 */
public class CategoryTypeListProcess extends AbstractProcess {

	public CategoryTypeListProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new CategoryTypeListResponse();
	}

	@Override
	protected String getFuncId() {
		return "CTGR_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		CategoryTypeListResponse res = (CategoryTypeListResponse) response;

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT DISTINCT category_type FROM category WHERE del_flg = '0' ORDER BY category_type";
			ps = dba.prepareStatement(sql);
			rs = ps.executeQuery();

			List<String> types = new ArrayList<>();
			while (rs.next()) {
				types.add(rs.getString("category_type"));
			}
			res.types = types;

			return res;

		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			try {
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				throw new DBException(e);
			}
		}
	}
}
