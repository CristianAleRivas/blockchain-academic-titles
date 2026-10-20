package sv.edu.blockchain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sv.edu.blockchain.model.AcademicTitleTransaction;
import sv.edu.blockchain.model.Block;
import sv.edu.blockchain.service.HashService;

public class BlockChain {
    private final List<Block> blocks = new ArrayList<>();
    private final int difficulty;
    private final String prefix;
    private Block proposedBlock;

    public BlockChain(int difficulty, String prefix) {
        if (difficulty <= 0) {
            throw new IllegalArgumentException("La dificultad debe ser mayor que cero.");
        }
        this.difficulty = difficulty;
        this.prefix = prefix == null || prefix.isBlank() ? "0" : prefix;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public String getPrefix() {
        return prefix;
    }

    public synchronized void createGenesis() {
        if (!blocks.isEmpty()) {
            throw new IllegalStateException("El bloque Génesis ya existe.");
        }
        Block genesis = new Block(0, System.currentTimeMillis(), "0".repeat(64));
        genesis.mine(difficulty, prefix);
        blocks.add(genesis);
    }

    public synchronized Block getGenesis() {
        if (blocks.isEmpty()) {
            return null;
        }
        return blocks.get(0);
    }

    public synchronized List<Block> getBlocks() {
        return List.copyOf(blocks);
    }

    public synchronized Block getProposedBlock() {
        return proposedBlock;
    }

    public synchronized Block createProposedBlock() {
        if (blocks.isEmpty()) {
            throw new IllegalStateException("Debe crear y minar el bloque Génesis antes de crear un bloque propuesto.");
        }
        if (proposedBlock != null && !proposedBlock.isSealed()) {
            throw new IllegalStateException("Ya existe un bloque propuesto no minado. Minarlo antes de crear otro.");
        }
        Block lastBlock = getLastBlock();
        Block nextBlock = new Block(lastBlock.getId() + 1, System.currentTimeMillis(), lastBlock.getHash());
        proposedBlock = nextBlock;
        return nextBlock;
    }

    public synchronized void addTransactionToLastBlock(String transactionId,
                                         String fullName,
                                         String dui,
                                         String academicProgram,
                                         String degree,
                                         String institutionName,
                                         LocalDate graduationDate,
                                         LocalDate emissionDate,
                                         String issuerSignature) {
        if (proposedBlock == null) {
            throw new IllegalStateException("No hay un bloque propuesto activo.");
        }
        AcademicTitleTransaction transaction = new AcademicTitleTransaction(
                transactionId,
                fullName,
                dui,
                academicProgram,
                degree,
                institutionName,
                graduationDate,
                emissionDate,
                issuerSignature
        );
        proposedBlock.addTransaction(transaction);
    }

    public synchronized void mineLastBlock() {
        if (proposedBlock == null) {
            throw new IllegalStateException("No hay un bloque propuesto para minar.");
        }
        if (proposedBlock.getTransactions().isEmpty()) {
            throw new IllegalStateException("El bloque propuesto debe contener al menos una transacción antes de minar.");
        }
        proposedBlock.mine(difficulty, prefix);
        blocks.add(proposedBlock);
        proposedBlock = null;
    }

    public synchronized void addMinedBlock(Block block) {
        if (block == null) {
            throw new IllegalArgumentException("El bloque no puede ser nulo.");
        }
        if (blocks.isEmpty() && block.getId() != 0) {
            throw new IllegalArgumentException("El primer bloque debe ser el Génesis.");
        }
        if (blocks.isEmpty() && !"0".repeat(64).equals(block.getPreviousHash())) {
            throw new IllegalArgumentException("El previousHash del Génesis no es válido.");
        }
        if (blocks.size() > 0) {
            Block lastBlock = getLastBlock();
            if (block.getPreviousHash() == null || !block.getPreviousHash().equals(lastBlock.getHash())) {
                throw new IllegalArgumentException("El previousHash del bloque no coincide con el último bloque de la cadena.");
            }
            if (block.getId() != lastBlock.getId() + 1) {
                throw new IllegalArgumentException("El ID del bloque no es consecutivo.");
            }
        }
        if (block.getHash() == null || block.getHash().isBlank()) {
            throw new IllegalArgumentException("El bloque no tiene hash.");
        }
        if (!block.matchesDifficulty(difficulty, prefix)) {
            throw new IllegalArgumentException("El bloque no cumple la prueba de trabajo.");
        }
        if (!block.validateHash()) {
            throw new IllegalArgumentException("El hash del bloque es incorrecto.");
        }
        if (!block.isSealed()) {
            throw new IllegalArgumentException("El bloque debe estar sellado antes de agregarse.");
        }
        if (block.getId() == 0) {
            if (!block.getTransactions().isEmpty()) {
                throw new IllegalArgumentException("El bloque Génesis no debe contener transacciones.");
            }
        } else {
            Set<String> seenTransactionIds = new HashSet<>();
            for (Block existing : blocks) {
                for (AcademicTitleTransaction transaction : existing.getTransactions()) {
                    seenTransactionIds.add(transaction.getTransactionId());
                }
            }
            for (AcademicTitleTransaction tx : block.getTransactions()) {
                if (tx == null || !tx.validate()) {
                    throw new IllegalArgumentException("El bloque contiene transacciones inválidas.");
                }
                if (!seenTransactionIds.add(tx.getTransactionId())) {
                    throw new IllegalArgumentException("El bloque contiene un ID de transacción duplicado.");
                }
            }
        }
        for (Block existing : blocks) {
            if (existing.getId() == block.getId()) {
                throw new IllegalArgumentException("El bloque ya existe en la cadena.");
            }
        }
        blocks.add(block);
    }

    public synchronized boolean considerChain(List<Block> candidateBlocks) {
        if (candidateBlocks == null || candidateBlocks.isEmpty()) {
            return false;
        }
        BlockChain candidate = new BlockChain(difficulty, prefix);
        try {
            for (Block block : candidateBlocks) {
                candidate.addMinedBlock(block);
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (!candidate.validateChain()) {
            return false;
        }
        boolean longer = candidate.blocks.size() > blocks.size();
        boolean deterministicTieBreak = candidate.blocks.size() == blocks.size()
                && !blocks.isEmpty()
                && candidate.getLastBlock().getHash().compareTo(getLastBlock().getHash()) < 0;
        if (!longer && !deterministicTieBreak) {
            return false;
        }
        blocks.clear();
        blocks.addAll(candidate.blocks);
        proposedBlock = null;
        return true;
    }

    public synchronized boolean validateChain() {
        if (blocks.isEmpty()) {
            return false;
        }
        Set<String> seenTransactionIds = new HashSet<>();
        HashService hashService = new HashService();

        for (int i = 0; i < blocks.size(); i++) {
            Block current = blocks.get(i);
            if (current == null) {
                return false;
            }
            if (current.getHash() == null || current.getHash().isBlank()) {
                return false;
            }
            if (!current.matchesDifficulty(difficulty, prefix)) {
                return false;
            }
            String expectedHash = hashService.hash(current.generateDeterministicRepresentation() + current.getNonce());
            if (!current.getHash().equals(expectedHash)) {
                return false;
            }
            if (i == 0) {
                if (current.getId() != 0) {
                    return false;
                }
                if (!"0".repeat(64).equals(current.getPreviousHash())) {
                    return false;
                }
                if (!current.getTransactions().isEmpty()) {
                    return false;
                }
            } else {
                Block previous = blocks.get(i - 1);
                if (current.getId() != previous.getId() + 1) {
                    return false;
                }
                if (!current.getPreviousHash().equals(previous.getHash())) {
                    return false;
                }
                if (current.getTransactions().isEmpty()) {
                    return false;
                }
            }
            for (AcademicTitleTransaction tx : current.getTransactions()) {
                if (tx == null || !tx.validate()) {
                    return false;
                }
                if (!seenTransactionIds.add(tx.getTransactionId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public synchronized Block getBlock(int index) {
        if (index < 0 || index >= blocks.size()) {
            return null;
        }
        return blocks.get(index);
    }

    public synchronized Block getLastBlock() {
        if (blocks.isEmpty()) {
            return null;
        }
        return blocks.get(blocks.size() - 1);
    }

    public int size() {
        return blocks.size();
    }

    public void printChainSummary() {
        System.out.println("\n=== RESUMEN DE LA CADENA ===");
        for (Block block : blocks) {
            System.out.println("Bloque ID=" + block.getId()
                    + " | timestamp=" + block.getTimestamp()
                    + " | transacciones=" + block.getTransactions().size()
                    + " | previousHash=" + block.getPreviousHash()
                    + " | nonce=" + block.getNonce()
                    + " | hash=" + block.getHash());
        }
    }
}
