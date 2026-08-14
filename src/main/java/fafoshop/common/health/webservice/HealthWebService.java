package fafoshop.common.health.webservice;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.auth.NoAuth;
import fafoshop.common.health.dto.HealthRequest;
import fafoshop.common.health.dto.HealthResponse;
import fafoshop.common.health.process.HealthProcess;
import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;

/**
 * {@code GET /api/health} — endpoint kiểm tra sống, KHÔNG cần đăng nhập,
 * dùng cho script vận hành (deploy/watchdog, xem
 * docs/pos-deploy-production.md) và kiểm tra tay bằng curl. Khác quy ước
 * POST-với-body của các endpoint nghiệp vụ khác trong dự án (xem
 * architecture.md) — cố ý dùng GET vì đây không phải hành động nghiệp vụ,
 * không có input, và GET là quy ước phổ biến cho health check ở mọi công cụ
 * giám sát/health probe (kể cả tự gọi bằng trình duyệt/curl không cần body).
 */
@Path("health")
public class HealthWebService extends AbstractWebService {

	@GET
	@NoAuth
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public HealthResponse health() {
		return (HealthResponse) super.executeProcess(new HealthRequest());
	}

	@Override
	protected AbstractProcess getProcess() {
		return new HealthProcess(this);
	}
}
