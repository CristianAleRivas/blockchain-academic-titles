package sv.edu.blockchain.protocol;

import java.util.List;

public record NetworkResponse(boolean success, String message, List<BlockData> blocks, BlockData block) {
    public static NetworkResponse error(String message) {
        return new NetworkResponse(false, message, List.of(), null);
    }
}
