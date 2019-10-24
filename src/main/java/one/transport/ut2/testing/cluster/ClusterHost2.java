package one.transport.ut2.testing.cluster;

import gnu.trove.TCollections;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import one.transport.ut2.cluster.ClusterHost;
import one.transport.ut2.cluster.ClusterHostStatus;
import one.transport.ut2.utils.message.MessageReader;
import one.transport.ut2.utils.message.MessageWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Bulychev Ivan
 * @date 07.02.19.
 */
public final class ClusterHost2 {
    private static final int DEFAULT_WEIGHT = 1000;

    private static final String INSTANCE_ID = "id";
    private static final String IP = "i";
    private static final String HOSTNAME = "h";
    private static final String INTERNAL_IP = "iip";
    private static final String UDP_PORT = "up";
    private static final String TCP_PORT = "tp";
    private static final String STATUS = "s";
    private static final String WEIGHT = "w";
    private static final String REVISION = "rv";
    private static final String CDN_AFFINITY = "cn";
    private static final String TOKENS = "t";

    /* */

    public final long instanceId;
    public final String ip;
    private final String hostname;
    private final String internalIp;
    public final int udpPort;
    public final int tcpPort;
    public final ClusterHostStatus status;
    public final int weight;
    private final String revision;

    private final List<String> cdnAffinity;
    private final TLongList tokens;

    /* */

    public ClusterHost2(Builder builder) {
        this.instanceId = builder.instanceId;
        this.ip = builder.ip;
        this.hostname = builder.hostname;
        this.internalIp = builder.internalIp;
        this.udpPort = builder.udpPort;
        this.tcpPort = builder.tcpPort;
        this.status = builder.status;
        this.weight = builder.weight;
        this.revision = builder.revision;
        this.cdnAffinity = List.copyOf(builder.cdnAffinity);
        this.tokens = TCollections.unmodifiableList(new TLongArrayList(builder.tokens));
    }

    /* */

    public static ClusterHost toOld(ClusterHost2 item) {
        if (item == null) {
            return null;
        }
        ClusterHost.Builder builder = new ClusterHost.Builder();
        builder.ip = item.ip;
        builder.name = item.hostname;
        builder.udpPort = item.udpPort;
        builder.tcpPort = item.tcpPort;
        builder.status = item.status;
        builder.weight = item.weight;
        builder.rev = item.revision;
        builder.cdnAffinity = item.cdnAffinity.toArray(new String[0]);
        return new ClusterHost(builder);
    }

    public static byte[] toBytes(ClusterHost2 item) {
        MessageWriter writer = new MessageWriter()
                .putLong(INSTANCE_ID, item.instanceId)
                .putString(IP, item.ip)
                .putString(HOSTNAME, item.hostname)
                .putString(INTERNAL_IP, item.internalIp)
                .putInt(UDP_PORT, item.udpPort)
                .putInt(TCP_PORT, item.tcpPort)
                .putString(STATUS, String.valueOf(item.status))
                .putInt(WEIGHT, item.weight)
                .putString(REVISION, item.revision)
                .putLongs(TOKENS, item.tokens.toArray());
        if (!item.cdnAffinity.isEmpty()) {
            writer.putStrings(CDN_AFFINITY, item.cdnAffinity);
        }
        return writer.toByteArray();
    }

    public static ClusterHost2 fromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        MessageReader args = MessageReader.parse(bytes);
        ClusterHost2.Builder builder = new Builder();
        builder.instanceId = args.getLong(INSTANCE_ID, 0);
        builder.ip = args.getString(IP);
        builder.hostname = args.getString(HOSTNAME);
        builder.internalIp = args.getString(INTERNAL_IP);
        builder.udpPort = args.getInt(UDP_PORT, 0);
        builder.tcpPort = args.getInt(TCP_PORT, 0);
        builder.status = ClusterHostStatus.resolve(args.getString(STATUS));
        builder.weight = args.getInt(WEIGHT, DEFAULT_WEIGHT);
        builder.revision = args.getString(REVISION);
        builder.cdnAffinity.addAll(Arrays.asList(args.getStrings(CDN_AFFINITY)));
        builder.tokens.addAll(args.getLongs(TOKENS));
        if (builder.ip == null) {
            return null;
        }
        return new ClusterHost2(builder);
    }

    public static class Builder {
        public long instanceId;
        public String ip;
        String hostname;
        String internalIp;
        public int udpPort = 0;
        public int tcpPort = 0;
        public ClusterHostStatus status = ClusterHostStatus.undefined;
        int weight = DEFAULT_WEIGHT;
        String revision;

        final List<String> cdnAffinity = new ArrayList<>();
        final TLongList tokens = new TLongArrayList();

        public Builder() {
        }

        public Builder(ClusterHost2 host) {
            this.instanceId = host.instanceId;
            this.ip = host.ip;
            this.hostname = host.hostname;
            this.internalIp = host.internalIp;
            this.udpPort = host.udpPort;
            this.tcpPort = host.tcpPort;
            this.status = host.status;
            this.weight = host.weight;
            this.revision = host.revision;
            this.cdnAffinity.addAll(host.cdnAffinity);
            this.tokens.addAll(host.tokens);
        }
    }
}
