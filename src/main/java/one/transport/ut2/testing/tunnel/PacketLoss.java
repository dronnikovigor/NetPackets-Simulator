package one.transport.ut2.testing.tunnel;

public class PacketLoss {
    private final LossParams lossParams;

    PacketLoss(LossParams lossParams) {
        this.lossParams = lossParams;
    }

    private boolean checkLossGap(long id) {
        long fullPeriod = (id - lossParams.x0) / (lossParams.up + lossParams.down);
        long local = (id - lossParams.x0) - fullPeriod * (lossParams.up + lossParams.down);
        return !(local - lossParams.up < 0);
    }

    boolean loss(long id) {
        return checkLossGap(id);
    }

    public static class LossParams {
        private final int x0;
        private final int up;
        private final int down;

        public LossParams(int x0, int up, int down) {
            this.x0 = x0;
            this.up = up;
            this.down = down;
        }

        public String getName() {
            return ("x_" + x0 + "_up_" + up + "_down_" + down).replaceAll("-", "_");
        }

    }
}
