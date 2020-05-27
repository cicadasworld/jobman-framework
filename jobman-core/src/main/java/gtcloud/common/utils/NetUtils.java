package gtcloud.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import gtcloud.common.basetypes.PropertiesEx;
import platon.IntHolder;
import platon.StringHolder;


public class NetUtils {

    // 解析出 schema://host?p1=v1&p2=v2 的各部分
    static public class UrlParam {
        public String schema = "";
        public String host = "";
        public PropertiesEx props = new PropertiesEx();

        static public UrlParam parse(String str) {
            UrlParam up = new UrlParam();
            int p = str.indexOf("://");
            if (p > 0) {
                up.schema = str.substring(0, p);
                p += 3;
            } else {
                p = 0;
            }

            int q = str.indexOf('?', p);
            if (q > 0) {
                up.host = str.substring(p, q);
                p = q + 1;
            } else {
                up.host = str.substring(p);
                return up;
            }

            String args_part = str.substring(p) + "&";
            int from = 0;
            for (;;) {
                p = args_part.indexOf('&', from);
                if (p < 0)
                    break;
                q = args_part.indexOf('=', from);
                if (q > 0 && q < p) {
                    String nm = args_part.substring(from, q);
                    String v = args_part.substring(q+1, p);
                    up.props.setProperty(nm, v);
                }

                from = p + 1;
            }

            return up;
        }
    }

    // 解析出 host:port 的各部分
    static public class AddressParam {
        public String host = "";
        public int port = 0;

        static public AddressParam parse(String str) {
            AddressParam ap = new AddressParam();
            str = str.trim();
            int p = str.indexOf(':');
            if (p < 0) {
                ap.host = str;
            } else {
                ap.host = str.substring(0, p);
                try {
                    String tmp = str.substring(p + 1);
                    int port = Integer.parseInt(tmp);
                    ap.port = port;
                } catch (Exception e) {
                    // no-op
                }
            }

            return ap;
        }
    }

    private static final String LS = new String(new byte[] { 13, 10 });

    /**
     * 解析查询串。
     * @param query_string 查询串，形如"p1=v1&amp;p2=v2"
     * @return 返回存放各个参数的“key-value”对的属性对象。
     */
    public static PropertiesEx parseQueryString(String query_string) {
        PropertiesEx result = new PropertiesEx();
        query_string = query_string.replace("&", LS) + LS;

        final class MyStream extends InputStream {
            private String _str;
            private int _index;
            MyStream(String str) {
                _str = str;
                _index = 0;
            }

            @Override
            public int read() throws IOException {
                if (_index >= _str.length()) {
                    return 0;
                } else {
                    return _str.charAt(_index ++);
                }
            }
        }

        try {
            MyStream ms = new MyStream(query_string);
            result.load(ms);
        } catch (Exception e) {
            // Auto-generated catch block
            //e.printStackTrace();
        }
        return result;
    }

    public static class NetAdapterInfo {
        String adapterName;     // name of adapter, such as eth0
        String hardwareAddress; // 4C-CC-6A-0E-2F-9D
        String ipv4Address;     // 129.0.3.243
        String ipv4SubnetMask;  // 255.255.255.0
        String ipv4Gateway;     // 默认网关地址

        public String getAdapterName() {
            return adapterName;
        }

        public String getHardwareAddress() {
            return hardwareAddress;
        }

        public String getIpv4Address() {
            return ipv4Address;
        }

        public String getIpv4SubnetMask() {
            return ipv4SubnetMask;
        }

        public String getIpv4Gateway() {
            return ipv4Gateway;
        }
    }

    public static ArrayList<NetAdapterInfo> getNetAdapters() throws Exception {
        ArrayList<NetAdapterInfo> result = new ArrayList<NetAdapterInfo>();
        Enumeration<NetworkInterface> netcards = NetworkInterface.getNetworkInterfaces();
        while (netcards.hasMoreElements()) {
            NetworkInterface netcard = netcards.nextElement();
            if (netcard.isLoopback() || netcard.isVirtual()) {
                continue;
            }
            NetAdapterInfo nai = digNetworkInterface(netcard);
            if (nai != null) {
                result.add(nai);
            }
        }
        return result;
    }

    private static NetAdapterInfo digNetworkInterface(NetworkInterface netcard) throws Exception {
        byte[] ha = netcard.getHardwareAddress();
        if (ha == null || ha.length != 6) {
            return null;
        }

        final String displayName = netcard.getDisplayName();
        if (MiscUtils.isWindows() && displayName.contains("VMware ")) {
            return null;
        }

        List<InterfaceAddress> intfAddrs = netcard.getInterfaceAddresses();
        InterfaceAddress firstIntfAddr = null;
        String ip = null;
        for (InterfaceAddress intfAddr : intfAddrs) {
            InetAddress addr = intfAddr.getAddress();
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isMulticastAddress()) {
                continue;
            }

            ip = addr.getHostAddress();
            if (ip.indexOf('.') > 0) {
                firstIntfAddr = intfAddr;
                break;
            }
        }

        String mac = String.format("%02x-%02x-%02x-%02x-%02x-%02x",
                ha[0], ha[1], ha[2], ha[3], ha[4], ha[5]);
        NetAdapterInfo o = new NetAdapterInfo();
        o.adapterName = displayName;
        o.hardwareAddress = mac;
        if (firstIntfAddr != null) {
            final short n = firstIntfAddr.getNetworkPrefixLength();
            o.ipv4Address = ip;
            o.ipv4SubnetMask = makeNetmask(n);
            o.ipv4Gateway = "127.0.0.1";
        }
        return o;
    }

    private static String makeNetmask(int networkPrefixLength) {
        int n = 0;
        for (int i=0; i<networkPrefixLength; ++i) {
            n |= (1 << (31-i) );
        }
        int a1 = (n >> 24) & 0xff;
        int a2 = (n >> 16) & 0xff;
        int a3 = (n >>  8) & 0xff;
        int a4 = (n >>  0) & 0xff;
        return String.format("%d.%d.%d.%d", a1, a2, a3, a4);
    }

    // 返回本机的MAC地址列表, 每个MAC地址形如: 4c-cc-6a-46-ae-9b
    public static ArrayList<String> getAllMacAddresses() throws Exception {
        ArrayList<String> result = new ArrayList<>();
        for (NetAdapterInfo adapter : getNetAdapters()) {
            if (adapter.hardwareAddress != null) {
                result.add(adapter.hardwareAddress);
            }
        }
        return result;
    }

    public static ArrayList<String> getAllIpv4Address() throws Exception {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> dhcpIPs = new ArrayList<>();
        for (NetAdapterInfo adapter : getNetAdapters()) {
            if (adapter.ipv4Address != null) {
                if (adapter.ipv4Address.startsWith("169.254.")) {
                    // 这是一个dhcp获取的地址
                    dhcpIPs.add(adapter.ipv4Address);
                } else {
                    result.add(adapter.ipv4Address);
                }
            }
        }

        // dhcp的ip排在后边
        result.addAll(dhcpIPs);

        return result;
    }

    public static String getLocalIPv4() {
        try {
            ArrayList<String> ips = getAllIpv4Address();
            if (ips.size() > 0) {
                String ip = ips.get(0);
                return ip;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    public static void close(SocketChannel c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    public static void parseIpAndPort(String httpURL, StringHolder outIp, IntHolder outPort) {
        //
        // 从 http://129.0.3.244:80/xx 中析出ip地址及端口号
        //
        String ip = null;
        int port = 80;
        int pos = httpURL.indexOf("://");
        if (pos < 0) {
            throw new IllegalStateException();
        }
        String str = httpURL.substring(pos + 3); //129.0.3.244:80/xx
        pos = str.indexOf('/');
        if (pos > 0) {
            str = str.substring(0, pos); //129.0.3.244:80
        }
        pos = str.indexOf(':');
        if (pos > 0) {
            ip = str.substring(0, pos);
            try {
                String tmp = str.substring(pos + 1);
                int a = Integer.parseInt(tmp);
                port = a;
            } catch (Exception e) {
                // no-op
            }
        } else {
            ip = str;
        }

        outIp.value = ip;
        outPort.value = port;
    }

    // 129.0.3.244 --> 整数
    public static int dottedIpToInt(String ip) {
        StringTokenizer tokenizer = new StringTokenizer(ip, ".");
        int i = 0;
        int n0=0, n1=0, n2=0, n3=0;
        for (; tokenizer.hasMoreTokens(); ++ i) {
            String tok = tokenizer.nextToken();
            switch (i) {
                case 0: n0 = Integer.parseInt(tok); break;
                case 1: n1 = Integer.parseInt(tok); break;
                case 2: n2 = Integer.parseInt(tok); break;
                case 3: n3 = Integer.parseInt(tok); break;
            }
        }
        if (i == 4) {
            return ((n0 << 24) & 0xff000000) + ((n1 << 16) & 0x00ff0000) + ((n2 << 8) & 0x0000ff00) + (n3 & 0xff);
        }
        return 0;
    }

    // 整数 --> 129.0.3.244
    public static String dottedIpFromInt(int n) {
        int n0 = (n >> 24) & 0xff;
        int n1 = (n >> 16) & 0xff;
        int n2 = (n >>  8) & 0xff;
        int n3 = (n >>  0) & 0xff;
        return String.format("%d.%d.%d.%d", n0, n1, n2, n3);
    }

    public static void swallowHttpError(HttpURLConnection conn) {
        try {
            InputStream es = conn.getErrorStream();
            if (es != null) {
                byte[] buf = new byte[1024];
                while (es.read(buf) > 0) {
                    ;
                }
                es.close();
            }
        }
        catch (IOException e) {
            ;
        }
    }

}
