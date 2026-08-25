package sv.edu.blockchain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import sv.edu.blockchain.crypto.CryptoService;
import sv.edu.blockchain.model.AcademicTitleTransaction;
import sv.edu.blockchain.model.Block;

public class BlockchainTest {

    @Test
    void shouldCreateAndMineGenesis() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();

        assertNotNull(chain.getBlock(0));
        assertEquals(0, chain.getBlock(0).getId());
        assertEquals("000", chain.getBlock(0).getHash().substring(0, 3));
        assertTrue(chain.getBlock(0).getTransactions().isEmpty());
    }

    @Test
    void genesisShouldHaveNoTransactions() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();

        assertTrue(chain.getGenesis().getTransactions().isEmpty());
    }

    @Test
    void shouldCreateProposedBlock() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();
        Block block = chain.createProposedBlock();

        assertNotNull(block);
        assertEquals(1, block.getId());
        assertTrue(block.getTransactions().isEmpty());
    }

    @Test
    void shouldAddMultipleTransactions() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();
        Block block = chain.createProposedBlock();

        block.addTransaction(new AcademicTitleTransaction("tx-1", "Ana Lopez", "01234567-1", "Ingenieria", "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1), LocalDate.of(2024, 1, 15), "UES-REG-001"));
        block.addTransaction(new AcademicTitleTransaction("tx-2", "Carlos Perez", "12345678-9", "Derecho", "Maestria", "Universidad Centroamericana José Simeón Cañas", LocalDate.of(2021, 8, 10), LocalDate.of(2024, 2, 20), "UCA-REG-777"));

        assertEquals(2, block.getTransactions().size());
    }

    @Test
    void shouldHashMeetDifficulty() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();
        Block block = chain.createProposedBlock();
        block.addTransaction(new AcademicTitleTransaction("tx-1", "Ana Lopez", "01234567-1", "Ingenieria", "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1), LocalDate.of(2024, 1, 15), "UES-REG-001"));

        block.mine(3, "0");
        assertTrue(block.getHash().startsWith("000"));
    }

    @Test
    void shouldChainByPreviousHash() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();
        Block first = chain.createProposedBlock();
        first.addTransaction(new AcademicTitleTransaction("tx-1", "Ana Lopez", "01234567-1", "Ingenieria", "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1), LocalDate.of(2024, 1, 15), "UES-REG-001"));
        chain.mineLastBlock();

        Block second = chain.createProposedBlock();
        assertEquals(chain.getLastBlock().getHash(), second.getPreviousHash());
    }

    @Test
    void shouldRejectIncorrectPreviousHash() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();

        Block block = new Block(1, 0L, "bad-hash", "", new java.util.ArrayList<>());
        assertThrows(IllegalArgumentException.class, () -> chain.addMinedBlock(block));
    }

    @Test
    void shouldRejectIncorrectHash() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();
        Block block = chain.createProposedBlock();
        block.addTransaction(new AcademicTitleTransaction("tx-1", "Ana Lopez", "01234567-1", "Ingenieria", "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1), LocalDate.of(2024, 1, 15), "UES-REG-001"));
        block.setHash("invalidhash");
        assertThrows(IllegalArgumentException.class, () -> chain.addMinedBlock(block));
    }

    @Test
    void shouldValidateCorrectChain() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();
        chain.createProposedBlock();
        chain.addTransactionToLastBlock("tx-1", "Ana Lopez", "01234567-1", "Ingenieria", "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1), LocalDate.of(2024, 1, 15), "UES-REG-001");
        chain.mineLastBlock();

        assertTrue(chain.validateChain());
    }

    @Test
    void shouldDetectModification() {
        BlockChain chain = new BlockChain(3, "0");
        chain.createGenesis();
        chain.createProposedBlock();
        chain.addTransactionToLastBlock("tx-1", "Ana Lopez", "01234567-1", "Ingenieria", "Licenciatura", "Universidad de El Salvador", LocalDate.of(2020, 6, 1), LocalDate.of(2024, 1, 15), "UES-REG-001");
        chain.mineLastBlock();

        Block tampered = chain.getBlock(1);
        tampered.getTransactions().get(0).setInstitutionName("Universidad modificada");

        assertFalse(chain.validateChain());
    }

    @Test
    void shouldEncryptAndDecryptNameAndDui() {
        CryptoService crypto = new CryptoService("supersecretkey1234567890");
        String originalName = "Ana Lopez";
        String originalDui = "01234567-1";

        String encryptedName = crypto.encrypt(originalName);
        String encryptedDui = crypto.encrypt(originalDui);

        assertNotEquals(originalName, encryptedName);
        assertNotEquals(originalDui, encryptedDui);
        assertEquals(originalName, crypto.decrypt(encryptedName));
        assertEquals(originalDui, crypto.decrypt(encryptedDui));
    }

    @Test
    void shouldGenerateUniqueCiphertextForSameText() {
        CryptoService crypto = new CryptoService("supersecretkey1234567890");
        String value = "Ana Lopez";

        String encryptedOne = crypto.encrypt(value);
        String encryptedTwo = crypto.encrypt(value);

        assertNotEquals(encryptedOne, encryptedTwo);
    }
}
