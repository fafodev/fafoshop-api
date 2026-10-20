package fafoshop.pos.saleorder.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import fafoshop.common.ConstantValue;
import fafoshop.common.ILogSender;
import fafoshop.common.LogLevel;
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
import fafoshop.common.utility.SeqNoUtility;
import fafoshop.pos.saleorder.dto.PaymentMethod;
import fafoshop.pos.saleorder.dto.SaleOrderBackfillRequest;
import fafoshop.pos.saleorder.dto.SaleOrderBackfillResponse;
import fafoshop.pos.saleorder.dto.SaleOrderItemDto;

/**
 * Bổ sung đơn bán cho 1 thời điểm TRƯỚC ĐÓ — khách đã mua/trả tiền (tiền mặt
 * hoặc chuyển khoản) nhưng thu ngân quên nhập hoặc bán đông không kịp nhập.
 * Xem docs/pos-bo-sung-don-ban.md (gốc workspace) — quyết định đã chốt với
 * người dùng.
 *
 * Khác {@link SaleOrderCreateProcess} (KHÔNG sửa class đó — cố ý tách riêng,
 * cùng cách SaleOrderUpdateProcess giữ độc lập với Create):
 * <ul>
 * <li>sale_datetime = thời điểm bán do người dùng nhập (không được ở tương
 * lai, không giới hạn lùi về quá khứ). entry_datetime/update_datetime của
 * sale_order VÀ sale_order_item cũng ghi theo ĐÚNG thời điểm đó (+ phần
 * micro giây ngẫu nhiên trong cùng giây) — mọi mốc thời gian của đơn đồng bộ
 * với ngày bán thật.</li>
 * <li>KHÔNG trừ tồn kho, không chọn lô (stock_code/expiry_date NULL). Không
 * có cờ phân loại đơn (người dùng tự ghi chú) — Void/Update nhận ra đơn tạo
 * ở đây qua cột audit sẵn có entry_program = {@link #PRG_CD} để KHÔNG
 * hoàn/điều chỉnh tồn.</li>
 * <li>Số đơn dùng CHUNG dãy `HD`: ngày bán = hôm nay → sinh qua
 * SeqNoUtility như POS (tránh đụng số POS sắp cấp); ngày trước đó → số lớn
 * nhất đã có của đúng ngày đó + 1 (xem {@link #nextBackfillOrderNo}).</li>
 * </ul>
 * Giá bán/giá vốn xử lý y hệt POS (lineAmount client gửi có kiểm tra lệch,
 * giá vốn snapshot theo Product Master hiện tại). Quyền: dùng chung
 * `SALE_CREAT` của POS (không giới hạn riêng — quyết định của người dùng).
 */
public class SaleOrderBackfillProcess extends AbstractProcess {

	/**
	 * Mã chương trình ghi vào entry_program/update_program (cột audit chuẩn
	 * mọi bảng). Package-private vì SaleOrderStockAdjuster.isBackfillOrder()
	 * đọc lại sale_order.entry_program = mã này để huỷ/sửa đơn KHÔNG hoàn/điều
	 * chỉnh tồn (đơn tạo ở đây không trừ tồn) — KHÔNG dùng cột cờ riêng
	 * (quyết định của người dùng: không phân loại đơn bổ sung/đơn thường).
	 */
	static final String PRG_CD = "SALE_BKF";

	private static final String SEQ_PREFIX = "HD";

	private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final DateTimeFormatter ORDER_NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

	public SaleOrderBackfillProcess(ILogSender logSender) {
		super(logSender);
	}

	@Override
	protected AbstractResponse createNewResponse(AbstractRequest request) {
		return new SaleOrderBackfillResponse();
	}

	@Override
	protected String getFuncId() {
		return "SALE_CREAT";
	}

	@Override
	public AbstractResponse process(DBAccessor dba, AbstractRequest request, AbstractResponse response,
			AbstractResponse parentResponse) throws FatalException, DBException, ProcessCheckErrorException {

		SaleOrderBackfillRequest req = (SaleOrderBackfillRequest) request;
		SaleOrderBackfillResponse res = (SaleOrderBackfillResponse) response;

		// Chống ghi trùng — kiểm tra TRƯỚC mọi validate khác (cùng lý do với
		// SaleOrderCreateProcess, xem docs/pos-chong-ghi-trung-don.md).
		String idempotencyKey = normalizeIdempotencyKey(req.idempotencyKey);
		if (idempotencyKey != null && replayExistingOrder(dba, idempotencyKey, req.accessInfo.userCode, res)) {
			logSend(LogLevel.INFOMATION, "Idempotency:Replay saleOrderNo=" + res.saleOrderNo);
			return res;
		}

		LocalDateTime saleDatetime = parseSaleDatetime(req.saleDatetime);

		validateItems(req.items);
		validatePaymentMethod(req.paymentMethod);

		BigDecimal subtotal = BigDecimal.ZERO;
		for (SaleOrderItemDto item : req.items) {
			validateItemExists(dba, item.productCode);
			subtotal = subtotal.add(effectiveLineAmount(item));
		}

		validatePaidAmount(req.paidAmount, subtotal);

		String branchCode = getCashierBranchCode(dba, req.accessInfo.userCode);
		BigDecimal changeAmount = req.paidAmount.subtract(subtotal);

		String saleOrderNo = saleDatetime.toLocalDate().equals(LocalDate.now())
				? SeqNoUtility.generate(dba, SEQ_PREFIX, req.accessInfo.userCode, PRG_CD)
				: nextBackfillOrderNo(dba, saleDatetime.toLocalDate());

		Timestamp saleTs = Timestamp.valueOf(saleDatetime);
		Timestamp auditTs = withRandomFraction(saleDatetime);

		insertSaleOrder(dba, saleOrderNo, branchCode, req.customerName, saleTs, auditTs, req.paidAmount,
				changeAmount, req.paymentMethod, req.accessInfo.userCode, idempotencyKey);
		insertSaleOrderItems(dba, saleOrderNo, req.items, auditTs, req.accessInfo.userCode);

		res.saleOrderNo = saleOrderNo;
		res.subtotal = subtotal;
		res.changeAmount = changeAmount;
		res.saleDatetime = saleDatetime.format(DATETIME_FMT);
		res.duplicate = false;
		logSend(LogLevel.INFOMATION, "Backfill:Created saleOrderNo=" + saleOrderNo + " saleDatetime=" + res.saleDatetime);
		return res;
	}

	/**
	 * `yyyy-MM-dd HH:mm:ss` (chấp nhận thêm dạng ISO `yyyy-MM-ddTHH:mm[:ss]`
	 * của input datetime trình duyệt). Không được sau thời điểm hiện tại —
	 * "bổ sung" chỉ cho đơn ĐÃ bán; không giới hạn lùi về quá khứ (quyết định
	 * của người dùng).
	 */
	private LocalDateTime parseSaleDatetime(String raw) throws ProcessCheckErrorException {
		if (raw == null || raw.trim().isEmpty()) {
			throwError("ME000145");
		}
		LocalDateTime value = null;
		String text = raw.trim().replace('T', ' ');
		try {
			value = text.length() == 16
					? LocalDateTime.parse(text + ":00", DATETIME_FMT)
					: LocalDateTime.parse(text, DATETIME_FMT);
		} catch (Exception e) {
			throwError("ME000145");
		}
		if (value.isAfter(LocalDateTime.now())) {
			throwError("ME000146");
		}
		return value;
	}

	/**
	 * Mốc entry_datetime/update_datetime = đúng giây bán + phần micro giây
	 * ngẫu nhiên (cột TIMESTAMP(6)) — đồng bộ ngày giờ với đơn mà vẫn không
	 * trùng tròn số giây (yêu cầu của người dùng). sale_datetime (DATETIME,
	 * không phần lẻ) giữ đúng giây người dùng nhập.
	 */
	private Timestamp withRandomFraction(LocalDateTime saleDatetime) {
		Timestamp ts = Timestamp.valueOf(saleDatetime);
		ts.setNanos(ThreadLocalRandom.current().nextInt(0, 1_000_000) * 1000);
		return ts;
	}

	/**
	 * Số đơn cho NGÀY TRƯỚC hôm nay — dùng CHUNG dãy `HD` (không tạo sequence
	 * mới): `HD` + ngày bán + (số lớn nhất đã có của ĐÚNG ngày đó + 1), đệm 0
	 * đủ max_digit của prefix HD.
	 *
	 * Ngày đó chưa có đơn nào → nối tiếp số lớn nhất của NGÀY GẦN NHẤT trước đó
	 * (giữ số tăng dần theo thời gian như dãy POS), chưa có đơn nào trước đó
	 * nữa → bắt đầu từ 1.
	 *
	 * KHÔNG đụng số của POS: SeqNoUtility luôn ghép ngày HIỆN TẠI, nên không
	 * bao giờ sinh lại số có phần ngày ở quá khứ. (Ngày bán = hôm nay thì
	 * process đi đường SeqNoUtility, không qua hàm này.)
	 *
	 * So số theo GIÁ TRỊ (CAST) chứ không theo chuỗi — phần số có thể dài hơn
	 * max_digit khi vượt 9999 (xem seqno-convention.md), so chuỗi sẽ sai.
	 *
	 * `FOR UPDATE` khoá khoảng khoá chính của ngày đó (kể cả khi chưa có dòng
	 * nào): 2 người bổ sung cùng ngày cùng lúc sẽ tắc nhau thành deadlock ở
	 * bước INSERT → AbstractProcess tự retry → lần retry thấy số mới, không
	 * trùng khoá chính.
	 */
	private String nextBackfillOrderNo(DBAccessor dba, LocalDate saleDate) throws DBException {
		String dayPrefix = SEQ_PREFIX + saleDate.format(ORDER_NO_DATE_FMT);
		Long maxOfDay = queryMaxNumberOfDay(dba, dayPrefix, true);
		long next;
		if (maxOfDay != null) {
			next = maxOfDay + 1;
		} else {
			String earlierPrefix = queryNearestEarlierDayPrefix(dba, dayPrefix);
			Long maxEarlier = earlierPrefix == null ? null : queryMaxNumberOfDay(dba, earlierPrefix, false);
			next = (maxEarlier == null ? 0L : maxEarlier) + 1;
		}
		return dayPrefix + String.format("%0" + queryMaxDigit(dba) + "d", next);
	}

	private Long queryMaxNumberOfDay(DBAccessor dba, String dayPrefix, boolean lock) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT MAX(CAST(SUBSTRING(sale_order_no, ?) AS UNSIGNED)) AS max_no FROM sale_order "
					+ "WHERE sale_order_no LIKE ? AND sale_order_no REGEXP '^HD[0-9]+$'" + (lock ? " FOR UPDATE" : "");
			ps = dba.prepareStatement(sql);
			ps.setInt(1, dayPrefix.length() + 1);
			ps.setString(2, dayPrefix + "%");
			rs = ps.executeQuery();
			if (!rs.next()) {
				return null;
			}
			long value = rs.getLong("max_no");
			return rs.wasNull() ? null : value;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/** Phần `HD`+ngày của đơn có ngày gần nhất TRƯỚC dayPrefix (so chuỗi đúng vì phần ngày luôn đủ 8 số). */
	private String queryNearestEarlierDayPrefix(DBAccessor dba, String dayPrefix) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT MAX(SUBSTRING(sale_order_no, 1, ?)) AS day_prefix FROM sale_order "
					+ "WHERE sale_order_no < ? AND sale_order_no REGEXP '^HD[0-9]{12,}$'";
			ps = dba.prepareStatement(sql);
			ps.setInt(1, dayPrefix.length());
			ps.setString(2, dayPrefix);
			rs = ps.executeQuery();
			return rs.next() ? rs.getString("day_prefix") : null;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/** max_digit của prefix HD (đồng bộ độ dài số với SeqNoUtility), mặc định 4 nếu thiếu cấu hình. */
	private int queryMaxDigit(DBAccessor dba) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT max_digit FROM seq_no WHERE prefix = ?");
			ps.setString(1, SEQ_PREFIX);
			rs = ps.executeQuery();
			int digit = rs.next() ? rs.getInt("max_digit") : 0;
			return digit > 0 ? digit : 4;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/** Giống hệt SaleOrderCreateProcess.normalizeIdempotencyKey(). */
	private String normalizeIdempotencyKey(String raw) throws ProcessCheckErrorException {
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		String key = raw.trim();
		if (!key.matches("[A-Za-z0-9-]{8,64}")) {
			throwError("ME000142");
		}
		return key;
	}

	/** Giống SaleOrderCreateProcess.replayExistingOrder() — thêm trả lại sale_datetime của đơn cũ để in bill đúng ngày. */
	private boolean replayExistingOrder(DBAccessor dba, String idempotencyKey, String userCode,
			SaleOrderBackfillResponse res) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			String sql = "SELECT so.sale_order_no, so.entry_user_code, so.change_amount, so.sale_datetime, "
					+ "(SELECT COALESCE(SUM(soi.line_amount), 0) FROM sale_order_item soi "
					+ " WHERE soi.sale_order_no = so.sale_order_no) AS subtotal "
					+ "FROM sale_order so WHERE so.idempotency_key = ? FOR UPDATE";
			ps = dba.prepareStatement(sql);
			ps.setString(1, idempotencyKey);
			rs = ps.executeQuery();
			if (!rs.next()) {
				return false;
			}
			if (!userCode.equals(rs.getString("entry_user_code"))) {
				throwError("ME000143");
			}
			res.saleOrderNo = rs.getString("sale_order_no");
			res.subtotal = rs.getBigDecimal("subtotal");
			res.changeAmount = rs.getBigDecimal("change_amount");
			Timestamp saleTs = rs.getTimestamp("sale_datetime");
			res.saleDatetime = saleTs != null ? saleTs.toLocalDateTime().format(DATETIME_FMT) : null;
			res.duplicate = true;
			return true;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	/** Giống hệt SaleOrderCreateProcess.validateItems(). */
	private void validateItems(List<SaleOrderItemDto> items) throws ProcessCheckErrorException {
		if (items == null || items.isEmpty()) {
			throwError("ME000085");
		}
		for (SaleOrderItemDto item : items) {
			if (item.productCode == null || item.productCode.trim().isEmpty()) {
				throwError("ME000061");
			}
			if (item.quantity == null || item.quantity <= 0) {
				throwError("ME000086");
			}
			if (item.unitPrice == null || item.unitPrice.signum() < 0) {
				throwError("ME000067");
			}
			validateLineAmount(item);
		}
	}

	/** Giống hệt SaleOrderCreateProcess.validateLineAmount() — lineAmount lệch tối đa `quantity` đồng (ME000126). */
	private void validateLineAmount(SaleOrderItemDto item) throws ProcessCheckErrorException {
		if (item.lineAmount == null) {
			return;
		}
		if (item.lineAmount.signum() < 0) {
			throwError("ME000126");
		}
		BigDecimal computed = item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));
		BigDecimal maxDrift = BigDecimal.valueOf(item.quantity);
		if (item.lineAmount.subtract(computed).abs().compareTo(maxDrift) > 0) {
			throwError("ME000126");
		}
	}

	private BigDecimal effectiveLineAmount(SaleOrderItemDto item) {
		return item.lineAmount != null ? item.lineAmount : item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));
	}

	private void validateItemExists(DBAccessor dba, String productCode) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT product_code FROM product WHERE product_code = ? AND del_flg = '0'");
			ps.setString(1, productCode);
			rs = ps.executeQuery();
			if (!rs.next()) {
				throwError("ME000061");
			}
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void validatePaidAmount(BigDecimal paidAmount, BigDecimal subtotal) throws ProcessCheckErrorException {
		if (paidAmount == null || paidAmount.compareTo(subtotal) < 0) {
			throwError("ME000087");
		}
	}

	private void validatePaymentMethod(String paymentMethod) throws ProcessCheckErrorException {
		if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
			throwError("ME000097");
		}
		if (!PaymentMethod.isValid(paymentMethod)) {
			throwError("ME000098");
		}
	}

	/** Chi nhánh = main_branch_code của thu ngân đang đăng nhập, KHÔNG nhận từ client (giống POS). */
	private String getCashierBranchCode(DBAccessor dba, String userCode) throws DBException, ProcessCheckErrorException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT main_branch_code FROM app_user WHERE user_code = ?");
			ps.setString(1, userCode);
			rs = ps.executeQuery();
			String branchCode = rs.next() ? rs.getString("main_branch_code") : null;
			if (branchCode == null || branchCode.trim().isEmpty()) {
				throwError("ME000088");
			}
			return branchCode;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void insertSaleOrder(DBAccessor dba, String saleOrderNo, String branchCode, String customerName,
			Timestamp saleDatetime, Timestamp auditDatetime, BigDecimal paidAmount, BigDecimal changeAmount,
			String paymentMethod, String userCode, String idempotencyKey) throws DBException {

		DBStatement ps = null;
		try {
			String sql = "INSERT INTO sale_order "
					+ "(sale_order_no, branch_code, customer_code, customer_name, sale_datetime, "
					+ " paid_amount, change_amount, payment_method, cashier_user_code, void_flg, idempotency_key, "
					+ " entry_user_code, entry_datetime, entry_program, update_user_code, update_datetime, update_program) "
					+ "VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, '0', ?, ?, ?, ?, ?, ?, ?)";

			ps = dba.prepareStatement(sql);
			ps.setString(1, saleOrderNo);
			ps.setString(2, branchCode);
			ps.setString(3, customerName);
			ps.setTimestamp(4, saleDatetime);
			ps.setBigDecimal(5, paidAmount);
			ps.setBigDecimal(6, changeAmount);
			ps.setString(7, paymentMethod);
			ps.setString(8, userCode);
			ps.setString(9, idempotencyKey);
			ps.setString(10, userCode);
			ps.setTimestamp(11, auditDatetime);
			ps.setString(12, PRG_CD);
			ps.setString(13, userCode);
			ps.setTimestamp(14, auditDatetime);
			ps.setString(15, PRG_CD);
			ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	/** stock_code/expiry_date luôn NULL — đơn bổ sung không trừ tồn, không gắn lô. */
	private void insertSaleOrderItems(DBAccessor dba, String saleOrderNo, List<SaleOrderItemDto> items,
			Timestamp auditDatetime, String userCode) throws DBException {

		DBStatement ps = null;
		try {
			String sql = "INSERT INTO sale_order_item "
					+ "(sale_order_no, line_no, product_code, stock_code, expiry_date, unit_price, quantity, line_amount, unit_cost, "
					+ " unit_name, unit_qty, "
					+ " entry_user_code, entry_datetime, entry_program, update_user_code, update_datetime, update_program) "
					+ "VALUES (?, ?, ?, NULL, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

			ps = dba.prepareStatement(sql);
			int lineNo = 1;
			for (SaleOrderItemDto item : items) {
				ps.setString(1, saleOrderNo);
				ps.setInt(2, lineNo++);
				ps.setString(3, item.productCode);
				ps.setBigDecimal(4, item.unitPrice);
				ps.setInt(5, item.quantity);
				ps.setBigDecimal(6, effectiveLineAmount(item));
				ps.setBigDecimal(7, resolveUnitCost(dba, item.productCode, item.unitName));
				ps.setString(8, item.unitName);
				ps.setNullableInt(9, item.unitQty);
				ps.setString(10, userCode);
				ps.setTimestamp(11, auditDatetime);
				ps.setString(12, PRG_CD);
				ps.setString(13, userCode);
				ps.setTimestamp(14, auditDatetime);
				ps.setString(15, PRG_CD);
				ps.executeUpdate();
			}
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
	}

	/** Giống hệt SaleOrderCreateProcess.resolveUnitCost() — giá vốn theo Product Master HIỆN TẠI (không có lịch sử giá). */
	private BigDecimal resolveUnitCost(DBAccessor dba, String productCode, String unitName) throws DBException {
		if (unitName == null || unitName.trim().isEmpty()) {
			return queryProductCost(dba, productCode);
		}
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT unit_cost, conversion_qty FROM product_unit WHERE product_code = ? AND unit_name = ?");
			ps.setString(1, productCode);
			ps.setString(2, unitName);
			rs = ps.executeQuery();
			if (!rs.next()) {
				return null;
			}
			BigDecimal unitCost = rs.getBigDecimal("unit_cost");
			int conversionQty = rs.getInt("conversion_qty");
			if (unitCost == null || conversionQty <= 0) {
				return null;
			}
			return unitCost.divide(BigDecimal.valueOf(conversionQty), 2, RoundingMode.HALF_UP);
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private BigDecimal queryProductCost(DBAccessor dba, String productCode) throws DBException {
		ResultSet rs = null;
		DBStatement ps = null;
		try {
			ps = dba.prepareStatement("SELECT cost FROM product WHERE product_code = ?");
			ps.setString(1, productCode);
			rs = ps.executeQuery();
			return rs.next() ? rs.getBigDecimal("cost") : null;
		} catch (SQLException e) {
			throw new DBException(e);
		} finally {
			closeQuietly(rs, ps);
		}
	}

	private void throwError(String errId) throws ProcessCheckErrorException {
		List<ErrorDto> errors = new ArrayList<>();
		ErrorDto error = new ErrorDto();
		error.errId = errId;
		error.errMsg = MessageUtility.getSystemErrMsg(errId);
		errors.add(error);
		throw new ProcessCheckErrorException(errors, ConstantValue.NORMAL_ERROR);
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
