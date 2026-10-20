package sv.edu.blockchain.protocol;

public record NetworkRequest(String action, String hash, BlockData block) {
}
