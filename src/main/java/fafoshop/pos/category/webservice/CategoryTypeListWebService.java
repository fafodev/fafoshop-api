package fafoshop.pos.category.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.category.dto.CategoryTypeListRequest;
import fafoshop.pos.category.dto.CategoryTypeListResponse;
import fafoshop.pos.category.process.CategoryTypeListProcess;

@Path("pos/category")
public class CategoryTypeListWebService extends AbstractWebService {

	@POST
	@Path("/type/list")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public CategoryTypeListResponse typeList(CategoryTypeListRequest request) {
		return (CategoryTypeListResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new CategoryTypeListProcess(this);
	}
}
