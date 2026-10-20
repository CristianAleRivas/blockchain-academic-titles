package sv.edu.blockchain.api.repository;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import sv.edu.blockchain.BlockChain;
import sv.edu.blockchain.api.model.NodeStatus;
import sv.edu.blockchain.protocol.BlockData;
import sv.edu.blockchain.protocol.NetworkRequest;
import sv.edu.blockchain.protocol.NetworkResponse;
import sv.edu.blockchain.protocol.TlsBlockchainClient;

@ApplicationScoped
public class BlockchainRepository {
    private final Map<String, NodeConnection> nodes;
    private final String primaryNodeId = "node1";

    public BlockchainRepository(
            @ConfigProperty(name = "blockchain.node.host") String primaryHost,
            @ConfigProperty(name = "blockchain.node.port") int primaryPort,
            @ConfigProperty(name = "blockchain.peer.host") String peerHost,
            @ConfigProperty(name = "blockchain.peer.port") int peerPort) {
        Map<String, NodeConnection> configuredNodes = new LinkedHashMap<>();
        configuredNodes.put(primaryNodeId, new NodeConnection("Nodo 1", primaryHost, primaryPort,
                new TlsBlockchainClient(primaryHost, primaryPort)));
        configuredNodes.put("node2", new NodeConnection("Nodo 2", peerHost, peerPort,
                new TlsBlockchainClient(peerHost, peerPort)));
        this.nodes = configuredNodes;
    }

    public List<BlockData> findAllBlocks() {
        return findAllBlocks(primaryNodeId);
    }

    public List<BlockData> findAllBlocks(String nodeId) {
        NodeConnection node = findNode(nodeId);
        try {
            return node.client().getChain();
        } catch (IOException exception) {
            throw new BlockchainUnavailableException(
                    "No se pudo consultar la cadena de " + node.name() + ".", exception);
        }
    }

    public Optional<BlockData> findBlockByHash(String hash) {
        return findBlockByHash(primaryNodeId, hash);
    }

    public Optional<BlockData> findBlockByHash(String nodeId, String hash) {
        NodeConnection node = findNode(nodeId);
        try {
            NetworkResponse response = node.client().send(new NetworkRequest("get-block", hash, null));
            return response.success() ? Optional.of(response.block()) : Optional.empty();
        } catch (IOException exception) {
            throw new BlockchainUnavailableException(
                    "No se pudo consultar el bloque en " + node.name() + ".", exception);
        }
    }

    public BlockData submitBlock(BlockData block) {
        return submitBlock(primaryNodeId, block);
    }

    public BlockData submitBlock(String nodeId, BlockData block) {
        NodeConnection node = findNode(nodeId);
        try {
            NetworkResponse response = node.client().send(new NetworkRequest("submit-block", null, block));
            if (!response.success()) {
                throw new BlockchainConflictException(response.message());
            }
            if (response.block() == null) {
                throw new BlockchainUnavailableException(
                        node.name() + " aceptó la solicitud sin devolver el bloque.",
                        new IOException("Respuesta incompleta del nodo."));
            }
            return response.block();
        } catch (IOException exception) {
            throw new BlockchainUnavailableException(
                    "No se pudo enviar el bloque a " + node.name() + ".", exception);
        }
    }

    public List<NodeStatus> getNodesStatus() {
        return nodes.entrySet().stream()
                .map(entry -> getNodeStatus(entry.getKey(), entry.getValue()))
                .toList();
    }

    public String getPrimaryNodeId() {
        return primaryNodeId;
    }

    private NodeStatus getNodeStatus(String id, NodeConnection node) {
        try {
            List<BlockData> blocks = node.client().getChain();
            boolean valid = validateChain(blocks);
            String lastHash = blocks.isEmpty() ? null : blocks.get(blocks.size() - 1).hash();
            return new NodeStatus(id, node.name(), node.host(), node.port(), true, blocks.size(),
                    lastHash, valid, valid ? "Conectado; cadena válida." : "La cadena no supera la validación.");
        } catch (IOException exception) {
            return new NodeStatus(id, node.name(), node.host(), node.port(), false, 0,
                    null, false, "No se pudo consultar el nodo: " + exception.getMessage());
        }
    }

    private static boolean validateChain(List<BlockData> blocks) {
        if (blocks.isEmpty()) {
            return false;
        }
        BlockChain chain = new BlockChain(3, "0");
        try {
            for (BlockData block : blocks) {
                chain.addMinedBlock(block.toBlock());
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return chain.validateChain();
    }

    private NodeConnection findNode(String nodeId) {
        String requestedId = nodeId == null || nodeId.isBlank() ? primaryNodeId : nodeId;
        NodeConnection node = nodes.get(requestedId);
        if (node == null) {
            throw new IllegalArgumentException("Nodo desconocido. Usa node1 o node2.");
        }
        return node;
    }

    private record NodeConnection(String name, String host, int port, TlsBlockchainClient client) {
    }
}
