package sv.edu.blockchain.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import sv.edu.blockchain.crypto.CryptoService;
import sv.edu.blockchain.model.Block;
import sv.edu.blockchain.protocol.BlockData;
import sv.edu.blockchain.protocol.BlockchainJson;

public class EncryptedBlockStore {
    private final Path directory;
    private final CryptoService crypto;

    public EncryptedBlockStore(Path directory, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Configure la variable BLOCKCHAIN_STORAGE_KEY.");
        }
        this.directory = directory;
        this.crypto = new CryptoService(secret);
    }

    public List<Block> load() throws IOException {
        Files.createDirectories(directory);
        List<Block> blocks = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                JsonNode envelope = BlockchainJson.MAPPER.readTree(file.toFile());
                String ciphertext = requiredText(envelope, "payload", file);
                String json = crypto.decrypt(ciphertext);
                BlockData block = BlockchainJson.MAPPER.readValue(json, BlockData.class);
                if (!requiredText(envelope, "hash", file).equals(block.hash())
                        || !file.getFileName().toString().equals(block.hash() + ".json")) {
                    throw new IOException("El nombre del archivo no coincide con el hash del bloque: " + file);
                }
                blocks.add(block.toBlock());
            }
        }
        blocks.sort((left, right) -> Integer.compare(left.getId(), right.getId()));
        return blocks;
    }

    public synchronized void saveChain(List<Block> blocks) throws IOException {
        Files.createDirectories(directory);
        Set<String> expectedFiles = new HashSet<>();
        for (Block block : blocks) {
            String filename = block.getHash() + ".json";
            expectedFiles.add(filename);
            String payload = crypto.encrypt(BlockchainJson.MAPPER.writeValueAsString(BlockData.from(block)));
            ObjectNode envelope = BlockchainJson.MAPPER.createObjectNode();
            envelope.put("hash", block.getHash());
            envelope.put("payload", payload);
            Path destination = directory.resolve(filename);
            Path temporary = Files.createTempFile(directory, "block-", ".tmp");
            try {
                BlockchainJson.MAPPER.writeValue(temporary.toFile(), envelope);
                try {
                    Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException atomicMoveFailure) {
                    Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                if (!expectedFiles.contains(file.getFileName().toString())) {
                    Files.delete(file);
                }
            }
        }
    }

    private static String requiredText(JsonNode json, String field, Path file) throws IOException {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IOException("Archivo de bloque cifrado inválido: " + file);
        }
        return value.asText();
    }
}
