package sv.edu.blockchain.model;

import java.time.LocalDate;
import java.util.Objects;

import sv.edu.blockchain.crypto.CryptoService;

public class AcademicTitleTransaction {
    private static final CryptoService CRYPTO_SERVICE = new CryptoService("blockchain-academic-titles-simulated-key-2026");

    private String transactionId;
    private String encryptedFullName;
    private String encryptedDui;
    private String academicProgram;
    private String degree;
    private String institutionName;
    private LocalDate graduationDate;
    private LocalDate emissionDate;
    private String issuerSignature;

    public AcademicTitleTransaction(String transactionId,
                                   String fullName,
                                   String dui,
                                   String academicProgram,
                                   String degree,
                                   String institutionName,
                                   LocalDate graduationDate,
                                   LocalDate emissionDate,
                                   String issuerSignature) {
        this.transactionId = requireText(transactionId, "ID de transacción");
        this.encryptedFullName = CRYPTO_SERVICE.encrypt(requireText(fullName, "Nombre completo"));
        this.encryptedDui = CRYPTO_SERVICE.encrypt(requireText(dui, "DUI"));
        this.academicProgram = requireText(academicProgram, "Carrera o programa académico");
        this.degree = requireText(degree, "Grado obtenido");
        this.institutionName = requireText(institutionName, "Universidad o institución emisora");
        this.graduationDate = Objects.requireNonNull(graduationDate, "Fecha de graduación");
        this.emissionDate = Objects.requireNonNull(emissionDate, "Fecha de emisión");
        this.issuerSignature = requireText(issuerSignature, "Firma o identificador de la institución");
    }

    public static AcademicTitleTransaction fromEncrypted(String transactionId,
                                                          String encryptedFullName,
                                                          String encryptedDui,
                                                          String academicProgram,
                                                          String degree,
                                                          String institutionName,
                                                          LocalDate graduationDate,
                                                          LocalDate emissionDate,
                                                          String issuerSignature) {
        AcademicTitleTransaction transaction = new AcademicTitleTransaction();
        transaction.transactionId = requireText(transactionId, "ID de transacción");
        transaction.encryptedFullName = requireText(encryptedFullName, "Nombre cifrado");
        transaction.encryptedDui = requireText(encryptedDui, "DUI cifrado");
        transaction.academicProgram = requireText(academicProgram, "Carrera o programa académico");
        transaction.degree = requireText(degree, "Grado obtenido");
        transaction.institutionName = requireText(institutionName, "Universidad o institución emisora");
        transaction.graduationDate = Objects.requireNonNull(graduationDate, "Fecha de graduación");
        transaction.emissionDate = Objects.requireNonNull(emissionDate, "Fecha de emisión");
        transaction.issuerSignature = requireText(issuerSignature, "Firma o identificador de la institución");
        return transaction;
    }

    private AcademicTitleTransaction() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getEncryptedFullName() {
        return encryptedFullName;
    }

    public String getFullName() {
        return CRYPTO_SERVICE.decrypt(encryptedFullName);
    }

    public String getEncryptedDui() {
        return encryptedDui;
    }

    public String getDui() {
        return CRYPTO_SERVICE.decrypt(encryptedDui);
    }

    public String getAcademicProgram() {
        return academicProgram;
    }

    public String getDegree() {
        return degree;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = requireText(institutionName, "Universidad o institución emisora");
    }

    public LocalDate getGraduationDate() {
        return graduationDate;
    }

    public LocalDate getEmissionDate() {
        return emissionDate;
    }

    public String getIssuerSignature() {
        return issuerSignature;
    }

    public boolean validate() {
        try {
            requireText(transactionId, "ID de transacción");
            requireText(encryptedFullName, "Nombre cifrado");
            requireText(encryptedDui, "DUI cifrado");
            requireText(academicProgram, "Carrera o programa académico");
            requireText(degree, "Grado obtenido");
            requireText(institutionName, "Universidad o institución emisora");
            Objects.requireNonNull(graduationDate, "Fecha de graduación");
            Objects.requireNonNull(emissionDate, "Fecha de emisión");
            requireText(issuerSignature, "Firma o identificador de la institución");
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    public String serializeDeterministic() {
        return transactionId + "|"
                + encryptedFullName + "|"
                + encryptedDui + "|"
                + academicProgram + "|"
                + degree + "|"
                + institutionName + "|"
                + graduationDate + "|"
                + emissionDate + "|"
                + issuerSignature;
    }

    @Override
    public String toString() {
        return "Transaction{id='" + transactionId + "', program='" + academicProgram + "', degree='" + degree + "', university='" + institutionName + "', graduationDate=" + graduationDate + ", issuanceDate=" + emissionDate + ", issuer='" + issuerSignature + "', name=[cifrado], dui=[cifrado]}";
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo " + fieldName + " es obligatorio.");
        }
        return value.trim();
    }
}
