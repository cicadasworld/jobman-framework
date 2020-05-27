package gtcloud.common.nsocket;

import gtcloud.common.basetypes.Options;
import gtcloud.common.nsocket.netty.NetSocketManagerRegistryImpl;

public abstract class NetSocketManagerRegistry {
    //
    // 获取或创建NetSocketManager实例
    // netioThreadCount若为0, 则使用默认值。
    //
    abstract public NetSocketManager getNetSocketManager(
            String name,
            int netioThreadCount,
            boolean createIfMissing,
            Options options)
            throws NetSocketException;

    public NetSocketManager getNetSocketManager(
            String name,
            int netioThreadCount)
            throws NetSocketException {
    	return getNetSocketManager(name, netioThreadCount, true, null);
    }    
    
    private static final NetSocketManagerRegistryImpl _instance = new NetSocketManagerRegistryImpl();

    public static NetSocketManagerRegistry getInstance() {
        return _instance;
    }
}
