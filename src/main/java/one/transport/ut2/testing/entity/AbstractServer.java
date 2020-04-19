package one.transport.ut2.testing.entity;


import java.nio.file.Path;

public abstract class AbstractServer extends Thread {
    private final Path logDir;
    protected TestResult testResult;

    protected int port;
    protected byte[] fileData;

    protected AbstractServer(Path logDir) {
        testResult = new TestResult();
        this.logDir = logDir;
    }

    public abstract void clear() throws TestErrorException;

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public TestResult getTestResult() {
        return testResult;
    }
}
