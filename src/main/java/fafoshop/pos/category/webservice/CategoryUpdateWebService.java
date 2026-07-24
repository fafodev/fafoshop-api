package fafoshop.pos.category.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.category.dto.CategoryUpdateRequest;
import fafoshop.pos.category.dto.CategoryUpdateResponse;
import fafoshop.pos.category.process.CategoryUpdateProcess;

@Path("pos/category")
public class CategoryUpdateWebService extends AbstractWebService {

	@POST
	@Path("/update")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public CategoryUpdateResponse update(CategoryUpdateRequest request) {
		return (CategoryUpdateResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new CategoryUpdateProcess(this);
	}
}
