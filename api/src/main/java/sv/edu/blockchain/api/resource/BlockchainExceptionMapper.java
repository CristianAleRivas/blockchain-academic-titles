package sv.edu.blockchain.api.resource;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import sv.edu.blockchain.api.repository.BlockchainUnavailableException;

@Provider
public class BlockchainExceptionMapper implements ExceptionMapper<BlockchainUnavailableException> {
    @Override
    public Response toResponse(BlockchainUnavailableException exception) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }

    public record ErrorResponse(String message) {
    }
}
