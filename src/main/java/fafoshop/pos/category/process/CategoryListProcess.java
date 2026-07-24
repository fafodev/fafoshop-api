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
import fafoshop.pos.category.dto.CategoryListRequest;
import fafoshop.pos.category.dto.CategoryListResponse;
import fafoshop.pos.category.dto.CategoryRowDto;

/**
 * Lấy danh sách danh mục theo loại (category_type) — bảng category dùng
 * CHUNG nhiều nghiệp vụ, hiện chỉ phục vụ màn Product Master
 * (categoryType="PRODUCT"). Chỉ đọc, chưa có màn quản trị category riêng
 * (dữ liệu category test được thêm tay vào DB — xem retail-domain.md/kế
 * hoạch Product Master).
 */
public class CategoryListProcess extends AbstractProcess {

	public CategoryListProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new CategoryListResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		CategoryListRequest req = (CategoryListRequest) request;
		CategoryListResponse res = (CategoryListResponse) response;

		ResultSet rs = null;
		DBStatement ps = null;

		try {
			String sql = "SELECT category_code, name FROM category "
					+ "WHERE category_type = ? AND del_flg = '0' ORDER BY display_order, name";

			ps = dba.prepareStatement(sql);
			ps.setString(1, req.categoryType);
			rs = ps.executeQuery();

			List<CategoryRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				CategoryRowDto row = new CategoryRowDto();
				row.categoryCode = rs.getString("category_code");
				row.name = rs.getString("name");
				rows.add(row);
			}
			res.rows = rows;

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
