package sv.edu.blockchain.api.model;

public record NodeStatus(String id, String name, String host, int port, boolean connected,
                         int blockCount, String lastBlockHash, boolean chainValid, String message) {
}
