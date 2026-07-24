package fafoshop.pos.category.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.category.dto.CategoryListRequest;
import fafoshop.pos.category.dto.CategoryListResponse;
import fafoshop.pos.category.process.CategoryListProcess;

@Path("pos/category")
public class CategoryListWebService extends AbstractWebService {

	@POST
	@Path("/list")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public CategoryListResponse list(CategoryListRequest request) {
		return (CategoryListResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new CategoryListProcess(this);
	}
}
