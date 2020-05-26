package one.transport.ut2.testing.entity;

import one.transport.ut2.testing.tunnel.PacketLoss;

public class TestResult {
    public boolean success = false;
    public long resultTime;
    public String error;

    public int rtt;
    public int fileSize;
    public int requests;
    public int bandwidth;
    public double speedRate;
    public int congestionControlWindow;

    public one.transport.ut2.testing.tunnel.PacketLoss.LossParams lossParams;

    public PacketLossResults packetLoss = new PacketLossResults();

    public TestResult() {
    }

    public TestResult(String error) {
        this.error = error;
    }

    public TestResult(String error, long resultTime, int fileSize, int bandwidth, int rtt, int requests, double speedRate, int congestionControlWindow, PacketLoss.LossParams lossParams, PacketLossResults packetLoss) {
        this.error = error;
        this.resultTime = resultTime;
        this.fileSize = fileSize;
        this.bandwidth = bandwidth;
        this.rtt = rtt;
        this.requests = requests;
        this.speedRate = speedRate;
        this.congestionControlWindow = congestionControlWindow;
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
                ", resultTime=" + resultTime +
                ", error='" + error + '\'' +
                ", rtt=" + rtt +
                ", fileSize=" + fileSize +
                ", requests=" + requests +
                ", bandwidth=" + bandwidth +
                ", speedRate=" + speedRate +
                ", congestionControlWindow=" + congestionControlWindow +
                ", lossParams=" + lossParams +
                ", packetLoss=" + packetLoss +
                '}';
    }
}
