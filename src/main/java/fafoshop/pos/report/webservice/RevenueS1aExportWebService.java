package fafoshop.pos.report.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.report.dto.RevenueS1aExportRequest;
import fafoshop.pos.report.dto.RevenueS1aExportResponse;
import fafoshop.pos.report.process.RevenueS1aExportProcess;

/**
 * LỆCH khỏi khuôn @Produces(JSON) chuẩn — trả file Excel nhị phân để trình
 * duyệt tải về, cùng pattern với ProductExportWebService/SaleOrderExportWebService.
 */
@Path("pos/report")
public class RevenueS1aExportWebService extends AbstractWebService {

	@POST
	@Path("/exportS1a")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/octet-stream" })
	public Response exportS1a(RevenueS1aExportRequest request) {
		RevenueS1aExportResponse res = (RevenueS1aExportResponse) super.executeProcess(request);

		boolean hasError = res == null || !res.getFatalError().isEmpty() || !res.getNormalError().isEmpty()
				|| res.fileBytes == null;

		if (hasError) {
			return Response.ok(res).type(MediaType.APPLICATION_JSON + ";charset=utf-8").build();
		}

		return Response.ok(res.fileBytes)
				.header("Content-Disposition", "attachment; filename=\"" + res.fileName + "\"")
				.type(res.contentType)
				.build();
	}

	@Override
	protected AbstractProcess getProcess() {
		return new RevenueS1aExportProcess(this);
	}
}
