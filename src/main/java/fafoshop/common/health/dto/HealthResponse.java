package fafoshop.common.health.dto;

import fafoshop.common.dto.response.AbstractResponse;

/**
 * Kết quả kiểm tra sống — dùng cho script vận hành (deploy/watchdog trong
 * workspace, xem docs/pos-deploy-production.md) VÀ người kiểm tra tay bằng
 * curl. KHÔNG cần đăng nhập (endpoint @NoAuth).
 *
 * {@code ok}/{@code dbOk} mặc định {@code false} — nếu ping DB lỗi,
 * {@link fafoshop.common.health.process.HealthProcess} tự bắt lỗi và GIỮ
 * NGUYÊN giá trị mặc định này thay vì để lỗi rơi vào cơ chế
 * lstFatalError chung (health check DB chết là tình huống BÌNH THƯỜNG cần
 * phát hiện, không phải lỗi hệ thống bất ngờ) — script gọi vẫn nhận HTTP
 * 200 kèm {@code "dbOk":false} để tự đọc, không phải parse lstFatalError.
 */
public class HealthResponse extends AbstractResponse {

	/** true nếu Tomcat/Jersey VÀ kết nối DB đều sống — hiện tại tương đương dbOk. */
	public boolean ok = false;

	/** true nếu ping được DB (SELECT 1) thành công. */
	public boolean dbOk = false;

	/** Thời điểm server xử lý request này (yyyy-MM-dd HH:mm:ss) — xác nhận response không phải cache cũ. */
	public String serverTime;
}
