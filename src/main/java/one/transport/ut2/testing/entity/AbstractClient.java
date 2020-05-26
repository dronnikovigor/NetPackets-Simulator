package one.transport.ut2.testing.entity;

import java.nio.file.Path;
import java.util.Map;

public abstract class AbstractClient extends Thread implements CommonClient, SingleFileSizeClient{
    final protected int id;
    protected ProcessBuilder processBuilder;
    Process process;
    final protected Path logDir;

    protected TestResult testResult;

    protected static int reqAmount = 1;

    protected Map<Integer, byte[]> filesData;
    protected static int fileSize;

    protected long startTime;
    protected long finishTime;

    public static void setReqAmount(int reqAmount) {
        AbstractClient.reqAmount = reqAmount;
    }

    public static void setFileSize(int fileSize) {
        AbstractClient.fileSize = fileSize;
    }

    protected AbstractClient(int id, Path logDir) {
        testResult = new TestResult();
        this.id = id;
        this.logDir = logDir;
    }

    public void setFilesData(Map<Integer, byte[]> filesData) {
        this.filesData = filesData;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    @Override
    public long getResultTime() {
        return finishTime - startTime;
    }

    @Override
    abstract public void clear();

    @Override
    abstract public String getError() throws TestErrorException;

    @Override
    abstract public boolean validateResponse() throws TestErrorException;

    @Override
    public int getClientId() {
        return id + 1;
    }
}
