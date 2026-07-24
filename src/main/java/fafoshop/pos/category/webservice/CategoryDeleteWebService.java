package fafoshop.pos.category.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.category.dto.CategoryDeleteRequest;
import fafoshop.pos.category.dto.CategoryDeleteResponse;
import fafoshop.pos.category.process.CategoryDeleteProcess;

@Path("pos/category")
public class CategoryDeleteWebService extends AbstractWebService {

	@POST
	@Path("/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public CategoryDeleteResponse delete(CategoryDeleteRequest request) {
		return (CategoryDeleteResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new CategoryDeleteProcess(this);
	}
}
