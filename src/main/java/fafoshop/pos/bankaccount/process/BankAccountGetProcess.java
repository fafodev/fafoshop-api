package fafoshop.pos.bankaccount.process;

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
import fafoshop.pos.bankaccount.dto.BankAccountGetRequest;
import fafoshop.pos.bankaccount.dto.BankAccountGetResponse;

/**
 * Lấy tài khoản ngân hàng nhận tiền của chi nhánh thu ngân đang đăng nhập,
 * dùng build mã QR chuyển khoản (chuẩn EMVCo/Napas247) phía frontend lúc in
 * hoá đơn. Chưa có màn quản lý bank_account riêng — đọc thẳng bảng, ghi rõ
 * `configured=false` nếu chi nhánh CHƯA được seed/cấu hình (không coi là lỗi
 * nghiệp vụ). Xem docs/pos-in-hoa-don.md (gốc workspace).
 *
 * Không khai function_code riêng ({@link #getFuncId()} trả về null) — đọc
 * cấu hình để in bill là thao tác cơ bản của MỌI thu ngân đã đăng nhập,
 * không cần ma trận phân quyền riêng (retail-domain.md: ma trận phân quyền
 * chi tiết vẫn UNKNOWN, chỉ 2 mã PRDCT_VIEW/PRDCT_EDIT là ví dụ mẫu).
 */
public class BankAccountGetProcess extends AbstractProcess {

	public BankAccountGetProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new BankAccountGetResponse();
	}

	@Override
	protected String getFuncId() {
		return null;
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		BankAccountGetRequest req = (BankAccountGetRequest) request;
		BankAccountGetResponse res = (BankAccountGetResponse) response;

		String branchCode = getCashierBranchCode(dba, req.accessInfo.userCode);

		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT bank_bin, bank_name, account_no, account_name "
					+ "FROM bank_account WHERE branch_code = ? AND del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, branchCode);
			rs = ps.executeQuery();

			if (rs.next()) {
				res.configured = true;
				res.bankBin = rs.getString("bank_bin");
				res.bankName = rs.getString("bank_name");
				res.accountNo = rs.getString("account_no");
				res.accountName = rs.getString("account_name");
			} else {
				res.configured = false;
			}

			return res;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/**
	 * Chi nhánh của thu ngân đăng nhập = main_branch_code (giống
	 * SaleOrderCreateProcess.getCashierBranchCode) — accessInfo hiện chưa
	 * mang branchCode nên phải tra lại theo user_code mỗi lần.
	 */
	private String getCashierBranchCode(DBAccessor dba, String userCode) throws DBException, ProcessCheckErrorException {
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

	private void closeQuietly(ResultSet rs, DBStatement ps) throws DBException {
		try {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
		} catch (SQLException e) {
			throw new DBException(e);
		}
	}
}
