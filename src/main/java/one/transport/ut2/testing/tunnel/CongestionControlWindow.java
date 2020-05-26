package one.transport.ut2.testing.tunnel;

public interface CongestionControlWindow {

    Statistic statistic = new Statistic();

    boolean tryToPush(TunnelInterface.PacketWithTimestamp packetWithTimestamp);

    TunnelInterface.PacketWithTimestamp peek();

    TunnelInterface.PacketWithTimestamp pop();

    void setCapacity(int newCapacity);

    int getCapacity();

    class Statistic {
        private int totalPackets = 0;
        private int droppedPackets = 0;

        public void incrementTotalPackets() {
            totalPackets++;
        }

        public void incrementDroppedPackets() {
            droppedPackets++;
        }

        int flushDroppedPackets() {
            final int result = droppedPackets;
            droppedPackets = 0;
            return result;
        }

        int flushTotalPackets() {
            final int result = totalPackets;
            totalPackets = 0;
            return result;
        }
    }
}
