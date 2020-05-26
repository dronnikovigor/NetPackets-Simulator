package one.transport.ut2.testing.entity;

public interface SingleFileSizeClient {

    long getResultTime() throws TestErrorException;

    boolean validateResponse() throws TestErrorException;
}
