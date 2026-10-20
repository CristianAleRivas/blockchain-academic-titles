package sv.edu.blockchain.wallet;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import sv.edu.blockchain.protocol.BlockData;
import sv.edu.blockchain.protocol.NetworkRequest;
import sv.edu.blockchain.protocol.NetworkResponse;
import sv.edu.blockchain.protocol.TlsBlockchainClient;

public class WalletMain {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Uso: WalletMain <host> <puerto>");
            System.exit(2);
        }
        TlsBlockchainClient client = new TlsBlockchainClient(args[0], Integer.parseInt(args[1]));
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.println("\n=== WALLET DE TÍTULOS ACADÉMICOS ===");
                System.out.println("1. Consultar cadena");
                System.out.println("2. Consultar bloque por hash");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                String option = scanner.nextLine().trim();
                try {
                    switch (option) {
                        case "1" -> printChain(client.getChain());
                        case "2" -> {
                            System.out.print("Hash del bloque: ");
                            NetworkResponse response = client.send(
                                    new NetworkRequest("get-block", scanner.nextLine().trim(), null));
                            if (!response.success()) {
                                System.out.println("Consulta rechazada: " + response.message());
                            } else {
                                printBlock(response.block());
                            }
                        }
                        case "0" -> running = false;
                        default -> System.out.println("Opción no válida.");
                    }
                } catch (IOException exception) {
                    System.err.println("No se pudo consultar el servidor: " + exception.getMessage());
                }
            }
        }
    }

    private static void printChain(List<BlockData> blocks) {
        if (blocks.isEmpty()) {
            System.out.println("La cadena no contiene bloques.");
            return;
        }
        blocks.forEach(WalletMain::printBlock);
    }

    private static void printBlock(BlockData block) {
        System.out.printf("Bloque #%d | hash=%s | anterior=%s | transacciones=%d%n",
                block.id(), block.hash(), block.previousHash(), block.transactions().size());
        block.transactions().forEach(transaction -> System.out.printf(
                "  Título %s | %s - %s | %s | identidad protegida%n",
                transaction.transactionId(), transaction.degree(), transaction.academicProgram(),
                transaction.institutionName()));
    }
}
