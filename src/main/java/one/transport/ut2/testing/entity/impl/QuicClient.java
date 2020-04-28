package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractQuicClient;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicClient extends AbstractQuicClient {

    public QuicClient(Configuration.Device clientConf, Configuration.Device serverConf, int id, Path logDir) throws TestErrorException {
        super(id, logDir);
        try {
            final byte[] clientDeviceIpBytes = clientConf.getIpBytes();
            clientDeviceIpBytes[3] = serverConf.getIpBytes()[3];
            processBuilder = new ProcessBuilder()
                    .directory(logDir.toFile())
                    .command(
                            applicationProps.getProperty("quic.client.binary"),
                            "--allow_unknown_root_cert",
                            "--quiet",
                            "--quic_ieft_draft=27",
                            "--quic_verion=27",
                            "http://" + InetAddress.getByAddress(clientDeviceIpBytes).getHostAddress() + ":" + serverConf.udpPort + "/" + fileSize + "KB"
                    );
        } catch (UnknownHostException e) {
            throw new TestErrorException("Unknown Host: " + e);
        }
    }
}
