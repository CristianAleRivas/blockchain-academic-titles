package sv.edu.blockchain.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import sv.edu.blockchain.BlockChain;
import sv.edu.blockchain.model.Block;
import sv.edu.blockchain.protocol.BlockData;
import sv.edu.blockchain.protocol.BlockchainJson;
import sv.edu.blockchain.protocol.NetworkRequest;
import sv.edu.blockchain.protocol.NetworkResponse;
import sv.edu.blockchain.protocol.TlsBlockchainClient;
import sv.edu.blockchain.service.EncryptedBlockStore;

public class BlockchainServer implements AutoCloseable {
    private final int port;
    private final BlockChain blockchain;
    private final EncryptedBlockStore store;
    private final List<Peer> peers;
    private final ExecutorService clients = Executors.newCachedThreadPool();
    private final ScheduledExecutorService synchronizer = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running;
    private SSLServerSocket serverSocket;

    public BlockchainServer(int port, Path storageDirectory, String storageKey, List<Peer> peers)
            throws IOException {
        this.port = port;
        this.blockchain = new BlockChain(3, "0");
        this.store = new EncryptedBlockStore(storageDirectory, storageKey);
        this.peers = List.copyOf(peers);
        List<Block> savedBlocks = store.load();
        if (!savedBlocks.isEmpty() && !blockchain.considerChain(savedBlocks)) {
            throw new IOException("La cadena guardada no supera las validaciones de consenso.");
        }
        if (savedBlocks.isEmpty()) {
            blockchain.createGenesis();
            store.saveChain(blockchain.getBlocks());
        }
    }

    public void start() throws IOException {
        serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(port);
        running = true;
        synchronizer.scheduleWithFixedDelay(this::synchronizePeersSafely, 1, 5, TimeUnit.SECONDS);
        System.out.println("Servidor blockchain TLS activo en el puerto " + port);
        while (running) {
            SSLSocket client = (SSLSocket) serverSocket.accept();
            clients.execute(() -> handle(client));
        }
    }

    private void handle(SSLSocket socket) {
        try (socket) {
            socket.setSoTimeout(10000);
            socket.startHandshake();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            NetworkResponse response;
            if (line == null || line.length() > 1_000_000) {
                response = NetworkResponse.error("Solicitud vacía o demasiado grande.");
            } else {
                try {
                    response = handleRequest(BlockchainJson.MAPPER.readValue(line, NetworkRequest.class));
                } catch (Exception exception) {
                    response = NetworkResponse.error(exception.getMessage() == null
                            ? "Solicitud inválida." : exception.getMessage());
                }
            }
            writer.write(BlockchainJson.MAPPER.writeValueAsString(response));
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            System.err.println("Error atendiendo cliente TLS: " + exception.getMessage());
        }
    }

    private NetworkResponse handleRequest(NetworkRequest request) throws IOException {
        if (request == null || request.action() == null) {
            return NetworkResponse.error("La solicitud debe indicar una acción.");
        }
        return switch (request.action()) {
            case "get-chain" -> new NetworkResponse(true, "Cadena actual.",
                    blockchain.getBlocks().stream().map(BlockData::from).toList(), null);
            case "get-block" -> findBlock(request.hash());
            case "submit-block" -> acceptBlock(request.block());
            default -> NetworkResponse.error("Acción no soportada.");
        };
    }

    private NetworkResponse findBlock(String hash) {
        if (hash == null || hash.isBlank()) {
            return NetworkResponse.error("Debe proporcionar el hash del bloque.");
        }
        return blockchain.getBlocks().stream()
                .filter(block -> hash.equals(block.getHash()))
                .findFirst()
                .map(block -> new NetworkResponse(true, "Bloque encontrado.", List.of(), BlockData.from(block)))
                .orElseGet(() -> NetworkResponse.error("No existe un bloque con ese hash."));
    }

    private NetworkResponse acceptBlock(BlockData data) throws IOException {
        if (data == null) {
            return NetworkResponse.error("Debe proporcionar un bloque.");
        }
        synchronized (blockchain) {
            Block block;
            try {
                block = data.toBlock();
                BlockChain candidate = new BlockChain(blockchain.getDifficulty(), blockchain.getPrefix());
                for (Block existing : blockchain.getBlocks()) {
                    candidate.addMinedBlock(existing);
                }
                candidate.addMinedBlock(block);
                if (!candidate.validateChain()) {
                    throw new IllegalArgumentException("El bloque produce una cadena inválida.");
                }
                blockchain.addMinedBlock(block);
            } catch (RuntimeException exception) {
                return NetworkResponse.error(exception.getMessage());
            }
            store.saveChain(blockchain.getBlocks());
            return new NetworkResponse(true, "Bloque validado y agregado.", List.of(), BlockData.from(block));
        }
    }

    private void synchronizePeersSafely() {
        for (Peer peer : peers) {
            try {
                List<BlockData> remoteData = new TlsBlockchainClient(peer.host(), peer.port(), 3000).getChain();
                List<Block> remoteBlocks = new ArrayList<>();
                for (BlockData block : remoteData) {
                    remoteBlocks.add(block.toBlock());
                }
                synchronized (blockchain) {
                    if (blockchain.considerChain(remoteBlocks)) {
                        store.saveChain(blockchain.getBlocks());
                        System.out.println("Cadena sincronizada desde " + peer.host() + ":" + peer.port());
                    }
                }
            } catch (Exception exception) {
                System.err.println("No se pudo sincronizar con " + peer.host() + ":" + peer.port()
                        + ": " + exception.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        synchronizer.shutdownNow();
        clients.shutdownNow();
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    public record Peer(String host, int port) {
        public Peer {
            if (host == null || host.isBlank() || port < 1 || port > 65535) {
                throw new IllegalArgumentException("La configuración de peer no es válida.");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Uso: BlockchainServer <puerto> <directorio> [host:puerto ...]");
            System.exit(2);
        }
        int port = Integer.parseInt(args[0]);
        List<Peer> peers = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String[] endpoint = args[i].split(":", 2);
            if (endpoint.length != 2) {
                throw new IllegalArgumentException("Peer inválido: " + args[i]);
            }
            peers.add(new Peer(endpoint[0], Integer.parseInt(endpoint[1])));
        }
        String key = System.getenv("BLOCKCHAIN_STORAGE_KEY");
        try (BlockchainServer server = new BlockchainServer(port, Path.of(args[1]), key, peers)) {
            server.start();
        }
    }
}
