package gtcloud.common.cynosure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.StatusCodeException;
import gtcloud.common.cynosure.pdo.AppInfoDO;
import gtcloud.common.cynosure.pdo.AppInfoListDO;

public class CynosureUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CynosureUtils.class);

    /**
     * 定位给定名称的app，返回其服务地址
     * @param client
     * @param appName
     * @return
     * @throws StatusCodeException
     */
    public static String tryDiscoverApp(CynosureClient client, String appName, CountDownLatch quitLatch) throws Exception {
        for (;;) {
            if (LOG.isInfoEnabled()) {
                LOG.info("尝试从CynosureServer处定位{}...", appName);
            }
            AppInfoListDO list = client.fetchAppList();
            for (AppInfoDO ai : list) {
                if (ai.getAppName().equalsIgnoreCase(appName)) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("从CynosureServer处定位{}完成, 其服务地址={}", appName, ai.getServiceAddress());
                    }
                    return ai.getServiceAddress();
                }
            }
            boolean quit = quitLatch.await(2000, TimeUnit.MILLISECONDS);
            if (quit) {
                return null;
            }
        }
    }

    public static CynosureClient waitForCynosureServerLocated() throws Exception {
        String localServiceAddress = "unknown";
        HashMap<String, String> params = null;
        ArrayList<String> appNames = null;
        return CynosureClient.createInstance(localServiceAddress, appNames, params, null);
    }

}
