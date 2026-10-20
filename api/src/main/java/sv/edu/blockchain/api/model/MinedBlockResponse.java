package sv.edu.blockchain.api.model;

public record MinedBlockResponse(BlockSummary block, int titleCount, String message) {
}
