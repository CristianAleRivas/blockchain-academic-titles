package sv.edu.blockchain.model;

import java.util.ArrayList;
import java.util.List;

import sv.edu.blockchain.service.HashService;

public class Block {
    private final int id;
    private final long timestamp;
    private final String previousHash;
    private String hash;
    private long nonce;
    private final List<AcademicTitleTransaction> transactions = new ArrayList<>();
    private boolean sealed;

    public Block(int id, long timestamp, String previousHash) {
        this(id, timestamp, previousHash, new ArrayList<>());
    }

    public Block(int id, long timestamp, String previousHash, List<AcademicTitleTransaction> transactions) {
        this(id, timestamp, previousHash, null, transactions);
    }

    public Block(int id, long timestamp, String previousHash, String hash, List<AcademicTitleTransaction> transactions) {
        this.id = id;
        this.timestamp = timestamp;
        this.previousHash = previousHash;
        this.hash = hash;
        this.transactions.addAll(transactions == null ? new ArrayList<>() : transactions);
    }

    public int getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }

    public long getNonce() {
        return nonce;
    }

    public List<AcademicTitleTransaction> getTransactions() {
        return transactions;
    }

    public boolean isSealed() {
        return sealed;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public void addTransaction(AcademicTitleTransaction transaction) {
        if (sealed) {
            throw new IllegalStateException("No se puede modificar un bloque ya minado.");
        }
        if (transaction == null) {
            throw new IllegalArgumentException("La transacción no puede ser nula.");
        }
        if (!transaction.validate()) {
            throw new IllegalArgumentException("La transacción no es válida.");
        }
        transactions.add(transaction);
    }

    public void seal() {
        this.sealed = true;
    }

    public static Block restore(int id, long timestamp, String previousHash, String hash,
                                long nonce, List<AcademicTitleTransaction> transactions) {
        Block block = new Block(id, timestamp, previousHash, hash, transactions);
        block.nonce = nonce;
        block.sealed = true;
        return block;
    }

    public void mine(int difficulty, String prefix) {
        if (sealed) {
            throw new IllegalStateException("El bloque ya está minado y sellado.");
        }
        String targetPrefix = prefix.repeat(Math.max(1, difficulty));
        long attempt = 0;
        HashService hashService = new HashService();
        while (true) {
            this.nonce = attempt;
            String candidate = hashService.hash(generateDeterministicRepresentation() + nonce);
            if (candidate.startsWith(targetPrefix)) {
                this.hash = candidate;
                this.sealed = true;
                return;
            }
            attempt++;
        }
    }

    public String generateDeterministicRepresentation() {
        StringBuilder builder = new StringBuilder();
        builder.append("id=").append(id)
                .append("|timestamp=").append(timestamp)
                .append("|previousHash=").append(previousHash)
                .append("|transactions=");
        for (AcademicTitleTransaction tx : transactions) {
            builder.append(tx.serializeDeterministic()).append(";");
        }
        return builder.toString();
    }

    public boolean matchesDifficulty(int difficulty, String prefix) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        String target = prefix.repeat(Math.max(1, difficulty));
        return hash.startsWith(target);
    }

    public boolean validateHash() {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        String computed = new HashService().hash(generateDeterministicRepresentation() + nonce);
        return hash.equals(computed);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Block{id=").append(id)
                .append(", timestamp=").append(timestamp)
                .append(", txCount=").append(transactions.size())
                .append(", previousHash='").append(previousHash)
                .append("', nonce=").append(nonce)
                .append(", hash='").append(hash)
                .append("'}");
        if (transactions.isEmpty()) {
            builder.append("\n  Transacciones: ninguna");
        } else {
            builder.append("\n  Transacciones:");
            for (AcademicTitleTransaction tx : transactions) {
                builder.append("\n    - ").append(tx);
            }
        }
        return builder.toString();
    }
}
