package gtcloud.common.utils;

import java.security.MessageDigest;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeId {

    private static Logger LOG = LoggerFactory.getLogger(NodeId.class);

    private String value = null;

    // instanceMarkerDir: ����Ψһ��ʶ��ǰʵ����·��
    // instanceSymbolName: ��GTCLOUD_SUBJOBPROC_SID
    // nodeIdPrefix: ��subjobproc-
    public NodeId(String instanceMarkerDir, String instanceSymbolName, String nodeIdPrefix) {
        final String instanceId = System.getProperty(instanceSymbolName);
        String partialNodeId;
		try {
			partialNodeId = generatePartialNodeId(instanceMarkerDir, instanceId);
		} catch (Exception e) {
			partialNodeId = instanceMarkerDir + "-" + instanceId;
		}
        this.value = makeCurrentNodeId(nodeIdPrefix, partialNodeId);
    }

    @Override
    public String toString() {
        return this.value;
    }

    // ���ɵ�ǰ�ڵ�Ĳ���nodeId
    private static String generatePartialNodeId(final String instanceMarkerDir, final String instanceId) throws Exception {
        byte[] data = instanceMarkerDir.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data, 0, data.length);
        byte[] hex = Base16.encode(md.digest());
        String prefix = (new String(hex)).toLowerCase().substring(0, 16);
        if (instanceId == null) {
            return prefix + "-d";
        } else {
            return prefix + "-" + instanceId;
        }
    }

    static private String makeCurrentNodeId(String nodeIdPrefix, String partialNodeId) {
        try {
            ArrayList<String> v4ips = NetUtils.getAllIpv4Address();
            if (v4ips.size() > 0) {
                return nodeIdPrefix + v4ips.get(0) + "-" + partialNodeId;
            }
        } catch (Exception e) {
            LOG.error("get ip failed: " + e);
            //e.printStackTrace();
        }
        return nodeIdPrefix + partialNodeId;
    }
}
