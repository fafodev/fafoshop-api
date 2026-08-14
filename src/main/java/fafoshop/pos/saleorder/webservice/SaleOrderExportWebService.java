package fafoshop.pos.saleorder.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.saleorder.dto.SaleOrderExportRequest;
import fafoshop.pos.saleorder.dto.SaleOrderExportResponse;
import fafoshop.pos.saleorder.process.SaleOrderExportProcess;

/**
 * LỆCH khỏi khuôn @Produces(JSON) chuẩn — luồng export cần trả file nhị phân
 * (Excel/CSV) để trình duyệt tải về. Theo đúng mẫu ProductExportWebService.
 */
@Path("pos/saleorder")
public class SaleOrderExportWebService extends AbstractWebService {

	@POST
	@Path("/export")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/octet-stream" })
	public Response export(SaleOrderExportRequest request) {
		SaleOrderExportResponse res = (SaleOrderExportResponse) super.executeProcess(request);

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
		return new SaleOrderExportProcess(this);
	}
}
