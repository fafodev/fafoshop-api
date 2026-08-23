package fafoshop.pos.stocktake.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.stocktake.dto.StocktakeListRequest;
import fafoshop.pos.stocktake.dto.StocktakeListResponse;
import fafoshop.pos.stocktake.process.StocktakeListProcess;

@Path("pos/stocktake")
public class StocktakeListWebService extends AbstractWebService {

	@POST
	@Path("/list")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public StocktakeListResponse list(StocktakeListRequest request) {
		return (StocktakeListResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new StocktakeListProcess(this);
	}
}
