package sv.edu.blockchain;

import java.time.LocalDate;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import sv.edu.blockchain.model.AcademicTitleTransaction;
import sv.edu.blockchain.model.Block;

public class Main {
    private static final int DIFFICULTY = 3;
    private static final String PREFIX = "0";
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        BlockChain blockchain = new BlockChain(DIFFICULTY, PREFIX);
        System.out.println("=== BLOCKCHAIN DE TÍTULOS ACADÉMICOS ===");
        System.out.println("Configuración actual: dificultad " + DIFFICULTY + ", prefijo " + PREFIX.repeat(DIFFICULTY));
        System.out.println("Simulación académica: no es un registro oficial del MINED ni una validación legal.");

        boolean running = true;
        while (running) {
            showMenu();
            int option;
            try {
                option = SCANNER.nextInt();
                SCANNER.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Entrada inválida. Debe ingresar un número.");
                SCANNER.nextLine();
                continue;
            }

            try {
                switch (option) {
                    case 1 -> createGenesis(blockchain);
                    case 2 -> createProposedBlock(blockchain);
                    case 3 -> registerAcademicTitle(blockchain);
                    case 4 -> showProposedBlock(blockchain);
                    case 5 -> mineLastBlock(blockchain);
                    case 6 -> blockchain.printChainSummary();
                    case 7 -> validateBlockchain(blockchain);
                    case 8 -> searchTitle(blockchain);
                    case 9 -> demonstrateFraudAndDetection(blockchain);
                    case 0 -> {
                        System.out.println("Gracias por usar la simulación blockchain.");
                        running = false;
                    }
                    default -> System.out.println("Opción no válida.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        SCANNER.close();
    }

    private static void showMenu() {
        System.out.println();
        System.out.println("1. Crear y minar bloque Génesis");
        System.out.println("2. Crear nuevo bloque propuesto");
        System.out.println("3. Registrar título académico");
        System.out.println("4. Mostrar bloque propuesto");
        System.out.println("5. Minar último bloque");
        System.out.println("6. Mostrar resumen de la blockchain");
        System.out.println("7. Validar blockchain");
        System.out.println("8. Buscar título por hash o identificador");
        System.out.println("9. Demostrar alteración y detectar fraude");
        System.out.println("0. Salir");
        System.out.print("Seleccione una opción: ");
    }

    private static void createGenesis(BlockChain blockchain) {
        if (blockchain.getGenesis() != null) {
            System.out.println("El bloque Génesis ya existe.");
            return;
        }
        blockchain.createGenesis();
        System.out.println("Bloque Génesis creado y minado correctamente.");
    }

    private static void createProposedBlock(BlockChain blockchain) {
        if (blockchain.getGenesis() == null) {
            System.out.println("Debe crear el bloque Génesis primero.");
            return;
        }
        if (blockchain.getProposedBlock() != null) {
            System.out.println("Ya existe un bloque propuesto activo. Debe minarlo antes de crear otro.");
            return;
        }
        Block block = blockchain.createProposedBlock();
        System.out.println("Bloque propuesto creado con ID " + block.getId());
    }

    private static void registerAcademicTitle(BlockChain blockchain) {
        if (blockchain.getGenesis() == null) {
            System.out.println("Debe crear el bloque Génesis primero.");
            return;
        }
        if (blockchain.getProposedBlock() == null) {
            blockchain.createProposedBlock();
        }

        System.out.print("ID de la transacción: ");
        String txId = SCANNER.nextLine();
        System.out.print("Nombre completo del ciudadano: ");
        String fullName = SCANNER.nextLine();
        System.out.print("DUI del ciudadano: ");
        String dui = SCANNER.nextLine();
        System.out.print("Carrera o programa académico: ");
        String program = SCANNER.nextLine();
        System.out.print("Grado obtenido: ");
        String degree = SCANNER.nextLine();
        System.out.print("Universidad o institución emisora: ");
        String institution = SCANNER.nextLine();
        System.out.print("Fecha de graduación (yyyy-MM-dd): ");
        LocalDate graduationDate = LocalDate.parse(SCANNER.nextLine());
        System.out.print("Fecha de emisión (yyyy-MM-dd): ");
        LocalDate emissionDate = LocalDate.parse(SCANNER.nextLine());
        System.out.print("Firma o identificador de la institución: ");
        String issuerSignature = SCANNER.nextLine();

        blockchain.addTransactionToLastBlock(txId, fullName, dui, program, degree, institution, graduationDate, emissionDate, issuerSignature);
        System.out.println("Transacción registrada. Total en bloque propuesto: " + blockchain.getProposedBlock().getTransactions().size());
    }

    private static void showProposedBlock(BlockChain blockchain) {
        Block proposed = blockchain.getProposedBlock();
        if (proposed == null) {
            if (blockchain.getLastBlock() == null) {
                System.out.println("No hay bloques en la cadena.");
                return;
            }
            proposed = blockchain.getLastBlock();
        }
        System.out.println(proposed);
    }

    private static void mineLastBlock(BlockChain blockchain) {
        if (blockchain.getGenesis() == null) {
            System.out.println("Debe crear el bloque Génesis primero.");
            return;
        }
        if (blockchain.getProposedBlock() == null) {
            System.out.println("No hay un bloque propuesto. Cree uno primero.");
            return;
        }
        Block proposed = blockchain.getProposedBlock();
        long start = System.currentTimeMillis();
        blockchain.mineLastBlock();
        long elapsed = System.currentTimeMillis() - start;
        Block block = blockchain.getLastBlock();
        System.out.println("Minado del bloque: id=" + block.getId());
        System.out.println("Cantidad de transacciones: " + block.getTransactions().size());
        System.out.println("Nonce encontrado: " + block.getNonce());
        System.out.println("Hash resultante: " + block.getHash());
        System.out.println("Tiempo aproximado de minado: " + elapsed + " ms");
        System.out.println("Bloque agregado a la blockchain.");
    }

    private static void validateBlockchain(BlockChain blockchain) {
        boolean valid = blockchain.validateChain();
        if (valid) {
            System.out.println("La blockchain es válida y no se detectaron alteraciones.");
        } else {
            System.out.println("La blockchain está comprometida. Se detectó una alteración o inconsistencia.");
        }
    }

    private static void searchTitle(BlockChain blockchain) {
        System.out.println("Funcionalidad de búsqueda por identificador o hash de verificación (simulada).");
        System.out.print("Ingrese identificador o hash: ");
        String query = SCANNER.nextLine();

        for (Block block : blockchain.getBlocks()) {
            for (AcademicTitleTransaction tx : block.getTransactions()) {
                if (tx.getTransactionId().equalsIgnoreCase(query) || block.getHash().equalsIgnoreCase(query)) {
                    System.out.println("Título encontrado:");
                    System.out.println("- ID: " + tx.getTransactionId());
                    System.out.println("- Programa: " + tx.getAcademicProgram());
                    System.out.println("- Grado: " + tx.getDegree());
                    System.out.println("- Universidad: " + tx.getInstitutionName());
                    System.out.println("- Fecha de graduación: " + tx.getGraduationDate());
                    System.out.println("- Hash de verificación: " + block.getHash());
                    return;
                }
            }
        }
        System.out.println("No se encontró ningún título con ese identificador o hash.");
    }

    private static void demonstrateFraudAndDetection(BlockChain blockchain) {
        System.out.println("Demostración de alteración: se modifica un campo de una transacción y luego se valida la cadena.");
        if (blockchain.getGenesis() == null) {
            blockchain.createGenesis();
        }
        if (blockchain.getProposedBlock() == null) {
            blockchain.createProposedBlock();
        }

        blockchain.addTransactionToLastBlock("tx-demo-1", "Ana López", "01234567-1", "Ingeniería en Sistemas", "Ingeniera", "Universidad de El Salvador", LocalDate.of(2020, 5, 15), LocalDate.of(2024, 1, 10), "UES-REG-100");
        blockchain.addTransactionToLastBlock("tx-demo-2", "Carlos Hernández", "12345678-9", "Derecho", "Abogado", "Universidad Centroamericana", LocalDate.of(2021, 7, 20), LocalDate.of(2024, 2, 12), "UCA-REG-200");
        blockchain.addTransactionToLastBlock("tx-demo-3", "María Flores", "98765432-1", "Administración", "Licenciada", "Universidad Tecnológica", LocalDate.of(2019, 9, 30), LocalDate.of(2024, 3, 8), "UTEC-REG-300");

        System.out.println("Transacciones agregadas al bloque propuesto: " + blockchain.getProposedBlock().getTransactions().size());
        System.out.println("Muestra de identidad cifrada:");
        for (AcademicTitleTransaction tx : blockchain.getProposedBlock().getTransactions()) {
            System.out.println("- tx=" + tx.getTransactionId() + " | nombreCipher=" + tx.getEncryptedFullName() + " | duiCipher=" + tx.getEncryptedDui());
        }

        blockchain.mineLastBlock();
        System.out.println("Validación inicial: " + blockchain.validateChain());

        Block target = blockchain.getLastBlock();
        AcademicTitleTransaction tx = target.getTransactions().get(0);
        String originalInstitution = tx.getInstitutionName();
        tx.setInstitutionName("Institución fraudulenta");

        System.out.println("Se ha alterado la transacción del bloque minado.");
        System.out.println("Regla de negocio detectada: los bloques minados son inmutables y la cadena debe poder validarse completa.");
        System.out.println("Validación después de la alteración: " + blockchain.validateChain());

        tx.setInstitutionName(originalInstitution);
    }
}
