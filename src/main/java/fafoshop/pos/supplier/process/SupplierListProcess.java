package fafoshop.pos.supplier.process;

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
import fafoshop.pos.supplier.dto.SupplierListRequest;
import fafoshop.pos.supplier.dto.SupplierListResponse;
import fafoshop.pos.supplier.dto.SupplierListRowDto;

/**
 * Lấy danh sách rút gọn nhà cung cấp còn hiệu lực — bảng supplier KHÔNG dùng
 * chung nhiều nghiệp vụ như category nên không cần tham số lọc, chỉ phục vụ
 * riêng dropdown chọn nhà cung cấp ở Product Master (getFuncId()="PRDCT_VIEW",
 * cùng lý do với CategoryListProcess — chưa có màn quản trị nào khác dùng
 * tới). Tách biệt SupplierSearchProcess (đầy đủ field, có phân trang, phục
 * vụ Supplier Master).
 */
public class SupplierListProcess extends AbstractProcess {

	public SupplierListProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SupplierListResponse();
	}

	@Override
	protected String getFuncId() {
		return "PRDCT_VIEW";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SupplierListResponse res = (SupplierListResponse) response;

		ResultSet rs = null;
		DBStatement ps = null;

		try {
			String sql = "SELECT supplier_code, name FROM supplier WHERE del_flg = '0' ORDER BY name";

			ps = dba.prepareStatement(sql);
			rs = ps.executeQuery();

			List<SupplierListRowDto> rows = new ArrayList<>();
			while (rs.next()) {
				SupplierListRowDto row = new SupplierListRowDto();
				row.supplierCode = rs.getString("supplier_code");
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
