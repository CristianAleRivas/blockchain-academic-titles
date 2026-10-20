package sv.edu.blockchain.api.model;

import java.util.List;

public record BlockDetails(int id, long timestamp, String previousHash, String hash, long nonce,
                           List<AcademicTitleView> transactions) {
}
