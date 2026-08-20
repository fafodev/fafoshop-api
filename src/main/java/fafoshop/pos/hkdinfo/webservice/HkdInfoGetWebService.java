package fafoshop.pos.hkdinfo.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.hkdinfo.dto.HkdInfoGetRequest;
import fafoshop.pos.hkdinfo.dto.HkdInfoGetResponse;
import fafoshop.pos.hkdinfo.process.HkdInfoGetProcess;

@Path("pos/hkdinfo")
public class HkdInfoGetWebService extends AbstractWebService {

	@POST
	@Path("/get")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public HkdInfoGetResponse get(HkdInfoGetRequest request) {
		return (HkdInfoGetResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new HkdInfoGetProcess(this);
	}
}
