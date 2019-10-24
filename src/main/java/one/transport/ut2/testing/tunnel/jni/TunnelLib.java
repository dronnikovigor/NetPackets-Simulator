package one.transport.ut2.testing.tunnel.jni;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TunnelLib {
    private static final String TUNNEL_LIB_PATH = "tunnel/build/libtunnel.so";

    public static void load() {
        Path path = Paths.get(TUNNEL_LIB_PATH);
        System.load(path.toAbsolutePath().toString());
    }

}
