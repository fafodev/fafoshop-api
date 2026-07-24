package fafoshop.pos.category.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.category.dto.CategorySearchRequest;
import fafoshop.pos.category.dto.CategorySearchResponse;
import fafoshop.pos.category.process.CategorySearchProcess;

@Path("pos/category")
public class CategorySearchWebService extends AbstractWebService {

	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public CategorySearchResponse search(CategorySearchRequest request) {
		return (CategorySearchResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new CategorySearchProcess(this);
	}
}
