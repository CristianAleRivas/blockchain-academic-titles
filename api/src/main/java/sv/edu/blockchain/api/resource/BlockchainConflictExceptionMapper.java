package sv.edu.blockchain.api.resource;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import sv.edu.blockchain.api.repository.BlockchainConflictException;

@Provider
public class BlockchainConflictExceptionMapper implements ExceptionMapper<BlockchainConflictException> {
    @Override
    public Response toResponse(BlockchainConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }

    public record ErrorResponse(String message) {
    }
}
