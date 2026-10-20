package sv.edu.blockchain.api.service;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import sv.edu.blockchain.BlockChain;
import sv.edu.blockchain.api.model.CreateTitleRequest;
import sv.edu.blockchain.api.model.AcademicTitleView;
import sv.edu.blockchain.api.model.BlockDetails;
import sv.edu.blockchain.api.model.BlockSummary;
import sv.edu.blockchain.api.model.ChainValidation;
import sv.edu.blockchain.api.model.ChainStatus;
import sv.edu.blockchain.api.model.MinedBlockResponse;
import sv.edu.blockchain.api.model.NodeStatus;
import sv.edu.blockchain.api.model.NodesStatus;
import sv.edu.blockchain.api.model.PendingTitleResponse;
import sv.edu.blockchain.api.model.TitleDetails;
import sv.edu.blockchain.api.repository.BlockchainConflictException;
import sv.edu.blockchain.api.repository.BlockchainRepository;
import sv.edu.blockchain.api.repository.BlockchainUnavailableException;
import sv.edu.blockchain.model.AcademicTitleTransaction;
import sv.edu.blockchain.model.Block;
import sv.edu.blockchain.protocol.BlockData;
import sv.edu.blockchain.protocol.BlockData.TransactionData;

@ApplicationScoped
public class BlockchainService {
    private final BlockchainRepository repository;
    private final List<AcademicTitleTransaction> pendingTitles = new ArrayList<>();

    @Inject
    public BlockchainService(BlockchainRepository repository) {
        this.repository = repository;
    }

    public List<BlockSummary> findAllBlocks() {
        return findAllBlocks(null);
    }

    public List<BlockSummary> findAllBlocks(String nodeId) {
        return repository.findAllBlocks(nodeId).stream()
                .map(BlockchainService::toSummary)
                .toList();
    }

    public Optional<BlockDetails> findBlockByHash(String hash) {
        return findBlockByHash(null, hash);
    }

    public Optional<BlockDetails> findBlockByHash(String nodeId, String hash) {
        return repository.findBlockByHash(nodeId, hash).map(BlockchainService::toDetails);
    }

    public Optional<TitleDetails> findTitleByTransactionId(String transactionId) {
        return findTitleByTransactionId(null, transactionId);
    }

    public Optional<TitleDetails> findTitleByTransactionId(String nodeId, String transactionId) {
        return repository.findAllBlocks(nodeId).stream()
                .filter(block -> block.transactions().stream()
                        .anyMatch(transaction -> transaction.transactionId().equals(transactionId)))
                .findFirst()
                .flatMap(block -> block.transactions().stream()
                        .filter(transaction -> transaction.transactionId().equals(transactionId))
                        .findFirst()
                        .map(transaction -> new TitleDetails(toTitle(transaction), toSummary(block))));
    }

    public List<AcademicTitleView> findAllTitles() {
        return findAllTitles(null);
    }

    public List<AcademicTitleView> findAllTitles(String nodeId) {
        return repository.findAllBlocks(nodeId).stream()
                .flatMap(block -> block.transactions().stream())
                .map(BlockchainService::toTitle)
                .toList();
    }

    public synchronized PendingTitleResponse registerTitle(CreateTitleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Debe proporcionar los datos del título.");
        }
        if (request.graduationDate() == null) {
            throw new IllegalArgumentException("La fecha de graduación es obligatoria.");
        }
        if (request.emissionDate() == null) {
            throw new IllegalArgumentException("La fecha de emisión es obligatoria.");
        }
        AcademicTitleTransaction title = new AcademicTitleTransaction(request.transactionId(), request.fullName(),
                request.dui(), request.academicProgram(), request.degree(), request.institutionName(),
                request.graduationDate(), request.emissionDate(), request.issuerSignature());

        boolean alreadyCommitted = repository.findAllBlocks().stream()
                .flatMap(block -> block.transactions().stream())
                .anyMatch(transaction -> transaction.transactionId().equals(title.getTransactionId()));
        boolean alreadyPending = pendingTitles.stream()
                .anyMatch(transaction -> transaction.getTransactionId().equals(title.getTransactionId()));
        if (alreadyCommitted || alreadyPending) {
            throw new BlockchainConflictException("Ya existe un título con ese identificador.");
        }

        pendingTitles.add(title);
        return new PendingTitleResponse(toTitle(title), pendingTitles.size(),
                "Título agregado a la cola; se confirmará cuando se mine el bloque.");
    }

    public synchronized List<AcademicTitleView> findPendingTitles() {
        return pendingTitles.stream().map(BlockchainService::toTitle).toList();
    }

    public synchronized MinedBlockResponse minePendingTitles() {
        if (pendingTitles.isEmpty()) {
            throw new BlockchainConflictException("No hay títulos pendientes para minar.");
        }

        List<BlockData> currentBlocks = repository.findAllBlocks();
        BlockChain chain = new BlockChain(3, "0");
        try {
            for (BlockData block : currentBlocks) {
                chain.addMinedBlock(block.toBlock());
            }
            if (!chain.validateChain()) {
                throw new BlockchainConflictException("La cadena del nodo no supera la validación.");
            }
            Block proposedBlock = chain.createProposedBlock();
            pendingTitles.forEach(proposedBlock::addTransaction);
            chain.mineLastBlock();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new BlockchainConflictException("No se pudo preparar el bloque: " + exception.getMessage());
        }

        Block minedBlock = chain.getLastBlock();
        BlockData acceptedBlock = repository.submitBlock(BlockData.from(minedBlock));
        pendingTitles.clear();
        return new MinedBlockResponse(toSummary(acceptedBlock), acceptedBlock.transactions().size(),
                "Bloque minado y aceptado por el nodo. Los peers lo sincronizarán automáticamente.");
    }

    public ChainStatus getChainStatus() {
        return getChainStatus(null);
    }

    public ChainStatus getChainStatus(String nodeId) {
        try {
            List<BlockData> blocks = repository.findAllBlocks(nodeId);
            return new ChainStatus(true, blocks.size(), "Conectado al nodo blockchain.");
        } catch (BlockchainUnavailableException exception) {
            return new ChainStatus(false, 0, "No se pudo conectar con el nodo blockchain.");
        }
    }

    public ChainValidation validateChain() {
        return validateChain(null);
    }

    public ChainValidation validateChain(String nodeId) {
        List<BlockData> blocks = repository.findAllBlocks(nodeId);
        if (blocks.isEmpty()) {
            return new ChainValidation(false, 0, "La cadena no contiene bloques.");
        }
        BlockChain chain = new BlockChain(3, "0");
        try {
            for (BlockData block : blocks) {
                chain.addMinedBlock(block.toBlock());
            }
        } catch (IllegalArgumentException exception) {
            return new ChainValidation(false, blocks.size(), exception.getMessage());
        }
        boolean valid = chain.validateChain();
        return new ChainValidation(valid, blocks.size(),
                valid ? "La cadena es válida." : "La cadena no supera la validación.");
    }

    public NodesStatus getNodesStatus() {
        List<NodeStatus> nodes = repository.getNodesStatus();
        NodeStatus first = nodes.get(0);
        NodeStatus second = nodes.get(1);
        boolean synchronizedChains = first.connected() && second.connected()
                && first.chainValid() && second.chainValid()
                && first.blockCount() == second.blockCount()
                && first.lastBlockHash() != null
                && first.lastBlockHash().equals(second.lastBlockHash());
        String message = synchronizedChains
                ? "Ambos nodos están conectados y tienen la misma cadena válida."
                : "Las cadenas aún no coinciden o uno de los nodos no está disponible.";
        return new NodesStatus(repository.getPrimaryNodeId(), synchronizedChains, message, nodes);
    }

    private static BlockSummary toSummary(BlockData block) {
        return new BlockSummary(block.id(), block.timestamp(), block.previousHash(), block.hash(),
                block.transactions().size());
    }

    private static BlockDetails toDetails(BlockData block) {
        return new BlockDetails(block.id(), block.timestamp(), block.previousHash(), block.hash(),
                block.nonce(), block.transactions().stream().map(BlockchainService::toTitle).toList());
    }

    private static AcademicTitleView toTitle(TransactionData transaction) {
        return new AcademicTitleView(transaction.transactionId(), transaction.academicProgram(),
                transaction.degree(), transaction.institutionName(), transaction.graduationDate(),
                transaction.emissionDate(), transaction.issuerSignature());
    }

    private static AcademicTitleView toTitle(AcademicTitleTransaction transaction) {
        return new AcademicTitleView(transaction.getTransactionId(), transaction.getAcademicProgram(),
                transaction.getDegree(), transaction.getInstitutionName(), transaction.getGraduationDate(),
                transaction.getEmissionDate(), transaction.getIssuerSignature());
    }
}
