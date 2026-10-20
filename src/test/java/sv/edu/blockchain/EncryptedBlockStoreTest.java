package sv.edu.blockchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sv.edu.blockchain.model.Block;
import sv.edu.blockchain.protocol.BlockData;
import sv.edu.blockchain.protocol.BlockchainJson;
import sv.edu.blockchain.service.EncryptedBlockStore;

class EncryptedBlockStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void storesEncryptedJsonNamedByHashAndRestoresValidBlock() throws Exception {
        BlockChain chain = new BlockChain(1, "0");
        chain.createGenesis();
        Block proposed = chain.createProposedBlock();
        chain.addTransactionToLastBlock("tx-1", "Ana Lopez", "01234567-1", "Ingenieria",
                "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1),
                LocalDate.of(2024, 1, 15), "UES-REG-001");
        chain.mineLastBlock();

        EncryptedBlockStore store = new EncryptedBlockStore(temporaryDirectory, "test-storage-key");
        store.saveChain(chain.getBlocks());

        Path blockFile = temporaryDirectory.resolve(proposed.getHash() + ".json");
        String encryptedJson = Files.readString(blockFile);
        assertFalse(encryptedJson.contains("Ana Lopez"));
        assertFalse(encryptedJson.contains("Ingenieria"));

        BlockChain restored = new BlockChain(1, "0");
        assertTrue(restored.considerChain(store.load()));
        assertTrue(restored.validateChain());
        assertEquals(proposed.getHash(), restored.getBlock(1).getHash());

        String transportJson = BlockchainJson.MAPPER.writeValueAsString(BlockData.from(proposed));
        BlockData transported = BlockchainJson.MAPPER.readValue(transportJson, BlockData.class);
        assertEquals(proposed.getHash(), transported.toBlock().getHash());
        assertTrue(transported.toBlock().validateHash());
    }

    @Test
    void nodesConvergeOnTheLongestValidChainAndBreakTiesDeterministically() {
        BlockChain firstNode = new BlockChain(1, "0");
        BlockChain secondNode = new BlockChain(1, "0");
        firstNode.createGenesis();
        secondNode.createGenesis();
        var firstChain = firstNode.getBlocks();
        var secondChain = secondNode.getBlocks();

        firstNode.considerChain(secondChain);
        secondNode.considerChain(firstChain);
        assertEquals(firstNode.getLastBlock().getHash(), secondNode.getLastBlock().getHash());

        BlockChain longerNode = new BlockChain(1, "0");
        longerNode.considerChain(firstNode.getBlocks());
        longerNode.createProposedBlock();
        longerNode.addTransactionToLastBlock("tx-longest", "Ana Lopez", "01234567-1", "Ingenieria",
                "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1),
                LocalDate.of(2024, 1, 15), "UES-REG-001");
        longerNode.mineLastBlock();
        assertTrue(firstNode.considerChain(longerNode.getBlocks()));
        assertEquals(2, firstNode.getBlocks().size());
    }
}
