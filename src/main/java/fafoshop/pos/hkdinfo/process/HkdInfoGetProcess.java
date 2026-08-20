package fafoshop.pos.hkdinfo.process;

import java.sql.ResultSet;
import java.sql.SQLException;

import fafoshop.common.ILogSender;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.pos.hkdinfo.dto.HkdInfoGetRequest;
import fafoshop.pos.hkdinfo.dto.HkdInfoGetResponse;

/**
 * Lấy thông tin hộ kinh doanh (tên/địa chỉ/mã số thuế) của chi nhánh người
 * đang đăng nhập — dùng in ở đầu Sổ S1a-HKD khi xuất báo cáo doanh thu.
 * Cùng pattern với BankAccountGetProcess: đọc thẳng bảng, ghi rõ
 * `configured=false` nếu chi nhánh CHƯA được seed/cấu hình (không coi là
 * lỗi nghiệp vụ).
 *
 * Không khai function_code riêng ({@link #getFuncId()} trả về null) — đọc
 * thông tin này để hiển thị/xuất báo cáo là thao tác cơ bản, giống lý do
 * BankAccountGetProcess không yêu cầu quyền riêng.
 */
public class HkdInfoGetProcess extends AbstractProcess {

	public HkdInfoGetProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new HkdInfoGetResponse();
	}

	@Override
	protected String getFuncId() {
		return null;
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		HkdInfoGetRequest req = (HkdInfoGetRequest) request;
		HkdInfoGetResponse res = (HkdInfoGetResponse) response;

		String branchCode = getUserBranchCode(dba, req.accessInfo.userCode);
		fillHkdInfo(dba, res, branchCode);
		return res;
	}

	/**
	 * Chi nhánh của người đang đăng nhập = main_branch_code (giống
	 * BankAccountGetProcess.getCashierBranchCode) — accessInfo hiện chưa
	 * mang branchCode nên phải tra lại theo user_code mỗi lần.
	 */
	private String getUserBranchCode(DBAccessor dba, String userCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT main_branch_code FROM app_user WHERE user_code = ?";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			rs = ps.executeQuery();
			return rs.next() ? rs.getString("main_branch_code") : null;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void fillHkdInfo(DBAccessor dba, HkdInfoGetResponse res, String branchCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT hkd_name, address, tax_code, phone, email FROM hkd_info "
					+ "WHERE branch_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			rs = ps.executeQuery();

			if (rs.next()) {
				res.configured = true;
				res.hkdName = rs.getString("hkd_name");
				res.address = rs.getString("address");
				res.taxCode = rs.getString("tax_code");
				res.phone = rs.getString("phone");
				res.email = rs.getString("email");
			} else {
				res.configured = false;
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
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
