package one.transport.ut2.testing.utils;

public class IpUtils {

    public static String ipAddrFromBytes(byte[] addr) {
        assert (addr.length == 4);
        return (0xFF & addr[0]) + "." + (0xFF & addr[1]) + "." + (0xFF & addr[2]) + "." + (0xFF & addr[3]);
    }

//    public static byte[] changeNetAddr(byte[] )

}
