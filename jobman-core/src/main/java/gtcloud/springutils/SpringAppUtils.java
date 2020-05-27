package gtcloud.springutils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import gtcloud.common.basetypes.Options;
import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.cynosure.CynosureClient;
import gtcloud.common.cynosure.CynosureUtils;
import gtcloud.common.utils.PathUtils;

public class SpringAppUtils {

    //private static final Logger LOG = LoggerFactory.getLogger(SpringAppUtils.class);

    public static final String FLAG_RegisterWithCynosure = "registerWithCynosure";
    private static final String FLAG_AutoDiscoveryDglmServer = "autoDiscoveryDglmServer";
    private static final String FLAG_AutoDiscoveryAuth2Server = "autoDiscoveryAuth2Server";
    private static final String FLAG_AutoDiscoveryEurekaServer = "autoDiscoveryEurekaServer";
    private static final String FLAG_AutoDiscoveryJobScheduler = "autoDiscoveryJobScheduler";
    private static final String FLAG_AutoDiscoveryAmqConfig = "autoDiscoveryAmqConfig";
    private static final String FLAG_AutoDiscoveryDataStoreConfig = "autoDiscoveryDataStoreConfig";

    private static final String[] wellknownFlags = new String[] {
        FLAG_RegisterWithCynosure,
        FLAG_AutoDiscoveryDglmServer,
        FLAG_AutoDiscoveryAuth2Server,
        FLAG_AutoDiscoveryEurekaServer,
        FLAG_AutoDiscoveryJobScheduler,
        FLAG_AutoDiscoveryAmqConfig,
        FLAG_AutoDiscoveryDataStoreConfig,
    };

    public static String[] parseCmdLineArguments(String[] rawArgs, Options outOptions) {
        ArrayList<String> argv = new ArrayList<>();
        for (String arg : rawArgs) {
            boolean argUsed = false;
            for (String flag : wellknownFlags) {
                String f1 = "--" + flag;
                String f2 = "--" + flag + "=1";
                if (f1.equalsIgnoreCase(arg) || f2.equalsIgnoreCase(arg)) {
                    outOptions.setBool(flag, true);
                    argUsed = true;
                    break;
                }
            }
            if (!argUsed) {
                argv.add(arg);
            }
        }
        return argv.toArray(new String[0]);
    }

    /**
     * 等待依赖的服务或配置参数就绪
     * @param cmdOptions
     * @throws Exception
     */
    public static void waitForDependentServerAndConfigurationsReady(Options cmdOptions) throws Exception {
        CynosureClient tempCynosureClient = CynosureUtils.waitForCynosureServerLocated();
        if (tempCynosureClient == null) {
            return;
        }
        tempCynosureClient.dispose();
    }

    private static void waitForDglmServerDiscovered(CynosureClient client, CountDownLatch quitLatch) throws Exception {
        // 此处"DglmServer"得与cpp端注册的名字保持一致
        String serviceAddr = CynosureUtils.tryDiscoverApp(client, "DglmServer", quitLatch);
        if (serviceAddr != null) {
            System.setProperty("GTCLOUD_DGLM_SERVER_URL", serviceAddr);
            System.setProperty("GTCLOUD_DGLM_REST_URL", serviceAddr);
        }
    }

    private static void waitForAuth2ServerDiscovered(CynosureClient client, CountDownLatch quitLatch) throws Exception {
        // 此处"Auth2Server"得与cpp端注册的名字保持一致
        String serviceAddr = CynosureUtils.tryDiscoverApp(client, "Auth2Server", quitLatch);
        if (serviceAddr != null) {
            System.setProperty("GTCLOUD_AUTH2_SERVER_URL", serviceAddr);
        }
    }

    private static void waitForEurekaServerDiscovered(CynosureClient client, CountDownLatch quitLatch) throws Exception {
        final String KEY_EurekaDefaultZone  = "eureka.client.serviceUrl.defaultZone";
        ArrayList<String> expectedParamNames = new ArrayList<>();
        expectedParamNames.add(KEY_EurekaDefaultZone);
        PropertiesEx globalParams = client.waitAndFetchGlobalParams(expectedParamNames, quitLatch);
        if (globalParams != null) {
            String zone = globalParams.getProperty(KEY_EurekaDefaultZone);
            System.setProperty("gtcloud.eurekaClientServiceUrlDefaultZone", zone);

            int i = globalParams.getProperty_Int("eureka.instance.lease-renewal-interval-in-seconds", 30);
            System.setProperty("gtclout.eurekaInsanceLeaseRenewalIntervalInSeconds", i + "");
        }
    }

    private static void waitForJobSchedulerDiscovered(CynosureClient client, CountDownLatch quitLatch) throws Exception {
        // 此处"JobScheduler"得与提供者注册的名字保持一致
        String serviceAddr = CynosureUtils.tryDiscoverApp(client, "JobScheduler", quitLatch);
        if (serviceAddr != null) {
            System.setProperty("gtcloud.jobmanSchedulerBaseURL", serviceAddr);
        }
    }

    // 等待ActiveMQ的配置参数被发现
    private static void waitForAmqConfigDiscovered(CynosureClient client, CountDownLatch quitLatch) throws Exception {
        // extra.tools CynosureDiscovery 已经将配置文件写到本地了，我们使用即可
        File of = new File(PathUtils.getVarDir() + "/cynosure_activemq.properties");
        PropertiesEx.GLOBAL.setProperty("gtcloud.activemqConfigFile", of.getAbsolutePath());
    }

    // 等待DataStore的配置参数被发现
    // /datastore/__last.txt
    private static void waitForDataStoreConfigDiscovered(CynosureClient client, CountDownLatch quitLatch) throws Exception {
        // extra.tools CynosureDiscovery 已经将配置文件写到本地了，我们使用即可
        File localDataStoreDir = new File(PathUtils.getVarDir() + "/cynosure_datastore");
        PropertiesEx.GLOBAL.setProperty("GTCLOUD_DATASTORE_CONFIG_DIR", localDataStoreDir.getAbsolutePath());
    }

}
