package gtcloud.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import gtcloud.common.basetypes.ByteArray;
import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.cynosure.CynosureClient;
import gtcloud.common.cynosure.pdo.AppInfoListDO;
import gtcloud.common.cynosure.pdo.FileListDO;
import gtcloud.common.utils.NetUtils;
import platon.ByteStream;
import platon.JsonNode;

public class CynosureClientTest {

    public static void main(String[] args) throws Exception {
        //-----------------------------
        // 获得本地ip地址，放入系统属性中
        //-----------------------------
        try {
            ArrayList<String> ips = NetUtils.getAllIpv4Address();
            if (ips.size() > 0) {
                String ip = ips.get(0);
                System.setProperty("gtcloud.local-ip", ip);
            } else {
                System.setProperty("gtcloud.local-ip", "127.0.0.1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String localServiceAddress = "http://129.0.3.60:44848";
        ArrayList<String> appNames = new ArrayList<>();
        appNames.add("BzkMaster");
        HashMap<String, String> params = new HashMap<>();
        params.put("BzkMaster.ip", "129.0.3.44");

        CynosureClient client = CynosureClient.createInstance(localServiceAddress, appNames, params, null);

        if (true) {
            PropertiesEx newParams = new PropertiesEx();
            newParams.setProperty("somebody.name", "张三");
            newParams.setProperty_Int("somebody.age", 44);
            client.uploadGlobalParams(newParams);

            PropertiesEx globalParams = client.fetchGlobalParams();
            globalParams.list(System.out);
        }

        if (true) {
            AppInfoListDO appList = client.fetchAppList();
            JsonNode jsonNode = appList.freezeToJSON();
            String jsonBody = jsonNode.toPrettyString(2, 2);
            System.out.println(jsonBody);
        }

        if (true) {
            ByteStream fileContent = new ByteStream();
            fileContent.writeString("a1");
            fileContent.writeString("a2");
            client.uploadConfigFileContent("/config/mytest.blob", fileContent);

            FileListDO fileList = client.fetchConfigFileList();
            JsonNode jsonNode = fileList.freezeToJSON();
            String jsonBody = jsonNode.toPrettyString(2, 2);
            System.out.println(jsonBody);

            ByteArray ba = client.fetchConfigFileContent("/config/mytest.blob");
            ByteStream newContent = new ByteStream(ba.array, ba.offset, ba.length);
            String s1 = newContent.readString();
            String s2 = newContent.readString();
            if (!s1.equals("a1")) {
                System.out.println("ASSERT 1 failed.");

            }
            if (!s2.equals("a2")) {
                System.out.println("ASSERT 2 failed.");

            }

        }

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        client.dispose();
        System.out.println("all done.");
    }

}
