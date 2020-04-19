package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractClient;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Arrays;

public class PureTcpClient extends AbstractClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(PureTcpClient.class);

    private OutputStream os;
    private DataInputStream dis;

    private byte[] response;

    public PureTcpClient(Configuration.Device clientConf, Configuration.Device serverConf, int id, Path logDir, int fileSize) throws TestErrorException {
        super(id, logDir, fileSize);
        Socket socket = new Socket();
        try {
            SocketAddress clientAddr =
                    new InetSocketAddress(InetAddress.getByAddress(clientConf.getIpBytes()), 0);
            socket.bind(clientAddr);
            clientConf.tcpPort = socket.getLocalPort();
        } catch (IOException e) {
            throw new TestErrorException("Error while binding client: " + e);
        }
        try {
            byte[] serverAddr2Bytes = clientConf.getNetAddr();
            serverAddr2Bytes[3] = serverConf.getHostAddr();
            SocketAddress serverAddr =
                    new InetSocketAddress(InetAddress.getByAddress(serverAddr2Bytes), serverConf.tcpPort);
            socket.connect(serverAddr);
        } catch (IOException e) {
            throw new TestErrorException("Error while connecting client: " + e);
        }
        try {
            InputStream is = socket.getInputStream();
            os = socket.getOutputStream();
            dis = new DataInputStream(
                    new BufferedInputStream(is)
            );
        } catch (IOException e) {
            throw new TestErrorException("IOException while opening client output streams: " + e);
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < reqAmount; ++i) {
            ByteArrayOutputStream req2 = new ByteArrayOutputStream();
            req2.write(fileData, 0, 1024);

            try {
                req2.writeTo(os);
                os.flush();
                response = new byte[fileData.length];
                dis.readFully(response);

                LOGGER.info("Progress: " + (i + 1) * 100 / reqAmount + "%");
            } catch (IOException e) {
                testResult = new TestResult("IOException while RW to stream: " + e);
            }
        }
        testResult.success = true;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean validateResponse() {
        return Arrays.equals(response, fileData);
    }
}
