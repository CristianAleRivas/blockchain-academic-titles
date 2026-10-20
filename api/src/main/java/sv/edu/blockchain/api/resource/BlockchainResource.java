package sv.edu.blockchain.api.resource;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import sv.edu.blockchain.api.model.BlockDetails;
import sv.edu.blockchain.api.model.BlockSummary;
import sv.edu.blockchain.api.service.BlockchainService;

@Path("/api/blocks")
@Produces(MediaType.APPLICATION_JSON)
public class BlockchainResource {
    private final BlockchainService service;

    @Inject
    public BlockchainResource(BlockchainService service) {
        this.service = service;
    }

    @GET
    public List<BlockSummary> getBlocks(@QueryParam("node") String nodeId) {
        return service.findAllBlocks(nodeId);
    }

    @GET
    @Path("/{hash}")
    public BlockDetails getBlock(@PathParam("hash") String hash, @QueryParam("node") String nodeId) {
        return service.findBlockByHash(nodeId, hash)
                .orElseThrow(() -> new NotFoundException("No existe un bloque con ese hash."));
    }
}
