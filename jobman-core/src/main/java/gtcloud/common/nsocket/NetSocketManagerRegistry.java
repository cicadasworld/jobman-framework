package gtcloud.common.nsocket;

import gtcloud.common.basetypes.Options;
import gtcloud.common.nsocket.netty.NetSocketManagerRegistryImpl;

public abstract class NetSocketManagerRegistry {
    //
    // ��ȡ�򴴽�NetSocketManagerʵ��
    // netioThreadCount��Ϊ0, ��ʹ��Ĭ��ֵ��
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
