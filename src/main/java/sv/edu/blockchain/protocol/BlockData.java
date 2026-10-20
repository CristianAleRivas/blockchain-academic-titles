package sv.edu.blockchain.protocol;

import java.time.LocalDate;
import java.util.List;

import sv.edu.blockchain.model.AcademicTitleTransaction;
import sv.edu.blockchain.model.Block;

public record BlockData(int id, long timestamp, String previousHash, String hash, long nonce,
                        List<TransactionData> transactions) {
    public static BlockData from(Block block) {
        List<TransactionData> transactions = block.getTransactions().stream()
                .map(TransactionData::from)
                .toList();
        return new BlockData(block.getId(), block.getTimestamp(), block.getPreviousHash(),
                block.getHash(), block.getNonce(), transactions);
    }

    public Block toBlock() {
        List<AcademicTitleTransaction> restoredTransactions = transactions.stream()
                .map(TransactionData::toTransaction)
                .toList();
        return Block.restore(id, timestamp, previousHash, hash, nonce, restoredTransactions);
    }

    public record TransactionData(String transactionId, String encryptedFullName, String encryptedDui,
                                  String academicProgram, String degree, String institutionName,
                                  LocalDate graduationDate, LocalDate emissionDate, String issuerSignature) {
        private static TransactionData from(AcademicTitleTransaction transaction) {
            return new TransactionData(transaction.getTransactionId(), transaction.getEncryptedFullName(),
                    transaction.getEncryptedDui(), transaction.getAcademicProgram(), transaction.getDegree(),
                    transaction.getInstitutionName(), transaction.getGraduationDate(),
                    transaction.getEmissionDate(), transaction.getIssuerSignature());
        }

        private AcademicTitleTransaction toTransaction() {
            return AcademicTitleTransaction.fromEncrypted(transactionId, encryptedFullName, encryptedDui,
                    academicProgram, degree, institutionName, graduationDate, emissionDate, issuerSignature);
        }
    }
}
