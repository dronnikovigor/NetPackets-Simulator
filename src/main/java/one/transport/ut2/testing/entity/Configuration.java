package one.transport.ut2.testing.entity;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import one.transport.ut2.testing.cluster.ClusterHost2;
import one.transport.ut2.testing.cluster.ClusterUtils;
import one.transport.ut2.testing.tunnel.Packet;
import one.transport.ut2.testing.tunnel.PacketLoss;
import one.transport.ut2.testing.utils.IpUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Configuration {
    @SerializedName(value = "tunnel_device")
    @NotNull
    public final String tunnelDevice;
    @SerializedName(value = "device")
    @NotNull
    public final Device[] devices;
    @SerializedName(value = "file_size")
    @NotNull
    public final int[] fileSizes;
    @SerializedName(value = "rtt")
    @NotNull
    public final int[] rtts;
    @SerializedName(value = "req_amount")
    public final int reqAmount;
    @SerializedName(value = "lossProfile")
    @NotNull
    public final PacketLoss.LossParams[] lossParams;
    public final boolean dumping;
    @SerializedName(value = "bandwidth")
    @NotNull
    public final int[] bandwidths;
    @SerializedName(value = "speed_rate")
    @NotNull
    public final double[] speedRates;
    @SerializedName(value = "congestion_window")
    @NotNull
    public final int[] congestionControlWindows;

    private Configuration(@NotNull String tunnelDevice, @NotNull Device[] devices, @NotNull int[] fileSizes, @NotNull int[] rtts, int reqAmount, @NotNull PacketLoss.LossParams[] lossParams, boolean dumping, @NotNull int[] bandwidths, @NotNull double[] speedRates, @NotNull int[] congestionControlWindows) {
        this.tunnelDevice = tunnelDevice;
        this.devices = devices;
        this.fileSizes = fileSizes;
        this.rtts = rtts;
        this.reqAmount = reqAmount;
        this.lossParams = lossParams;
        this.dumping = dumping;
        this.bandwidths = bandwidths;
        this.speedRates = speedRates;
        this.congestionControlWindows = congestionControlWindows;
    }

    @Nullable
    public static Configuration parseConfiguration(File configurationFile) {
        Gson gson =
                new GsonBuilder()
                        .registerTypeAdapter(Device.Type.class, new Device.TypeSerializer())
                        .create();
        try {
            JsonReader jsonReader = gson.newJsonReader(new FileReader(configurationFile));
            return gson.fromJson(jsonReader, Configuration.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Device[] getServers() {
        return Arrays.stream(devices)
                .filter(d -> d.type == Device.Type.SERVER)
                .toArray(Device[]::new);
    }

    public Device[] getClients() {
        return Arrays.stream(devices)
                .filter(d -> d.type == Device.Type.CLIENT)
                .toArray(Device[]::new);
    }

    public Device getServer(byte id) {
        for (Device device : devices) {
            if (device.type == Device.Type.SERVER && device.hostAddr == id) {
                return device;
            }
        }
        return null;
    }

    public Device getClient(byte id) {
        for (Device device : devices) {
            if (device.type == Device.Type.CLIENT && device.hostAddr == id) {
                return device;
            }
        }
        return null;
    }

    public static class Device {
        @SerializedName("host_addr")
        final byte hostAddr;
        @SerializedName("net_addr")
        final byte[] netAddr;
        public final Type type;
        @SerializedName("udp_port")
        public int udpPort;
        @SerializedName("tcp_port")
        public int tcpPort;

        volatile boolean active;

        public enum Type {
            CLIENT, SERVER
        }

        Device(byte hostAddr, byte[] netAddr, Type type, int udpPort, int tcpPort) {
            this.hostAddr = hostAddr;
            this.netAddr = netAddr;
            this.type = type;
            this.udpPort = udpPort;
            this.tcpPort = tcpPort;
        }

        public byte getHostAddr() {
            return hostAddr;
        }

        public byte[] getNetAddr() {
            return Arrays.copyOf(netAddr, 4);
        }

        public byte[] getIpBytes() {
            byte[] ip = new byte[4];
            System.arraycopy(netAddr, 0, ip, 0, netAddr.length);
            ip[3] = hostAddr;
            return ip;
        }

        public boolean isSrc(Packet packet) {
            byte[] srcAddr = packet.getSrcAddress();
            return srcAddr[3] == hostAddr;
        }

        public boolean isDst(Packet packet) {
            byte[] dstAddr = packet.getDstAddress();
            return dstAddr[3] == hostAddr;
        }

        public boolean isDstOrSrc(Packet packet) {
            byte[] dstAddr = packet.getDstAddress();
            byte[] srcAddr = packet.getSrcAddress();
            return dstAddr[3] == hostAddr || srcAddr[3] == hostAddr;
        }

        public boolean apply(Packet packet) {
            if (packet.protocol == Packet.Protocol.UDP || packet.protocol == Packet.Protocol.TCP) {
                byte[] dstAddr = packet.getDstAddress();
                //checking  last byte (host addr)
                if (dstAddr[3] == hostAddr) {
                    byte[] srcAddr = packet.getSrcAddress();

                    System.arraycopy(netAddr, 0, dstAddr, 0, netAddr.length);
                    System.arraycopy(netAddr, 0, srcAddr, 0, netAddr.length);

                    packet.setSrcAddress(srcAddr);
                    packet.setDstAddress(dstAddr);

                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            //todo
            return hostAddr;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Device)) {
                return false;
            }
            Device otherDevice = (Device) obj;
            return this.hostAddr == otherDevice.hostAddr
                    && Arrays.equals(netAddr, otherDevice.netAddr)
                    && this.type == otherDevice.type
                    && this.udpPort == otherDevice.udpPort
                    && this.tcpPort == otherDevice.tcpPort;
        }

        static class TypeSerializer implements JsonDeserializer<Type> {
            @Override
            public Type deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                for (Type type : Type.values()) {
                    if (type.name().equalsIgnoreCase(json.getAsString())) {
                        return type;
                    }
                }
                return null;
            }
        }
    }

    public static void writeClientConfiguration(UT2Mode ut2mode, Path clientConf, Device client, Device... serversDevices) throws TestErrorException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        JsonObject obj = new JsonObject();
        obj.addProperty("tcp_mode", ut2mode == UT2Mode.TCP);
        obj.addProperty("udp_mode", ut2mode == UT2Mode.UDP);
        obj.addProperty("client_ip", IpUtils.ipAddrFromBytes(client.getIpBytes()));

        ServerParam[] servers = new ServerParam[serversDevices.length];
        final ConcurrentHashMap<ClusterHost2, Boolean> hosts = ClusterUtils.hosts;

        int i = 0;
        for (ClusterHost2 host : hosts.keySet()) {
            if (ut2mode == UT2Mode.UDP)
                servers[i] = new ServerParam(host.ip, host.udpPort);
            else
                servers[i] = new ServerParam(host.ip, host.tcpPort);
            i++;
        }

        JsonElement element = gson.toJsonTree(servers, ServerParam[].class);
        obj.add("servers", element);

        try (JsonWriter jsonWriter = gson.newJsonWriter(new FileWriter(clientConf.toFile()))){
            gson.toJson(obj, jsonWriter);
        } catch (IOException e) {
            throw new TestErrorException("Error while creating configuration file " + e);
        }
    }

    private static class ServerParam {
        final String ip;
        final int port;

        private ServerParam(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }
}
