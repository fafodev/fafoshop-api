package fafoshop.pos.stock.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.stock.dto.StockLotListRequest;
import fafoshop.pos.stock.dto.StockLotListResponse;
import fafoshop.pos.stock.process.StockLotListProcess;

@Path("pos/stock")
public class StockLotListWebService extends AbstractWebService {

	@POST
	@Path("/lots")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public StockLotListResponse lots(StockLotListRequest request) {
		return (StockLotListResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new StockLotListProcess(this);
	}
}
