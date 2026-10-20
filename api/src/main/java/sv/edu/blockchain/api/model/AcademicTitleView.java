package sv.edu.blockchain.api.model;

import java.time.LocalDate;

public record AcademicTitleView(String transactionId, String academicProgram, String degree,
                                String institutionName, LocalDate graduationDate,
                                LocalDate emissionDate, String issuerSignature) {
}
