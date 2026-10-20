package sv.edu.blockchain.api.model;

public record ChainValidation(boolean valid, int blockCount, String message) {
}
