package gtcloud.common.cynosure;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.SharedOkHttpClient;
import gtcloud.common.basetypes.ByteArray;
import gtcloud.common.basetypes.Options;
import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.basetypes.StatusCodeException;
import gtcloud.common.cynosure.pdo.AppInfoListDO;
import gtcloud.common.cynosure.pdo.AppStatusListDO;
import gtcloud.common.cynosure.pdo.FileListDO;
import gtcloud.common.cynosure.pdo.ParamDO;
import gtcloud.common.cynosure.pdo.ParamListDO;
import gtcloud.common.cynosure.pdo.ProbeAckDO;
import gtcloud.common.cynosure.pdo.ProbeReqDO;
import gtcloud.common.cynosure.pdo.ResultDO;
import gtcloud.common.nsocket.NetIoMessage;
import gtcloud.common.utils.ChannelUtils;
import gtcloud.common.utils.MiscUtils;
import gtcloud.common.utils.NetUtils;
import gtcloud.common.utils.PathUtils;
import gtcloud.common.utils.StreamUtils;
import okhttp3.OkHttpClient;
import platon.ByteStream;
import platon.DefreezeException;
import platon.JsonNode;

public class CynosureClient {

    private static final Logger LOG = LoggerFactory.getLogger(CynosureClient.class);

    // �ಥ���ַ����"239.0.0.0:35000"
    private InetSocketAddress groupSocketAddr = null;

    private DatagramChannel dgramChannel = null;

    private Selector dgramSelector = null;

    private ProbeReqDO probeReqDO = null;

    private long serverEpoch = 0;

    private volatile long heartbeatIntervalMillis = 60*1000L;

    // �������˵ķ����ַ
    private String serverEndpointBase = null;

    private long ackReceiveCount = 0;

    private final CountDownLatch serverLocatedLatch = new CountDownLatch(1);

    private final OkHttpClient httpClient;

    private volatile boolean disposed = false;

    private CynosureClient() {
        this.httpClient = SharedOkHttpClient.get().newBuilder()
                .connectTimeout(10*1000L, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * ����CynosureClientʵ���� ��������������ǰ�̣߳�ֱ���ɹ���λ����CynosureServer��
     *
     * @param localServiceAddress �ͻ��˱��صķ����ַ;
     * @param appNames �ͻ��˵�Ӧ�������б�;
     * @param params ��Ҫע�ᵽCynosureServer�ϵ�һЩ���ò���;
     * @param options ��չ�ò���.
     * @return CynosureClientʵ��.
     * @throws Exception �����������׳��쳣��
     */
    public static CynosureClient createInstance(String localServiceAddress,
                                                ArrayList<String> appNames,
                                                HashMap<String, String> params,
                                                Options options) throws Exception {
        CynosureClient client = new CynosureClient();
        client.locateServer(localServiceAddress, appNames, params, options);
        return client;
    }

    /**
     * �ͷ���Դ��
     */
    public void dispose() {
        this.disposed = true;
        ChannelUtils.close(this.dgramSelector);
        ChannelUtils.close(this.dgramChannel);
    }

    /**
     * ��CynosureServer����ȡȫ�����ò���
     * @throws StatusCodeException
     */
    public PropertiesEx fetchGlobalParams() throws StatusCodeException {
        PropertiesEx result = new PropertiesEx();
        ParamListDO paramList = fetchGlobalParams2();
        for (ParamDO p : paramList) {
            String k = p.getName();
            if (p.getValues().size() > 0) {
                String v = p.getValues().get(0);
                result.setProperty(k, v);
            }
        }
        return result;
    }

    /**
     * ��CynosureServer����ȡȫ�����ò�����ֱ����ȡ���������Ĳ�����
     *
     * �������ò������������ò����������첽�ģ������ò�����������������
     * ���û�ȡȫ�ֲ���ʱ�����ؽ���в�һ��������ϣ���Ĳ����������������������߿�
     * ��ǰ��֪ϣ����ȡ�Ĳ������������ײ�����ͬ������ֱ���������Щ���ò�����ŷ��ء�
     *
     * @param expectedParamNames ������ȡ�Ĳ��������֡�
     * @return
     * @throws StatusCodeException
     */
    public PropertiesEx waitAndFetchGlobalParams(ArrayList<String> expectedParamNames, CountDownLatch abortLatch) throws StatusCodeException {
        if (expectedParamNames == null || expectedParamNames.isEmpty()) {
            return fetchGlobalParams();
        }

        String namesStr = null;
        HashMap<String, Boolean> paramNameToState = new HashMap<>();
        for (String paramName : expectedParamNames) {
            paramNameToState.put(paramName, Boolean.FALSE);
            if (namesStr == null) {
                namesStr = paramName;
            } else {
                namesStr += ",";
                namesStr += paramName;
            }
        }

        for (;;) {
            if (LOG.isInfoEnabled()) {
                LOG.info("��CynosureServer��ȡ���ò���[{}]...", namesStr);
            }

            PropertiesEx globalParams = fetchGlobalParams();
            int count = 0;
            for (Entry<String, Boolean> e : paramNameToState.entrySet()) {
                String paramName = e.getKey();
                if (globalParams.containsKey(paramName)) {
                    count ++;
                }
            }
            if (count == expectedParamNames.size()) {
                // �����ȫ��ϣ����ȡ�Ĳ���
                if (LOG.isInfoEnabled()) {
                    LOG.info("��CynosureServer��ȡ���ò���[{}]���.", namesStr);
                }
                return globalParams;
            }

            // ��һ������
            try {
                boolean abort = abortLatch.await(2000, TimeUnit.MILLISECONDS);
                if (abort) {
                    return new PropertiesEx();
                }
            } catch (InterruptedException ex) {
                throw new StatusCodeException(-1, ex);
            }
        }
    }

    /**
     * ��CynosureServer����ȡȫ�����ò���
     * @throws StatusCodeException
     */
    public ParamListDO fetchGlobalParams2() throws StatusCodeException {
        try {
            JsonNode ackJson = doHttpGetForJson("/cynosure/v1/params?encode=gbk");
            ParamListDO paramList = new ParamListDO();
            paramList.defreezeFromJSON(ackJson);
            return paramList;
        }
        catch (StatusCodeException ex) {
            LOG.error("��ȡȫ�����ò�������: " + ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            LOG.error("��ȡȫ�����ò�������: " + ex.getMessage(), ex);
            throw new StatusCodeException(-2, ex);
        }
    }

    /**
     * ����ȫ�����ò�����������CynosureServer����
     * @param params �����õ����ò�������
     * @throws StatusCodeException
     */
    public void uploadGlobalParams(PropertiesEx params) throws StatusCodeException {
        ParamListDO paramList = new ParamListDO();
        for (Entry<Object, Object> e : params.entrySet()) {
            String k = (String)e.getKey();
            String v = (String)e.getValue();
            ParamDO p = new ParamDO();
            p.setName(k);
            p.getValues().add(v);
            paramList.add(p);
        }

        try {
            ByteStream blob = new ByteStream();
            paramList.freeze(blob);
            doHttpPostAndGetResultDO("/cynosure/v1/params?f=blob&encode=gbk", blob);
        }
        catch (StatusCodeException ex) {
            LOG.error("����ȫ�����ò�������: " + ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            LOG.error("����ȫ�����ò�������: " + ex.getMessage(), ex);
            throw new StatusCodeException(-2, ex);
        }
    }

    /**
     * ��CynosureServer����ȡApp�б�
     * @throws StatusCodeException
     */
    public AppInfoListDO fetchAppList() throws StatusCodeException {
        try {
            JsonNode ackJson = doHttpGetForJson("/cynosure/v1/apps?encode=gbk");
            AppInfoListDO appList = new AppInfoListDO();
            appList.defreezeFromJSON(ackJson);
            return appList;
        }
        catch (StatusCodeException ex) {
            LOG.error("��ȡApp�б����: " + ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            LOG.error("��ȡApp�б����: " + ex.getMessage(), ex);
            throw new StatusCodeException(-2, ex);
        }
    }

    /**
     * ��CynosureServer����ȡ�����ļ��б�
     * @throws StatusCodeException
     */
    public FileListDO fetchConfigFileList() throws StatusCodeException {
        try {
            JsonNode ackJson = doHttpGetForJson("/cynosure/v1/filelist?encode=gbk");
            FileListDO fileList = new FileListDO();
            fileList.defreezeFromJSON(ackJson);
            return fileList;
        }
        catch (StatusCodeException ex) {
            LOG.error("��ȡ�����ļ��б����: " + ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            LOG.error("��ȡ�����ļ��б����: " + ex.getMessage(), ex);
            throw new StatusCodeException(-2, ex);
        }
    }

    /**
     * ��ȡ���������ļ������ݡ�
     * @param fileName �ļ�������"/datastore/elasticsearch/esstore.xml"
     * @return �ļ����ݡ�
     * @throws StatusCodeException
     */
    public ByteArray fetchConfigFileContent(String fileName) throws StatusCodeException {
        try {
            return doHttpGet("/cynosure/v1/file?filename=" + fileName);
        }
        catch (StatusCodeException ex) {
            LOG.error("��ȡ�����ļ����ݴ���: " + ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            LOG.error("��ȡ�����ļ����ݴ���: " + ex.getMessage(), ex);
            throw new StatusCodeException(-2, ex);
        }
    }

    /**
     * ��ȡ����״̬�б�
     * @return ״̬���ݡ�
     * @throws StatusCodeException
     */
    public AppStatusListDO fetchAppStatusList() throws StatusCodeException {
        try {
            JsonNode ackJson = doHttpGetForJson("/cynosure/v1/appstatus?encode=gbk");
            AppStatusListDO appstatus_list = new AppStatusListDO();
            appstatus_list.defreezeFromJSON(ackJson);
            return appstatus_list;
        }
        catch (StatusCodeException ex) {
            LOG.error("��ȡ����״̬��������: " + ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            LOG.error("��ȡ����״̬��������:" + ex.getMessage(), ex);
            throw new StatusCodeException(-2, ex);
        }
    }

    /**
     * Ϊ���������ļ��ϴ��ļ����ݡ�
     * @param fileName �ļ�����
     * @param fileContent ���ϴ����ļ����ݡ�
     * @throws StatusCodeException
     */
    public void uploadConfigFileContent(String fileName, ByteStream fileContent) throws StatusCodeException {
        try {
            doHttpPostAndGetResultDO("/cynosure/v1/file?filename=" + fileName, fileContent);
        }
        catch (StatusCodeException ex) {
            LOG.error("���������ļ����ݴ���: " + ex.getMessage(), ex);
            throw ex;
        }
        catch (Exception ex) {
            LOG.error("���������ļ����ݴ���: " + ex.getMessage(), ex);
            throw new StatusCodeException(-2, ex);
        }
    }

    public long getHeartbeatIntervalMillis() {
        return this.heartbeatIntervalMillis;
    }

    private void locateServer(String localServiceAddress,
                              ArrayList<String> appNames,
                              HashMap<String, String> params,
                              Options options) throws Exception {
        //--------------------------
        // 1-������ַ������
        //--------------------------
        String groupName = null;
        String groupAddress = null;
        if (options != null) {
            groupName = options.getString("cynosure.groupName", null);
            groupAddress = options.getString("cynosure.groupAddress", null);

            String t = options.getString("cynosure.defaultClientHeartbeatIntervalMillis", null);
            if (t != null && t.length() > 0) {
                this.heartbeatIntervalMillis = Math.max(Long.parseLong(t), 10*1000L);
            }
        }

        if (isEmpty(groupName) || isEmpty(groupAddress)) {
            // �������ļ����ز���
            FileInputStream fis = null;
            Properties props = new Properties();
            String fileName = null;
            try {
                String etcDir = PathUtils.getEtcDir();
                fileName = etcDir + "/cynosure/main.properties";
                fis = new FileInputStream(fileName);
                props.load(fis);
            }
            catch (Exception ex) {
                LOG.error("���������ļ�ʧ��: fileName={}, error={}", fileName, ex.getMessage());
            }
            finally {
                StreamUtils.close(fis);
            }

            if (isEmpty(groupName)) {
                groupName = props.getProperty("cynosure.groupName");
                if (groupName.indexOf("${") >= 0) {
                    String ip = NetUtils.getLocalIPv4();
                    groupName = groupName.replace("${gtcloud.local-ip}", ip);
                }
            }
            if (isEmpty(groupAddress)) {
                groupAddress = props.getProperty("cynosure.groupAddress");
            }
        }
        if (isEmpty(groupName)) {
            throw new Exception("groupNameΪ��");
        }
        if (isEmpty(groupAddress)) {
            throw new Exception("groupAddressΪ��");
        }

        //-------------------------
        // 2-׼���鲥��ַ��̽���
        //-------------------------
        int pos = groupAddress.indexOf(':');
        if (pos < 0) {
            throw new Exception("��Ч�����ַ��Ӧ����ip:port");
        }
        String hostname = groupAddress.substring(0, pos).trim();
        String port = groupAddress.substring(pos+1).trim();
        this.groupSocketAddr = new InetSocketAddress(hostname, Integer.parseInt(port));
        this.probeReqDO = makeProbeRequest(localServiceAddress, appNames, params, groupName);

        //----------------------------------------------------------
        // 3-������̨�̣߳������������̽�ⱨ�ģ����շ������˵���Ӧ
        //----------------------------------------------------------
        this.dgramChannel = DatagramChannel.open();
        this.dgramSelector = Selector.open();
        Thread worker = new Thread(() -> {
            try {
                runSendAndReceiveLoop();
            } catch (ClosedSelectorException ex) {
                // ���̵߳�����dispose(), ���������˳�
                ;
            } catch (Exception ex) {
                LOG.error("runSendAndReceiveLoop() error", ex);
            }
        });
        worker.setName("CynosureClientWorker");
        worker.setDaemon(true);
        worker.start();

        //----------------------------------------
        // 4-�ȴ���ֱ����λ���˷�����
        //----------------------------------------
        if (LOG.isInfoEnabled()) {
            LOG.info("�ȴ�̽����������...");
        }
        this.serverLocatedLatch.await();
        if (LOG.isInfoEnabled()) {
            synchronized (this) {
                LOG.info("�ɹ�̽�⵽�˷�����, serverAddress={}", this.serverEndpointBase);
            }
        }
    }

    private void runSendAndReceiveLoop() throws Exception {
        Selector selector = this.dgramSelector;
        this.dgramChannel.configureBlocking(false);
        this.dgramChannel.register(selector, SelectionKey.OP_READ);

        // �����׸�̽�ⱨ��
        sendProbeRequest(0);

        // �ȴ�Ӧ�𣬶�ʱ����̽�ⱨ��
        for (long round=1;; round ++) {
            boolean serverLocated = this.serverLocatedLatch.getCount() == 0; //�Ƿ��Ѿ�̽�⵽��
            long waitMillis = serverLocated ? this.heartbeatIntervalMillis : 5*1000L;
            int readyCount = selector.select(waitMillis);
            if (readyCount == 0) {
                // ��ʱ, ���ͱ���
                sendProbeRequest(round);
                continue;
            }
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                if (key.isReadable()) {
                    // ���ձ���
                    handleProbeAck(round);
                }
                it.remove();
            }
        }
    }

    private void sendProbeRequest(long round) throws Exception {
        synchronized (this) {
            this.probeReqDO.setServerEpoch(this.serverEpoch);
        }

        ByteStream body = new ByteStream();
        {
            NetIoMessage msg = new NetIoMessage();
            body.writeInt(0); //placehodler
            body.writeByte(msg.getWireFormat());
            body.writeByte(msg.getDomainId());
            body.writeShort(msg.getFunctionId());
            int pos1 = body.write_pos();
            this.probeReqDO.freeze(body);
            int pos2 = body.write_pos();
            try {
                int bodyLen = pos2 - pos1;
                body.write_pos(0);
                body.writeInt(bodyLen);
            } finally {
                body.write_pos(pos2);
            }
        }

        String destAddr = this.groupSocketAddr.toString();
        if (LOG.isInfoEnabled() && (round % 100) == 0) {
            LOG.info("����̽������(groupAddress={}, groupName={}), round={}.", destAddr, this.probeReqDO.getGroupName(), round);
        }
        ByteBuffer buf = ByteBuffer.wrap(body.array(), 0, body.length());
        try {
            int nsend = this.dgramChannel.send(buf, this.groupSocketAddr);
            if (LOG.isDebugEnabled()) {
                LOG.debug("�ɹ�������{}�ֽڵ�̽�����{}", nsend, destAddr);
            }
        } catch (IOException ex) {
            if (!this.disposed) {
                LOG.error("����̽�����{}ʧ��: {}", destAddr, ex.getMessage());
            }
        }
    }

    private void handleProbeAck(long round) throws Exception {
        final byte[] buf = new byte[8*1024];
        ByteBuffer dest = ByteBuffer.wrap(buf);
        dest.clear();
        SocketAddress sender = this.dgramChannel.receive(dest);

        ByteStream bs = new ByteStream(buf, 0, buf.length);
        int bodyLen = bs.readInt();
        assert bodyLen <= buf.length - 8;
        bs.readByte();
        bs.readByte();
        bs.readShort();

        ProbeAckDO ack = new ProbeAckDO();
        try {
            ack.defreeze(bs);
        }
        catch (DefreezeException ex) {
            LOG.error("���probeAckʧ��, sender={}, error={}", sender.toString(), ex.getMessage());
            return;
        }

        // ����������ĵ�ַ
        synchronized (this) {
            String t = ack.getOptions().get("cynosure.clientHeartbeatIntervalMillis");
            if (t != null && t.length() > 0) {
                this.heartbeatIntervalMillis = Math.max(Long.parseLong(t), 10*1000L);
            }

            this.serverEpoch = ack.getServerEpoch();
            String addr = ack.getServerAddress();
            while (addr.endsWith("/")) {
                addr = addr.substring(0, addr.length()-1);
            }
            this.serverEndpointBase = addr;
            this.ackReceiveCount ++;
            if (ackReceiveCount == 1) {
                this.serverLocatedLatch.countDown();
            }

            if (LOG.isInfoEnabled() && (round % 100) == 0) {
                LOG.info("�յ�̽��Ӧ��, serverEndpointBase={}, ackReceiveCount={}, round={}.",
                        serverEndpointBase,
                        ackReceiveCount,
                        round);
            }
        }
    }

    private static ProbeReqDO makeProbeRequest(String localServiceAddress,
                                               ArrayList<String> appNames,
                                               HashMap<String, String> params,
                                               String groupName) {
        ProbeReqDO probeReq = new ProbeReqDO();
        probeReq.setGroupName(groupName);
        probeReq.setServerEpoch(0);
        probeReq.setClientAddress(localServiceAddress);
        probeReq.setClientPid(MiscUtils.getpid());
        if (appNames != null) {
            for (String appName : appNames) {
                probeReq.getClientAppNames().add(appName);
            }
        }
        if (params != null) {
            probeReq.getClientParams().putAll(params);
        }
        return probeReq;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private JsonNode doHttpGetForJson(String url) throws Exception {
        String endpoint = buildEndpoint(url);
        return MiscUtils.doHttpGetForJson(this.httpClient, endpoint);
    }

    private ByteArray doHttpGet(String url) throws Exception {
        String endpoint = buildEndpoint(url);
        return MiscUtils.doHttpGet(httpClient, endpoint);
    }

    private ResultDO doHttpPostAndGetResultDO(String url, ByteStream blobToPost) throws Exception {
        ByteArray ba = doHttpPost(url, blobToPost);
        ByteArrayInputStream is = new ByteArrayInputStream(ba.array, ba.offset, ba.length);
        JsonNode jsonNode = JsonNode.parseJsonDoc(is, "gbk");
        ResultDO r = new ResultDO();
        r.defreezeFromJSON(jsonNode);
        if (r.getStatusCode() != 0) {
            throw new StatusCodeException(r.getStatusCode(), r.getStatusMessage());
        }
        return r;
    }

    private ByteArray doHttpPost(String url, ByteStream blobToPost) throws Exception {
        String endpoint = buildEndpoint(url);
        return MiscUtils.doHttpPost(httpClient, endpoint, blobToPost);
    }

    private String buildEndpoint(String url) {
        String endpointBase = null;
        synchronized (this) {
            if (this.serverEndpointBase == null) {
                throw new IllegalStateException();
            }
            endpointBase = this.serverEndpointBase;
        }
        String endpoint = endpointBase + url;
        return endpoint;
    }
}

