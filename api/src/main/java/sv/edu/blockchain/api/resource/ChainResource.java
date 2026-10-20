package sv.edu.blockchain.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import sv.edu.blockchain.api.model.ChainValidation;
import sv.edu.blockchain.api.model.ChainStatus;
import sv.edu.blockchain.api.model.MinedBlockResponse;
import sv.edu.blockchain.api.model.NodesStatus;
import sv.edu.blockchain.api.service.BlockchainService;

@Path("/api/chain")
@Produces(MediaType.APPLICATION_JSON)
public class ChainResource {
    private final BlockchainService service;

    @Inject
    public ChainResource(BlockchainService service) {
        this.service = service;
    }

    @GET
    @Path("/validation")
    public ChainValidation validateChain(@QueryParam("node") String nodeId) {
        return service.validateChain(nodeId);
    }

    @GET
    @Path("/status")
    public ChainStatus getChainStatus(@QueryParam("node") String nodeId) {
        return service.getChainStatus(nodeId);
    }

    @GET
    @Path("/nodes")
    public NodesStatus getNodesStatus() {
        return service.getNodesStatus();
    }

    @POST
    @Path("/mine")
    public MinedBlockResponse minePendingTitles() {
        return service.minePendingTitles();
    }
}
