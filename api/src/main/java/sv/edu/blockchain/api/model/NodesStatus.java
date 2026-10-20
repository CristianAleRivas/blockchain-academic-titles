package sv.edu.blockchain.api.model;

import java.util.List;

public record NodesStatus(String miningNodeId, boolean chainsSynchronized, String message,
                          List<NodeStatus> nodes) {
}
