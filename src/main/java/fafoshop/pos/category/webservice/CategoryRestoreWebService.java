package fafoshop.pos.category.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.category.dto.CategoryRestoreRequest;
import fafoshop.pos.category.dto.CategoryRestoreResponse;
import fafoshop.pos.category.process.CategoryRestoreProcess;

@Path("pos/category")
public class CategoryRestoreWebService extends AbstractWebService {

	@POST
	@Path("/restore")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public CategoryRestoreResponse restore(CategoryRestoreRequest request) {
		return (CategoryRestoreResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new CategoryRestoreProcess(this);
	}
}
