package gtcloud.jobman.sched;

import gtcloud.common.ClusterNodeURL;
import gtcloud.common.basetypes.Options;
import gtcloud.common.cynosure.CynosureClient;
import gtcloud.common.utils.EnvUtils;
import gtcloud.common.utils.NodeId;
import gtcloud.common.utils.NodeStopper;
import gtcloud.jobman.core.scheduler.main.JobScheduler;
import gtcloud.springutils.SpringAppUtils;
import gtcloud.springutils.SpringEnvUtils;
import gtcloud.springutils.SpringUtilsExports;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import platon.ClassUtil;

@SpringBootApplication
@Import(SpringUtilsExports.class)
@Controller
public class JobSchedulerApplication {

    private static CynosureClient cynosureClient = null;

    // [--registerWithCynosure]
    public static void main(String[] rawArgs) throws Exception {
        // 若不是生产模式，则将关键环境变量设置成默认值
        EnvUtils.injectKeySystemProperties("gtjobsched");

        // 解析命令行参数
        Options cmdOptions = new Options();
        String[] args = SpringAppUtils.parseCmdLineArguments(rawArgs, cmdOptions);

        // 若是停止命令 ，则停止之前的调度器进程
        String symName = "GTCLOUD_JOBSCHEDULER_SID";
        String markerDir = ClassUtil.getClassURL(JobSchedulerApplication.class);
        final NodeId nodeId = new NodeId(markerDir, symName, "jobscheduler-");
        final NodeStopper nodeStopper = new NodeStopper(nodeId);
        for (String arg : args) {
            if (arg.equals("--shutdown") || arg.equals("--stop")) {
                nodeStopper.stopPreviousInstance();
                return;
            }
        }

        // 启动spring-boot主应用
        SpringApplication app = new SpringApplication(JobSchedulerApplication.class);
        app.addListeners((ApplicationListener<ContextClosedEvent>) event -> {
            //System.out.println("ContextClosedEvent received.");
            if (cynosureClient != null) {
                cynosureClient.dispose();
            }
        });
        app.setBannerMode(Banner.Mode.OFF);
        ConfigurableApplicationContext ctx = app.run(args);

        // 注册自己到CynosureServer上
        if (cmdOptions.getBool(SpringAppUtils.FLAG_RegisterWithCynosure, false)) {
            cynosureClient = registerWithCynosure(ctx.getEnvironment());
            ClusterNodeURL.injectCynosureClient(cynosureClient);
        }

        // 启动调度器
        JobSchedulerHelperImpl helper = new JobSchedulerHelperImpl(ctx.getEnvironment());
        JobScheduler scheduler = JobSchedulerHolder.value = new JobScheduler(helper);
        scheduler.run(args);

        // 等待退出通知
        nodeStopper.reset();
        nodeStopper.waitForStopEvent();

        // 关闭调度器及spring-boot应用
        scheduler.stop();
        ctx.close();
    }

    private static CynosureClient registerWithCynosure(ConfigurableEnvironment env) throws Exception {
        String localServiceAddress = SpringEnvUtils.figureOutLocalBaseURL(env);
        HashMap<String, String> params = null; //需要发布到cynosure的一些全局参数
        ArrayList<String> appNames = new ArrayList<>(); //需要发布到cynosure的应用名称
        appNames.add("JobScheduler");
        return CynosureClient.createInstance(localServiceAddress, appNames, params, null);
    }

    @GetMapping("/{viewName}.html")
    public ModelAndView getHtml(@PathVariable String viewName) {
        Map<String, Object> model = new HashMap<>();
        return new ModelAndView(viewName, model);
    }    

}
