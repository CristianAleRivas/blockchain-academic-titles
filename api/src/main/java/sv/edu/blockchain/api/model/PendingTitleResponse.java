package sv.edu.blockchain.api.model;

public record PendingTitleResponse(AcademicTitleView title, int pendingCount, String message) {
}
