package one.transport.ut2.testing.entity;

import java.nio.file.Path;

public class ClientInterface {

    public static String sendCommand(int requestsAmount, int requestedFileSize, Path dataFile) {
        return "send " + requestsAmount + " " + requestedFileSize + " " + dataFile.toAbsolutePath().toString() + "\n";
    }

    public static String setSigningKey(String verifyingKey) {
        return "cipher " + verifyingKey + "\n";
    }

    public static String exit() {
        return "exit\n";
    }

}
