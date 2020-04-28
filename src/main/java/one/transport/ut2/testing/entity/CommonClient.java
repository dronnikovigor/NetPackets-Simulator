package one.transport.ut2.testing.entity;

public interface CommonClient {

    void clear();

    String getError() throws TestErrorException;
}
