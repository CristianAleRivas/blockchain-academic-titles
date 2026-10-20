package sv.edu.blockchain.api.repository;

public class BlockchainUnavailableException extends RuntimeException {
    public BlockchainUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
