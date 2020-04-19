package one.transport.ut2.testing.entity;

import java.nio.file.Path;

public abstract class AbstractClient extends Thread {
    final int id;
    protected ProcessBuilder processBuilder;
    Process process;
    final Path logDir;

    protected TestResult testResult;

    protected static int reqAmount = 1;

    protected byte[] fileData;
    private final int fileSize;

    public static void setReqAmount(int reqAmount) {
        AbstractClient.reqAmount = reqAmount;
    }

    protected AbstractClient(int id, Path logDir, int fileSize) {
        testResult = new TestResult();
        this.id = id;
        this.logDir = logDir;
        this.fileSize = fileSize;
    }

    abstract public void clear();

    abstract public boolean validateResponse() throws TestErrorException;


    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }


    public TestResult getTestResult() {
        return testResult;
    }
}
