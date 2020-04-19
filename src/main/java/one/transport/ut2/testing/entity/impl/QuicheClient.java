package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractQuicClient;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicheClient extends AbstractQuicClient {

    public QuicheClient(Configuration.Device clientConf, Configuration.Device serverConf, int id, Path logDir, int fileSize) throws TestErrorException {
        super(id, logDir, fileSize);
        try {
            final byte[] clientDeviceIpBytes = clientConf.getIpBytes();
            clientDeviceIpBytes[3] = serverConf.getIpBytes()[3];
            processBuilder = new ProcessBuilder()
                    .directory(logDir.toFile())
                    .command(
                            applicationProps.getProperty("quiche.client.binary"),
                            "--no-verify",
                            "http://" + InetAddress.getByAddress(clientDeviceIpBytes).getHostAddress() + ":" + serverConf.udpPort + "/" + fileSize + "KB"
                    );
        } catch (UnknownHostException e) {
            throw new TestErrorException("Unknown Host: " + e);
        }
    }
}
