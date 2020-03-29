package one.transport.ut2.testing.stand;

import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;

import java.io.IOException;
import java.net.InetAddress;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public class QuicQuicheDataTransferTestStand extends AbstractQuicDataTransferTestStand {

    @Override
    void initServer(Configuration.Device device) throws IOException {
        serverThread = new ServerThread(device);
        serverThread.start();
    }

    protected class ServerThread extends AbstractServerThread {
        private String[] cmd;

        protected ServerThread(Configuration.Device device) throws IOException {
            cmd = new String[]{"/bin/sh", "-c",
                    "cd " + applicationProps.getProperty("quiche.home.folder") + ";" +
                            " cargo run" +
                            " --manifest-path=" + applicationProps.getProperty("quiche.manifest.path") +
                            " --bin quiche-server --" +
                            " --cert " + applicationProps.getProperty("quiche.cert.file") +
                            " --key " + applicationProps.getProperty("quiche.key.file") +
                            " --listen " + InetAddress.getByAddress(device.getIpBytes()).getHostAddress() + ":" + device.udpPort +
                            " --root " + applicationProps.getProperty("quic.server.data.folder") + "/www.example.org"};
        }

        @Override
        public void run() {
            try {
                process = Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                testResult = new TestContext.TestResult("Error while starting server: " + e, 0, rtt, fileSize,
                        reqAmount, lossParams, null);
            }
        }

        @Override
        public void clear() {
            if (process != null && process.isAlive())
                process.destroy();
        }
    }
}
