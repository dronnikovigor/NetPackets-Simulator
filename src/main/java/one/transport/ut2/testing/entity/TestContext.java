package one.transport.ut2.testing.entity;

import one.transport.ut2.testing.tunnel.PacketLoss;
import one.transport.ut2.testing.tunnel.TunnelInterface;

import java.nio.file.Path;
import java.util.List;

public class TestContext {
    final public Configuration configuration;
    final public Path clientDir;
    final public Path clientConfigFile;
    final public ServerSide[] serverSides;
    final public TunnelInterface tunnelInterface;
    final public List<TestResult> testResults;

    public TestContext(Configuration configuration,
                       Path clientDir,
                       Path clientConfigFile,
                       ServerSide[] serverSides,
                       TunnelInterface tunnelInterface, List<TestResult> testResults) {
        this.configuration = configuration;
        this.clientDir = clientDir;
        this.clientConfigFile = clientConfigFile;
        this.serverSides = serverSides;
        this.tunnelInterface = tunnelInterface;
        this.testResults = testResults;
    }

    public static class TestResult {
        public boolean success = false;
        public long resultTime;
        public String error;

        public int bandwidth;
        public int fileSize;
        public int requests;
        public int speed;
        public double speedRate;

        public one.transport.ut2.testing.tunnel.PacketLoss.LossParams lossParams;

        public PacketLossResults packetLoss = new PacketLossResults();

        public TestResult() {
        }

        public TestResult(String error) {
            this.error = error;
        }

        public TestResult(String error, long resultTime, int bandwidth, int fileSize, int requests, PacketLoss.LossParams lossParams, PacketLossResults packetLoss) {
            this.error = error;
            this.resultTime = resultTime;
            this.bandwidth = bandwidth;
            this.fileSize = fileSize;
            this.requests = requests;
            this.lossParams = lossParams;
            this.packetLoss = packetLoss;
        }

        public static class PacketLossResults {
            public double fromClients;
            public double fromServers;
        }

        @Override
        public String toString() {
            return "TestResult{" +
                    "success=" + success +
                    ", error='" + error + '\'' +
                    ", resultTime=" + resultTime +
                    ", bandwidth=" + bandwidth +
                    ", fileSize=" + fileSize +
                    ", requests=" + requests +
                    ", speed=" + speed +
                    ", speedRate=" + speedRate +
                    ", lossParams=" + lossParams +
                    ", packetLoss=" + packetLoss +
                    '}';
        }
    }
}
