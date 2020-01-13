package one.transport.ut2.testing.tunnel.impl;

import one.transport.ut2.testing.tunnel.CongestionControlWindow;
import one.transport.ut2.testing.tunnel.Packet;
import one.transport.ut2.testing.tunnel.TunnelInterface;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class CongestionControlWindowImpl implements CongestionControlWindow {
    private Deque<TunnelInterface.PacketWithTimestamp> deque = new ConcurrentLinkedDeque<>();

    private int capacity = 1048576;
    private int currentSize;

    public CongestionControlWindowImpl(int initialCapacity) {
        this();
        capacity = initialCapacity;
    }

    public CongestionControlWindowImpl() {
        currentSize = 0;
    }

    @Override
    public boolean tryToPush(TunnelInterface.PacketWithTimestamp packetWithTimestamp) {
        statistic.incrementTotalPackets();
        Packet packet = packetWithTimestamp.packet;
        if (currentSize + (packet.getLength() - 4) <= capacity) {
            deque.add(packetWithTimestamp);
            currentSize = deque.stream().mapToInt(packetWithTimestampD -> packetWithTimestampD.packet.getLength() - 4).sum();
            return true;
        } else {
            statistic.incrementDroppedPackets();
            return false;
        }
    }

    @Override
    public TunnelInterface.PacketWithTimestamp peek() {
        return deque.peek();
    }

    @Override
    public TunnelInterface.PacketWithTimestamp pop() {
        final TunnelInterface.PacketWithTimestamp packetWithTimestamp = deque.pop();
        currentSize = deque.stream().mapToInt(packetWithTimestampD -> packetWithTimestampD.packet.getLength() - 4).sum();
        return packetWithTimestamp;
    }

    @Override
    public void setCapacity(int newCapacity) {
        capacity = newCapacity;
    }
}
