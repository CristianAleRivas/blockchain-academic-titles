package sv.edu.blockchain.api.resource;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import sv.edu.blockchain.api.model.AcademicTitleView;
import sv.edu.blockchain.api.model.CreateTitleRequest;
import sv.edu.blockchain.api.model.PendingTitleResponse;
import sv.edu.blockchain.api.model.TitleDetails;
import sv.edu.blockchain.api.service.BlockchainService;

@Path("/api/titles")
@Produces(MediaType.APPLICATION_JSON)
public class TitleResource {
    private final BlockchainService service;

    @Inject
    public TitleResource(BlockchainService service) {
        this.service = service;
    }

    @GET
    public List<AcademicTitleView> getTitles(@QueryParam("node") String nodeId) {
        return service.findAllTitles(nodeId);
    }

    @GET
    @Path("/pending")
    public List<AcademicTitleView> getPendingTitles() {
        return service.findPendingTitles();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public PendingTitleResponse registerTitle(CreateTitleRequest request) {
        return service.registerTitle(request);
    }

    @GET
    @Path("/{transactionId}")
    public TitleDetails getTitle(@PathParam("transactionId") String transactionId,
                                 @QueryParam("node") String nodeId) {
        return service.findTitleByTransactionId(nodeId, transactionId)
                .orElseThrow(() -> new NotFoundException("No existe un título con ese identificador."));
    }
}
