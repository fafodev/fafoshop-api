package fafoshop.pos.product.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.product.dto.ProductExportRequest;
import fafoshop.pos.product.dto.ProductExportResponse;
import fafoshop.pos.product.process.ProductExportProcess;

/**
 * LỆCH khỏi khuôn @Produces(JSON) chuẩn của các WebService khác trong dự án
 * — luồng export cần trả file nhị phân (Excel/CSV) để trình duyệt tải về,
 * không phải JSON body. Vẫn tái dùng nguyên luồng permission/transaction qua
 * super.executeProcess() (chạy đúng AuthTokenFilter/checkAuth()/commit như
 * mọi Process khác) — chỉ khác ở bước cuối: nếu có lỗi nghiệp vụ
 * (lstFatalError/lstNormalError) thì vẫn trả JSON như thường lệ để client
 * đọc được thông báo lỗi; nếu thành công thì trả byte[] kèm
 * Content-Disposition để trình duyệt tải file.
 */
@Path("pos/product")
public class ProductExportWebService extends AbstractWebService {

	@POST
	@Path("/export")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/octet-stream" })
	public Response export(ProductExportRequest request) {
		ProductExportResponse res = (ProductExportResponse) super.executeProcess(request);

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
		return new ProductExportProcess(this);
	}
}
