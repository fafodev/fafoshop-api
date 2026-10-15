package fafoshop.pos.sync.process;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fafoshop.common.ILogSender;
import fafoshop.common.LogLevel;
import fafoshop.common.database.DBAccessor;
import fafoshop.common.database.DBStatement;
import fafoshop.common.dto.request.AbstractRequest;
import fafoshop.common.dto.response.AbstractResponse;
import fafoshop.common.exception.DBException;
import fafoshop.common.exception.FatalException;
import fafoshop.common.exception.ProcessCheckErrorException;
import fafoshop.common.process.AbstractProcess;
import fafoshop.pos.sync.dto.SyncProductDto;
import fafoshop.pos.sync.dto.SyncProductUnitDto;
import fafoshop.pos.sync.dto.SyncPullRequest;
import fafoshop.pos.sync.dto.SyncPullResponse;

/**
 * Trả FULL dữ liệu để app iOS FaFoMart POS bán hàng offline: sản phẩm đang
 * hiệu lực, đơn vị đóng gói, tài khoản ngân hàng của chi nhánh thu ngân.
 * CHỈ ĐỌC (SELECT) — không ghi bảng nào, không cần migration, không đổi hành
 * vi các module có sẵn. Xem docs/pos-ios-app.md.
 *
 * Quyền: dùng lại function_code {@code SALE_CREAT} (ai được bán hàng thì được
 * kéo danh mục để bán) — không tạo function_code mới nên không cần seed.
 * Không trả giá vốn (product.cost, product_unit.unit_cost).
 */
public class SyncPullProcess extends AbstractProcess {

	public SyncPullProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SyncPullResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_CREAT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SyncPullRequest req = (SyncPullRequest) request;
		SyncPullResponse res = (SyncPullResponse) response;

		res.serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		res.products = queryProducts(dba);
		res.productUnits = queryProductUnits(dba);
		fillBankAccount(dba, req.accessInfo.userCode, res);

		logSend(LogLevel.INFOMATION, "SyncPull:Finish products=" + res.products.size() + " units=" + res.productUnits.size());
		return res;
	}

	private List<SyncProductDto> queryProducts(DBAccessor dba) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT product_code, name, short_name, barcode, category_code, unit_name, price "
					+ "FROM product WHERE del_flg = '0' ORDER BY product_code";
			ps = dba.prepareStatement(sql);
			rs = ps.executeQuery();
			List<SyncProductDto> rows = new ArrayList<>();
			while (rs.next()) {
				SyncProductDto row = new SyncProductDto();
				row.productCode = rs.getString("product_code");
				row.name = rs.getString("name");
				row.shortName = rs.getString("short_name");
				row.barcode = rs.getString("barcode");
				row.categoryCode = rs.getString("category_code");
				row.unitName = rs.getString("unit_name");
				row.price = rs.getBigDecimal("price");
				rows.add(row);
			}
			return rows;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private List<SyncProductUnitDto> queryProductUnits(DBAccessor dba) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT u.product_code, u.unit_name, u.conversion_qty, u.unit_price "
					+ "FROM product_unit u JOIN product p ON p.product_code = u.product_code "
					+ "WHERE p.del_flg = '0' ORDER BY u.product_code, u.conversion_qty";
			ps = dba.prepareStatement(sql);
			rs = ps.executeQuery();
			List<SyncProductUnitDto> rows = new ArrayList<>();
			while (rs.next()) {
				SyncProductUnitDto row = new SyncProductUnitDto();
				row.productCode = rs.getString("product_code");
				row.unitName = rs.getString("unit_name");
				row.conversionQty = rs.getInt("conversion_qty");
				row.unitPrice = rs.getBigDecimal("unit_price");
				rows.add(row);
			}
			return rows;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/** Tài khoản NH theo main_branch_code của thu ngân (giống BankAccountGetProcess). */
	private void fillBankAccount(DBAccessor dba, String userCode, SyncPullResponse res) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT b.bank_bin, b.bank_name, b.account_no, b.account_name "
					+ "FROM app_user u JOIN bank_account b ON b.branch_code = u.main_branch_code "
					+ "WHERE u.user_code = ? AND b.del_flg = '0'";
			ps = dba.prepareStatement(sql);
			ps.setString(1, userCode);
			rs = ps.executeQuery();
			if (rs.next()) {
				res.bankConfigured = true;
				res.bankBin = rs.getString("bank_bin");
				res.bankName = rs.getString("bank_name");
				res.accountNo = rs.getString("account_no");
				res.accountName = rs.getString("account_name");
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
