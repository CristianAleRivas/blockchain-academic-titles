package sv.edu.blockchain.api.repository;

public class BlockchainConflictException extends RuntimeException {
    public BlockchainConflictException(String message) {
        super(message);
    }
}
