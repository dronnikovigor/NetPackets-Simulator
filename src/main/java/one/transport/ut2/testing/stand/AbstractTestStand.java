package one.transport.ut2.testing.stand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestContext;
import one.transport.ut2.testing.tunnel.PacketLoss;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import one.transport.ut2.testing.utils.FileUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public abstract class AbstractTestStand {
    Path logDir;

    TestContext testContext;
    TestContext.TestResult testResult;

    private Process tsharkProcess;

    byte serverId;
    int reqAmount;
    int bandwidth;
    private int speed;
    PacketLoss.LossParams lossParams;
    private double speedRate;
    int fileSize;

    public abstract void init(Configuration configuration, TunnelInterface tunnelInterface);

    public TestContext.TestResult runTest(int fileSize) throws Exception {
        serverId = (byte) 254;
        this.fileSize = fileSize;
        reqAmount = testContext.configuration.reqAmount;
        bandwidth = testContext.tunnelInterface.bandwidth;
        speed = testContext.tunnelInterface.speed;
        speedRate = testContext.tunnelInterface.speedRate;
        lossParams = testContext.tunnelInterface.lossParams;

        testResult = new TestContext.TestResult();
        testResult.fileSize = fileSize;
        testResult.bandwidth = bandwidth;
        testResult.requests = reqAmount;
        testResult.lossParams = lossParams;
        testResult.speed = speed;
        testResult.speedRate = speedRate;

        initLogDir(applicationProps.getProperty("log.dir"),
                fileSize + "KB_" + bandwidth + "ms__PL_" + lossParams.getName());

        if (testContext.configuration.dumping) {
            try {
                tsharkProcess = new ProcessBuilder()
                        .directory(logDir.toFile())
                        .command("tshark", "-i", "tun1", "-w", "tsharkdump.pcap")
                        .start();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return new TestContext.TestResult("Error while starting Tshark: " + e, 0, bandwidth, fileSize,
                        reqAmount, lossParams, null);
            }
        }

        return testResult;
    }

    public void clear() {
        try {
            TunnelInterface.Statistic sentStat = testContext.tunnelInterface.flushStat();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonWriter jsonWriter = gson.newJsonWriter(new FileWriter(logDir.resolve("tunnelStat.json").toFile()));
            gson.toJson(sentStat, TunnelInterface.Statistic.class, jsonWriter);
            jsonWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (tsharkProcess != null) {
            tsharkProcess.destroy();
            try {
                tsharkProcess.waitFor(10000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void initLogDir(String logDir, String profileName) throws IOException {
        final Path baseLogDir = Paths.get(logDir);
        final Path testLogDir = baseLogDir.resolve(this.getClass().getSimpleName());
        this.logDir = testLogDir.resolve(profileName);
        FileUtils.delete(this.logDir);
        Files.createDirectories(this.logDir);
    }
}
