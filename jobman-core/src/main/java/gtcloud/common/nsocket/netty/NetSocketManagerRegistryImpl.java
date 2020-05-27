package gtcloud.common.nsocket.netty;

import java.util.HashMap;

import gtcloud.common.basetypes.Options;
import gtcloud.common.nsocket.NetSocketException;
import gtcloud.common.nsocket.NetSocketManager;
import gtcloud.common.nsocket.NetSocketManagerRegistry;

public class NetSocketManagerRegistryImpl extends NetSocketManagerRegistry {

    private HashMap<String, NetSocketManager> _table = new HashMap<String, NetSocketManager>(3);

    @Override
    public NetSocketManager getNetSocketManager(String name,
                                                int netioThreadCount,
                                                boolean createIfMissing,
                                                Options options) throws NetSocketException {

        synchronized(_table) {
            NetSocketManager oldSockman = _table.get(name);
            if (oldSockman != null) {
                return oldSockman;
            }

            if (!createIfMissing) {
                return null;
            }

            NetSocketManagerImpl newSockman = new NetSocketManagerImpl(name, netioThreadCount, options);
            _table.put(name, newSockman);
            return newSockman;
        }
    }
}
