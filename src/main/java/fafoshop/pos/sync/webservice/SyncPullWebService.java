package fafoshop.pos.sync.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fafoshop.common.process.AbstractProcess;
import fafoshop.common.webservice.AbstractWebService;
import fafoshop.pos.sync.dto.SyncPullRequest;
import fafoshop.pos.sync.dto.SyncPullResponse;
import fafoshop.pos.sync.process.SyncPullProcess;

@Path("pos/sync")
public class SyncPullWebService extends AbstractWebService {

	@POST
	@Path("/pull")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public SyncPullResponse pull(SyncPullRequest request) {
		return (SyncPullResponse) super.executeProcess(request);
	}

	@Override
	protected AbstractProcess getProcess() {
		return new SyncPullProcess(this);
	}
}
