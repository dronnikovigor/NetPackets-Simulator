package one.transport.ut2.testing.tunnel;


import java.util.Arrays;

public class Packet {
    private static final int IP_HEADER_POS = 4;
    private static final int SRC_START_POS = IP_HEADER_POS + 12;
    private static final int DST_START_POS = IP_HEADER_POS + 16;

    private final byte[] data;
    private final int ipHeaderLength;
    public final Protocol protocol;
    private byte[] srcAddr;
    private byte[] dstAddr;

    public Packet(byte[] data) {
        this.data = data;
        ipHeaderLength = (data[IP_HEADER_POS] & 0x0F) * 4;
        protocol = getProtocol();

    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }

    private int getByte(int i) {
        return data[i] & 0xFF;
    }

    private Protocol getProtocol() {
        switch (data[9 + IP_HEADER_POS]) {
            case 1:
                return Protocol.ICMP;
            case 17:
                return Protocol.UDP;
            case 6:
                return Protocol.TCP;
            default:
                return Protocol.UNKNOWN;
        }
    }

    public String getMagic() {
        if (protocol != Protocol.UDP) {
            throw new IllegalStateException("This packet is not UDP, it's " + protocol.name());
        }
        int startPos = IP_HEADER_POS + ipHeaderLength + 8;
        return new String(Arrays.copyOfRange(data, startPos, startPos + 4));
    }

    //region ip addresses
    private byte[] getAddress(int startPos) {
        return Arrays.copyOfRange(data, startPos, startPos + 4);
    }

    private String getAddressString(int startPos) {
        return String.format("%d.%d.%d.%d", getByte(startPos), getByte(startPos + 1), getByte(startPos + 2),
                getByte(startPos + 3));
    }

    private void setAddress(int startPos, byte[] address) {
        //we need to recalculate checksum


        System.arraycopy(address, 0, data, startPos, 4);
    }

    public byte[] getSrcAddress() {
        if (srcAddr == null) {
            srcAddr = getAddress(SRC_START_POS);
        }
        return srcAddr;
    }

    private String getSrcAddressString() {
        return getAddressString(SRC_START_POS);
    }

    public byte[] getDstAddress() {
        if (dstAddr == null) {
            dstAddr = getAddress(DST_START_POS);
        }
        return dstAddr;
    }

    private String getDstAddressString() {
        return getAddressString(DST_START_POS);
    }

    public void setSrcAddress(byte[] newSrcAddress) {
        if (newSrcAddress.length != 4) {
            throw new IllegalArgumentException("IP address should has length of 4 bytes");
        }
        setAddress(SRC_START_POS, newSrcAddress);
    }

    public void setDstAddress(byte[] newSrcAddress) {
        if (newSrcAddress.length != 4) {
            throw new IllegalArgumentException("IP address should has length of 4 bytes");
        }
        setAddress(DST_START_POS, newSrcAddress);
    }
    //endregion

    //region udp
    private int getUdpPort(int startPos) {
        if (protocol != Protocol.UDP) {
            throw new IllegalStateException("This packet is not UDP, it's " + protocol.name());
        }
        return ((data[startPos] & 0xFF) << 8) + (data[startPos + 1] & 0xFF);
    }

    private void setUdpPort(int startPos, int value) {
        if (protocol != Protocol.UDP) {
            throw new IllegalStateException("This packet is not UDP, it's " + protocol.name());
        }
        if (value > 65535 || value < 0) {
            throw new IllegalArgumentException("Wrong port value " + value);
        }

        data[startPos] = (byte) (value >> 8);
        data[startPos + 1] = (byte) (value);
    }

    private int getUdpSrcPort() {
        return getUdpPort(IP_HEADER_POS + ipHeaderLength);
    }

    public void setUdpSrcPort(int newSrcPort) {
        setUdpPort(IP_HEADER_POS + ipHeaderLength, newSrcPort);
    }

    private int getUdpDstPort() {
        return getUdpPort(IP_HEADER_POS + ipHeaderLength + 2);
    }

    public void setUdpDstPort(int newDstPort) {
        setUdpPort(IP_HEADER_POS + ipHeaderLength + 2, newDstPort);
    }

    //endregion

    public enum Protocol {
        ICMP,
        UDP,
        TCP,
        UNKNOWN;

        public static Protocol getByString(String name) {
            for (Protocol item : values()) {
                if (item.toString().equalsIgnoreCase(name)) {
                    return item;
                }
            }
            return UNKNOWN;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Packet{\n")
                .append("\tprotocol: ").append(protocol.name()).append('\n')
                .append("\tsrc address: ").append(getSrcAddressString()).append('\n')
                .append("\tdst address: ").append(getDstAddressString()).append('\n');
        if (protocol == Protocol.UDP) {
            sb.append("\tsrc port: ").append(getUdpSrcPort()).append('\n')
                    .append("\tdst port: ").append(getUdpDstPort()).append('\n');
        }

        sb.append("}\n");
        return sb.toString();
    }
}
