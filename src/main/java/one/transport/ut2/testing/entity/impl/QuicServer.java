package one.transport.ut2.testing.entity.impl;

import one.transport.ut2.testing.entity.AbstractServer;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.TestResult;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicServer extends AbstractServer {
    private ProcessBuilder processBuilder;
    private Process process;

    public QuicServer(Configuration.Device device, Path logDir) throws TestErrorException {
        super(logDir);
        try {
            processBuilder = new ProcessBuilder()
                    .directory(logDir.toFile())
                    .command(
                            applicationProps.getProperty("quic.server.binary"),
                            "--quic_response_cache_dir=" + applicationProps.getProperty("temp.data.folder"),
                            "--certificate_file=" + applicationProps.getProperty("quic.certs.file"),
                            "--key_file=" + applicationProps.getProperty("quic.pkcs8.file"),
                            "--quic_ieft_draft=27",
                            "--quic_verions=h3-27",
                            "--host=" + InetAddress.getByAddress(device.getIpBytes()).getHostAddress(),
                            "--port=" + device.udpPort
                    );
            port = device.udpPort;
        } catch (UnknownHostException e) {
            throw new TestErrorException("Unknown Host: " + e);
        }
    }

    @Override
    public void run() {
        try {
            process = processBuilder.start();
            testResult.success = true;
        } catch (IOException e) {
            testResult = new TestResult("Error while starting server: " + e);
        }
    }

    @Override
    public void clear() {
        if (process != null && process.isAlive())
            process.destroy();
    }
}
