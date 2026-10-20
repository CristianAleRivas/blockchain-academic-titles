package sv.edu.blockchain.api.model;

public record ChainStatus(boolean nodeConnected, int blockCount, String message) {
}
