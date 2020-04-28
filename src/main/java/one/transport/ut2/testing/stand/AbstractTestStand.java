package one.transport.ut2.testing.stand;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import one.transport.ut2.testing.entity.AbstractClient;
import one.transport.ut2.testing.entity.Configuration;
import one.transport.ut2.testing.entity.TestErrorException;
import one.transport.ut2.testing.entity.TestResult;
import one.transport.ut2.testing.tunnel.TunnelInterface;
import one.transport.ut2.testing.utils.FileUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static one.transport.ut2.testing.ApplicationProperties.applicationProps;

public abstract class AbstractTestStand {
    protected Path logDir;

    protected Configuration configuration;
    protected TunnelInterface tunnelInterface;

    private Process tsharkProcess;

    protected byte serverId;

    protected boolean packetStat;

    public void init(Configuration configuration, TunnelInterface tunnelInterface) throws TestErrorException {
        this.configuration = configuration;
        this.tunnelInterface = tunnelInterface;
        AbstractClient.setReqAmount(configuration.reqAmount);
    }

    public List<TestResult> runTest() throws TestErrorException {
        serverId = (byte) 254;

        initLogDir(applicationProps.getProperty("log.dir"),"rtt " + tunnelInterface.rtt + "ms/bandwidth " + tunnelInterface.bandwidth + "/congestionControl " + tunnelInterface.getCongestionControlWindowCapacity() + tunnelInterface.lossParams.getName());

        if (configuration.dumping) {
            try {
                tsharkProcess = new ProcessBuilder()
                        .directory(logDir.toFile())
                        .command("tshark", "-i", "tun1", "-w", "tsharkdump.pcap")
                        .start();
                Thread.sleep(2000);
            } catch (InterruptedException | IOException e) {
                throw new TestErrorException("Error while starting Tshark: " + e);
            }
        }

        return new ArrayList<>();
    }

    public void clear() throws TestErrorException {
        try {
            TunnelInterface.Statistic sentStat = tunnelInterface.flushStat();
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

        try {
            FileUtils.delete(Paths.get(applicationProps.getProperty("temp.data.folder")));
        } catch (IOException e) {
            throw new TestErrorException("Error while deleting dir with test files " + e);
        }
    }

    private void initLogDir(String logDir, String profileName) throws TestErrorException {
        final Path baseLogDir = Paths.get(logDir);
        final Path testLogDir = baseLogDir.resolve(this.getClass().getSimpleName());
        this.logDir = testLogDir.resolve(profileName);
        try {
            FileUtils.delete(this.logDir);
            Files.createDirectories(this.logDir);
        } catch (IOException e) {
            throw new TestErrorException("Error while initialising log dir " + e);
        }
    }

    void generateTestFiles(String serverAddress) throws TestErrorException {
        String basePath = applicationProps.getProperty("temp.data.folder");
        try {
            FileUtils.delete(Paths.get(basePath));
            Files.createDirectories(Paths.get(basePath));
        } catch (IOException e) {
            throw new TestErrorException("Error while initialising dir with temp files " + e);
        }
        for (int size : configuration.fileSizes) {
            createFile( basePath + "/" + size + "KB",
                    size, serverAddress);
        }
    }

    protected abstract void createFile(String path, int size, String serverAddress) throws TestErrorException;
}
