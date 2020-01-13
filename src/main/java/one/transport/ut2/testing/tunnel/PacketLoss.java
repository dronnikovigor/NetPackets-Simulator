package one.transport.ut2.testing.tunnel;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class PacketLoss {
    private final LossParams lossParams;
    private final ArrayList<Long> timestamps;

    private long resetId = Long.MIN_VALUE;

    PacketLoss(LossParams lossParams) {
        this.lossParams = lossParams;
        timestamps = new ArrayList<>();
    }

    private boolean checkLossGap(long id) {
        long fullPeriod = (id - lossParams.x0) / (lossParams.up + lossParams.down);
        long local = (id - lossParams.x0) - fullPeriod * (lossParams.up + lossParams.down);
        return !(local - lossParams.up < 0);
    }

    private boolean checkInterval(long timestamp) {
        timestamps.add(timestamp);
        if (timestamps.size() > lossParams.interval + 1)
            timestamps.remove(0);
        if (timestamps.size() == lossParams.interval + 1) {
            boolean[] result = new boolean[lossParams.interval - 1];
            for (int i = 1; i < timestamps.size() - 2; i++) {
                result[i] = timestamps.get(i + 1) - timestamps.get(i) < timestamps.get(i + 2) - timestamps.get(i + 1);
            }
            if (IntStream.range(1, result.length).mapToObj(idx -> result[idx]).allMatch(idx -> idx)) {
                timestamps.clear();
                return false;
            }
        }
        return true;
    }

    private long getResetId(long id) {
        long fullPeriod = (id - lossParams.x0) / (lossParams.up + lossParams.down);
        return (fullPeriod + 1) * (lossParams.up + lossParams.down);
    }

    /**
     * Loss function.
     *
     * @param id Id of packet to check.
     * @return true - loss, false - skip.
     */
    boolean loss(long id, long timestamp) {
        if (id >= resetId) {
            boolean result = lossParams.enabled && checkLossGap(id);
            if (result && lossParams.intervals) {
                result = checkInterval(timestamp);
                if (!result)
                    resetId = getResetId(id);
            }
            return result;
        }
        return false;
    }

    public static class LossParams {
        private final int x0;
        private final int up;
        private final int down;
        private final int interval;

        private boolean enabled;
        private boolean intervals;

        public LossParams(int x0, int up, int down, int interval) {
            this.x0 = x0;
            this.up = up;
            this.down = down;
            this.interval = interval;
        }

        public String getName() {
            return ("x_" + x0 + "_up_" + up + "_down_" + down).replaceAll("-", "_");
        }

        @Override
        public String toString() {
            return "LossParams{" +
                    "x0=" + x0 +
                    ", up=" + up +
                    ", down=" + down +
                    ", interval=" + interval +
                    ", enabled=" + enabled +
                    ", intervals=" + intervals +
                    '}';
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setIntervals(boolean intervals) {
            this.intervals = intervals;
        }
    }
}
