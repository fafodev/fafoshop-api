package fafoshop.pos.category.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.category.dto.CategoryCreateRequest;
import fafoshop.pos.category.dto.CategoryCreateResponse;
import fafoshop.pos.category.process.CategoryCreateProcess;

@Path("pos/category")
public class CategoryCreateWebService extends AbstractWebService {

	@POST
	@Path("/create")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public CategoryCreateResponse create(CategoryCreateRequest request) {
		return (CategoryCreateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new CategoryCreateProcess(this);
	}
}
