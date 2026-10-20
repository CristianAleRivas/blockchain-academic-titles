package sv.edu.blockchain.api.model;

import java.time.LocalDate;

public record CreateTitleRequest(String transactionId, String fullName, String dui,
                                 String academicProgram, String degree, String institutionName,
                                 LocalDate graduationDate, LocalDate emissionDate, String issuerSignature) {
}
