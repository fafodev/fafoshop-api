package fafoshop.pos.stocktake.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.stocktake.dto.StocktakeSaveRequest;
import fafoshop.pos.stocktake.dto.StocktakeSaveResponse;
import fafoshop.pos.stocktake.process.StocktakeSaveProcess;

@Path("pos/stocktake")
public class StocktakeSaveWebService extends AbstractWebService {

	@POST
	@Path("/save")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public StocktakeSaveResponse save(StocktakeSaveRequest request) {
		return (StocktakeSaveResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new StocktakeSaveProcess(this);
	}
}
