package sv.edu.blockchain.api.model;

public record BlockSummary(int id, long timestamp, String previousHash, String hash, int transactionCount) {
}
