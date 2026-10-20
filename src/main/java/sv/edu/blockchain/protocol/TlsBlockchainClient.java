package sv.edu.blockchain.protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLParameters;

public class TlsBlockchainClient {
    private final String host;
    private final int port;
    private final int timeoutMillis;

    public TlsBlockchainClient(String host, int port) {
        this(host, port, 5000);
    }

    public TlsBlockchainClient(String host, int port, int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    public NetworkResponse send(NetworkRequest request) throws IOException {
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
        try (socket) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parameters);
            socket.startHandshake();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer.write(BlockchainJson.MAPPER.writeValueAsString(request));
            writer.newLine();
            writer.flush();
            String response = reader.readLine();
            if (response == null) {
                throw new IOException("El servidor cerró la conexión sin responder.");
            }
            return BlockchainJson.MAPPER.readValue(response, NetworkResponse.class);
        }
    }

    public List<BlockData> getChain() throws IOException {
        NetworkResponse response = send(new NetworkRequest("get-chain", null, null));
        if (!response.success()) {
            throw new IOException(response.message());
        }
        return response.blocks();
    }
}
