package gtcloud.jobman.proc;

import gtcloud.common.ClusterNodeURL;
import gtcloud.common.basetypes.Options;
import gtcloud.common.cynosure.CynosureClient;
import gtcloud.common.utils.EnvUtils;
import gtcloud.common.utils.NodeId;
import gtcloud.common.utils.NodeStopper;
import gtcloud.jobman.core.processor.main.LocalProccessNode;
import gtcloud.springutils.SpringAppUtils;
import gtcloud.springutils.SpringEnvUtils;
import gtcloud.springutils.SpringUtilsExports;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import platon.ClassUtil;

@SpringBootApplication
@Import(SpringUtilsExports.class)
public class JobProcessorApplication {

    // [--registerWithCynosure]
    public static void main(String[] args) {
        try {
            main_i(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(13);
        }
    }

    private static CynosureClient cynosureClient = null;

    public static void main_i(String[] rawArgs) throws Exception {
        // ����������ģʽ���򽫹ؼ������������ó�Ĭ��ֵ
        EnvUtils.injectKeySystemProperties("gtjobproc");

        // ���������в���
        Options cmdOptions = new Options();
        String[] args = SpringAppUtils.parseCmdLineArguments(rawArgs, cmdOptions);

        // ����ֹͣ�����ֹ֮ͣǰ�Ĵ���������
        String symName = "GTCLOUD_SUBJOBPROC_SID";
        String markerDir = ClassUtil.getClassURL(JobProcessorApplication.class);
        final NodeId nodeId = new NodeId(markerDir, symName, "subjobproc-");
        final NodeStopper nodeStopper = new NodeStopper(nodeId);
        for (String arg : args) {
            if (arg.equals("--shutdown") || arg.equals("--stop")) {
                nodeStopper.stopPreviousInstance();
                return;
            }
        }

        // ����spring-boot��Ӧ��
        SpringApplication app = new SpringApplication(JobProcessorApplication.class);
        app.addListeners((ApplicationListener<ContextClosedEvent>) event -> {
            //System.out.println("ContextClosedEvent received.");
            if (cynosureClient != null) {
                cynosureClient.dispose();
            }
        });
        app.setBannerMode(Banner.Mode.OFF);
        ConfigurableApplicationContext ctx = app.run(args);

        // ע���Լ���CynosureServer��
        if (cmdOptions.getBool(SpringAppUtils.FLAG_RegisterWithCynosure, false)) {
            cynosureClient = registerWithCynosure(ctx.getEnvironment());
            ClusterNodeURL.injectCynosureClient(cynosureClient);
        }

        // ����������
        CountDownLatch latch = nodeStopper.getStopLatch();
        ConfigurableEnvironment env = ctx.getEnvironment();
        LocalProccessNodeHelperImpl helper = new LocalProccessNodeHelperImpl(env);
        LocalProccessNode node = LocalProccessNodeHolder.value = new LocalProccessNode(helper, nodeId, latch);
        node.run(args);

        // �ȴ��˳�֪ͨ
        nodeStopper.reset();
        nodeStopper.waitForStopEvent();

        // �رմ���spring-bootӦ��
        node.stop();
        ctx.close();
    }

    private static CynosureClient registerWithCynosure(ConfigurableEnvironment env) throws Exception {
        String localServiceAddress = SpringEnvUtils.figureOutLocalBaseURL(env);
        HashMap<String, String> params = null; //��Ҫ������cynosure��һЩȫ�ֲ���
        ArrayList<String> appNames = new ArrayList<>(); //��Ҫ������cynosure��Ӧ������
        appNames.add("JobProcessor");
        return CynosureClient.createInstance(localServiceAddress, appNames, params, null);
    }
}
