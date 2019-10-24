package one.transport.ut2.testing.tunnel.jni;

public class Tunnel extends JNIObject {

    public native boolean init(String interfaceName);

    public native void stop();

    public native byte[] readPacket(int timeout);

    public native void writePacket(byte[] packetData);

}
