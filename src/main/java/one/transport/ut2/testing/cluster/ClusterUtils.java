package one.transport.ut2.testing.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterUtils {
    public static final ConcurrentHashMap<ClusterHost2, Boolean> hosts = new ConcurrentHashMap<>();

    public static ClusterHost2[] getHosts() {
        return ClusterUtils.hosts.entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toArray(ClusterHost2[]::new);
    }

    public static void banServer(byte hostId) {
        for (Map.Entry<ClusterHost2, Boolean> entry : hosts.entrySet()) {
            if (entry.getKey().instanceId == (hostId & 0xFF)) {
                entry.setValue(false);
                return;
            }
        }
    }
}
