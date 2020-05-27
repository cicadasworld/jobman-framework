package gtcloud.jobman.core.scheduler.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gtcloud.common.basetypes.PropertiesEx;
import gtcloud.common.utils.FileUtils;
import gtcloud.common.utils.MiscUtils;
import gtcloud.jobman.core.pdo.JobControlBlockDO;
import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import gtcloud.jobman.core.scheduler.JobEntry;
import gtcloud.jobman.core.scheduler.JobPersister;
import gtcloud.jobman.core.scheduler.JobState;
import gtcloud.jobman.core.scheduler.LinesConsumer;
import gtcloud.jobman.core.scheduler.SubjobEntry;
import gtcloud.jobman.core.scheduler.event.SubjobBaseEvent;
import platon.FreezerJSON;
import platon.JsonNode;
import platon.LongHolder;

/**
 * 该类负责将作业数据写入到本地文件中，或从本地文件中读取作业数据。
 */
public class FlatJobPersister implements JobPersister {

    private static final Logger LOG = LoggerFactory.getLogger(FlatJobPersister.class);

    // 作业持久化后的数据文件所在的第一层目录的全路径
    private final File jobDbTopDir;

    // 已经执行完成的作业归档前在"finished目录"保保留几天? 之后会移动到"archived目录"
    private final long finishedJobKeepDays;

    // 上次归档的时刻, 上次整理pending目录的时刻
    private long whenLastArchive = 0;
    private long whenLastMoveFinishedJobFromPendingDir = 0;

    // 第二层目录名
    private static final String LEVEL2DIR_Archived = "Archived"; //该目录下存放7天之前已经完成的作业
    private static final String LEVEL2DIR_Finished = "Finished"; //该目录下存放7天之内已经完成的作业
    private static final String LEVEL2DIR_Pending  = "Pending";  //该目录下存放还未完成的作业

    private static class JobCookie {
        final File jobDir;
        JobCookie(File jobDir) {
            this.jobDir = jobDir;
        }
        JobCookie(String jobDirName) {
            this.jobDir = new File(jobDirName);
        }
    }

    private static class SubjobCookie {
        final File subjobDir;
        SubjobCookie(File subjobDir) {
            this.subjobDir = subjobDir;
        }
        SubjobCookie(String subjobDirName) {
            this.subjobDir = new File(subjobDirName);
        }
    }

    public FlatJobPersister(String jobDbTopDir, long finishedJobKeepDays) {
        String path = jobDbTopDir
            .replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);
        this.jobDbTopDir = new File(path);
        this.finishedJobKeepDays = finishedJobKeepDays;
    }

    /**
     * 将给定的新作业写入到文件中。
     * @param job 待写入的新作业。
     */
    public void insertNewJob(JobEntry job) throws Exception {
        //-------------------------------------------------------------------------------
        // 确定作业存放路径: ${jobDbTopDir}/pending/${jobCategory}/${timestamp}-${jobId}
        //-------------------------------------------------------------------------------
        final JobControlBlockDO jcb = job.getControlBlock();
        final long now = System.currentTimeMillis();
        final String timestamp = MiscUtils.formatDateTime(now, "%04d%02d%02d%02d%02d%02d");
        final String relativePath = String.format("%s/%s/%s-%s",
            LEVEL2DIR_Pending,
            jcb.getJobCategory(),
            timestamp,
            jcb.getJobId()).replace('/', File.separatorChar);
        final File jobDir = new File(this.jobDbTopDir, relativePath);
        if (!jobDir.exists()) {
            jobDir.mkdirs();
        }
        if (!jobDir.exists()) {
            String emsg = "创建目录失败: " + jobDir.getAbsolutePath();
            LOG.error(emsg);
            throw new IOException(emsg);
        }
        job.setPersisterCookie(new JobCookie(jobDir));

        //----------------------
        // 写作业控制块到文件中
        //----------------------
        try {
            File jobcbFile = new File(jobDir, "job_cb.json");
            JsonNode jsonNode = jcb.freezeToJSON();
            writeJsonFile(jobcbFile, jsonNode);
        }
        catch (Exception ex) {
            LOG.error("写作业控制块到文件过程中发生错误", ex);
            throw ex;
        }

        //---------------------
        // 写各个子作业到文件中
        //---------------------
        for (SubjobEntry subjob : job.getSubjobs()) {
            writeSubjob(jobDir, subjob);
        }
    }

    private void writeSubjob(final File jobDir, final SubjobEntry subjob) throws Exception {
        //----------------------
        // 确定该子作业的顶级目录
        //----------------------
        SubjobControlBlockDO subcb = subjob.getSubjobCB();
        String subjobKey = String.format("%06d-%06d", subcb.getSubjobCount(), subcb.getSubjobSeqNo());
        File subjobDir = new File(jobDir, subjobKey);
        subjobDir.mkdirs();
        if (!subjobDir.exists()) {
            String emsg = "创建目录失败: " + subjobDir.getAbsolutePath();
            LOG.error(emsg);
            throw new IOException(emsg);
        }
        subjob.setPersisterCookie(new SubjobCookie(subjobDir));

        //-----------------------
        // 写子作业控制块到文件中
        //-----------------------
        try {
            File subjobcbFile = new File(subjobDir, "subjob_cb.json");
            JsonNode jsonNode = subcb.freezeToJSON();
            writeJsonFile(subjobcbFile, jsonNode);
        }
        catch (Exception ex) {
            LOG.error("写子作业控制块到文件过程中发生错误", ex);
            throw ex;
        }

        //-----------------------------
        // 写子作业数据体(BLOB)到文件中
        //-----------------------------
        try {
            File subjobBodyFile = new File(subjobDir, "subjob_body.blob");
            byte[] body = subjob.getBodyBytes();
            try (FileOutputStream fos = new FileOutputStream(subjobBodyFile)) {
                fos.write(body);
            }
        }
        catch (Exception ex) {
            LOG.error("写子作业数据体(BLOB)到文件过程中发生错误", ex);
            throw ex;
        }

        //------------------------------------------
        // 写子作业数据体(JSON)到文件中, 仅用于调试
        //------------------------------------------
        Object bodydo = subjob.getBodyDO();
        if (bodydo != null && bodydo instanceof FreezerJSON) {
            try {
                File subjobJsonFile = new File(subjobDir, "subjob_body.json");
                JsonNode jsonNode = ((FreezerJSON)bodydo).freezeToJSON();
                writeJsonFile(subjobJsonFile, jsonNode);
            }
            catch (Exception ex) {
                LOG.error("写子作业数据体(JSON)到文件过程中发生错误", ex);
                throw ex;
            }
        }

        //--------------------------
        // 写子作业其它属性到文件中
        //--------------------------
        try {
            File subjobPropFile = new File(subjobDir, "subjob_props.json");
            JsonNode jsonNode = subjob.getProps().freezeToJSON();
            writeJsonFile(subjobPropFile, jsonNode);
        }
        catch (Exception ex) {
            LOG.error("写子作业其它属性到文件过程中发生错误", ex);
            throw ex;
        }
    }

    /**
     * 从文件中加载作业。
     * @param pendingJobs 输出参数，返回还未完成的作业，其中Map的Key为jobId;
     * @param finishedJobs 输出参数，返回7天之内完成的作业，其中Map的Key为jobId.
     */
    public void readJobs(Map<String, JobEntry> pendingJobs, Map<String, JobEntry> finishedJobs) {
        // 读之前维护一下
        doPeriodicalWork();

        // 加载已经完成的作业
        readJobsInLevel2Directory(LEVEL2DIR_Finished, finishedJobs);

        // 加载尚未完成的作业
        readJobsInLevel2Directory(LEVEL2DIR_Pending, pendingJobs);
    }

    /**
     * 从${jobDbTopDir}/${level2dirName}下加载各个作业
     * @param level2dirName 第二级目录名，如"pending"等
     * @param jobs 输出参数
     */
    private void readJobsInLevel2Directory(String level2dirName, Map<String, JobEntry> jobs) {
        File level2dir = new File(this.jobDbTopDir, level2dirName);
        File[] cateDirs = level2dir.listFiles();
        if (cateDirs == null || cateDirs.length == 0) {
            return;
        }

        // 遍历每个"作业类别"目录
        for (File cateDir : cateDirs) {
            readJobsInJobCategoryDirectory(cateDir, jobs);
        }
    }

    private void readJobsInJobCategoryDirectory(File cateDir, Map<String, JobEntry> jobs) {
        File[] jobDirs = cateDir.listFiles();
        if (jobDirs == null || jobDirs.length == 0) {
            return;
        }

        // 遍历每个"作业"目录
        for (File jobDir : jobDirs) {
            JobEntry job = readJob_i(jobDir);
            if (job != null) {
                jobs.put(job.getControlBlock().getJobId(), job);
            }
        }
    }

    private JobEntry readJob_i(File jobDir) {
        try {
            // 读"作业控制块"
            File jobcbFile = new File(jobDir, "job_cb.json");
            JsonNode jsonNode = JsonNode.parseJsonFile(jobcbFile.getAbsolutePath());
            JobControlBlockDO jcb = new JobControlBlockDO();
            jcb.defreezeFromJSON(jsonNode);

            // 读各个"子作业"
            ArrayList<SubjobEntry> subjobs = new ArrayList<>();
            do {
                File[] subjobDirs = jobDir.listFiles();
                if (subjobDirs == null || subjobDirs.length == 0) {
                    break;
                }
                for (File subjobDir : subjobDirs) {
                    if (!subjobDir.isDirectory()) {
                        continue;
                    }
                    SubjobEntry subjob = readSubjob_i(subjobDir);
                    subjobs.add(subjob);
                }
            }
            while (false);


            LongHolder whenFinished = new LongHolder();
            JobState jobState = readJobState_i(jobDir, whenFinished); //读"作业完成状态"
            JobEntry job = new JobEntry(this, jcb, subjobs, jobState, jobcbFile.lastModified());
            job.setPersisterCookie(new JobCookie(jobDir));
            if (whenFinished.value > 0) {
                job.setFinishedTime(whenFinished.value);
            }

            return job;

        } catch (Exception ex) {
            String emsg = String.format("从%s目录下读作业数据错误", jobDir.getAbsolutePath());
            LOG.error(emsg, ex);
            return null;
        }
    }

    private SubjobEntry readSubjob_i(File subjobDir) throws Exception {
        // 读子作业完成状态
        final LongHolder whenFinished = new LongHolder();
        final JobState state = readJobState_i(subjobDir, whenFinished);

        // 读子作业控制块
        final SubjobEntry subjob = new SubjobEntry(this, state);
        File subjobcbFile = new File(subjobDir, "subjob_cb.json");
        JsonNode jcbJsonNode = JsonNode.parseJsonFile(subjobcbFile.getAbsolutePath());
        subjob.getSubjobCB().defreezeFromJSON(jcbJsonNode);

        // 读子作业数据体(BLOB)
        // 需要时才即时加载, 见readSubjobBodyBytes()

        // 读子作业其它属性
        File subjobPropFile = new File(subjobDir, "subjob_props.json");
        JsonNode propsJsonNode = JsonNode.parseJsonFile(subjobPropFile.getAbsolutePath());
        subjob.getProps().defreezeFromJSON(propsJsonNode);

        // 读子作业事件日志
        // 需要时才即时加载, 见readSubjobEventLogs()

        subjob.setPersisterCookie(new SubjobCookie(subjobDir));

        // 子作业完成时刻
        if (whenFinished.value > 0) {
            subjob.setFinishedTime(whenFinished.value);
        }

        return subjob;
    }

    /**
     * 执行周期性处理，包括"已完成作业归档"等。
     */
    public void doPeriodicalWork() {
        final long now = System.currentTimeMillis();
        final long elapsedMillis1 = now - this.whenLastArchive;
        final long ONE_DAYS = 24*3600*1000L;
        if (elapsedMillis1 >= ONE_DAYS) {
            if (LOG.isInfoEnabled()) {
                LOG.info("开始archive()...");
            }
            archive();
            this.whenLastArchive = now;
            if (LOG.isInfoEnabled()) {
                LOG.info("结束archive().");
            }
        }

        // 将还位于pending目录下、但已经完成的作业移动到finished目录下
        final long elapsedMillis2 = now - this.whenLastMoveFinishedJobFromPendingDir;
        final long FIVE_MINUTES = 5*60*1000L;
        if (elapsedMillis2 >= FIVE_MINUTES) {
            moveFinishedJobFromPendingDir();
            this.whenLastMoveFinishedJobFromPendingDir = now;
        }
    }

    /**
     * 将给定天数以前完成的作业移动到归档目录下。
     */
    private void archive() {
        final File finishedDir = new File(this.jobDbTopDir, LEVEL2DIR_Finished);
        File[] cateDirs = finishedDir.listFiles();
        if (cateDirs == null || cateDirs.length == 0) {
            return;
        }

        final File archivedDir = new File(this.jobDbTopDir, LEVEL2DIR_Archived);

        // 遍历每个"作业类别"目录
        final long T0 = System.currentTimeMillis() - (this.finishedJobKeepDays * 24*3600*1000L);
        for (File cateDir : cateDirs) {
            File[] jobDirs = cateDir.listFiles();
            if (jobDirs == null || jobDirs.length == 0) {
                continue;
            }

            // 遍历每个"作业"目录
            for (File jobDir : jobDirs) {
                JobState jobState = readJobState_i(jobDir, null);
                if (jobState == JobState.PENDING) {
                    // 该作业还未执行完
                    continue;
                }

                // 是否已经超过了给定天数
                File jobcbFile = new File(jobDir, "job_cb.json");
                long jobBornTime = jobcbFile.lastModified();
                if (jobBornTime > T0) {
                    continue;
                }

                // 移动目录
                moveJobDir_ii(finishedDir/*from*/, archivedDir/*to*/, jobDir);
            }
        }
    }

    /**
     * 将还位于pending目录下、但已经完成的作业移动到finished目录下
     */
    private void moveFinishedJobFromPendingDir() {
        final File pedingDir = new File(this.jobDbTopDir, LEVEL2DIR_Pending);
        File[] cateDirs = pedingDir.listFiles();
        if (cateDirs == null || cateDirs.length == 0) {
            return;
        }

        // 遍历每个"作业类别"目录
        final File finishedDir = new File(this.jobDbTopDir, LEVEL2DIR_Finished);
        for (File cateDir : cateDirs) {
            File[] jobDirs = cateDir.listFiles();
            if (jobDirs == null || jobDirs.length == 0) {
                continue;
            }
            for (File jobDir : jobDirs) { //遍历每个"作业"目录
                 JobState jobState = readJobState_i(jobDir, null);
                 if (jobState != JobState.PENDING) {
                     moveJobDir_ii(pedingDir/*from*/, finishedDir/*to*/, jobDir);
                 }
            }
        }
    }

    private static JobState readJobState_i(File currentDir, LongHolder whenFinished) {
        File successFile = new File(currentDir, "finistate.success");
        if (successFile.exists()) {
            if (whenFinished != null) {
                whenFinished.value = readJobFinishTimestamp_i(successFile);
            }
            return JobState.FINISHED_SUCCESS;
        }

        File faileFile = new File(currentDir, "finistate.failure");
        if (faileFile.exists()) {
            if (whenFinished != null) {
                whenFinished.value = readJobFinishTimestamp_i(faileFile);
            }
            return JobState.FINISHED_FAILED;
        }

        return JobState.PENDING;
    }

    private static void writeJobState_i(File currentDir, JobState state) {
        File destFile;
        if (state == JobState.FINISHED_SUCCESS) {
            destFile = new File(currentDir, "finistate.success");
        }
        else if (state == JobState.FINISHED_FAILED) {
            destFile = new File(currentDir, "finistate.failure");
        } else {
            return;
        }

        try (PrintStream ps = new PrintStream(destFile)) {
            final long now = System.currentTimeMillis();
            String nowStr = MiscUtils.formatDateTime(now);
            ps.println("#" + nowStr);
            ps.println("timestamp=" + now);
        } catch (FileNotFoundException ex) {
            LOG.error("write finistate file error", ex);
        }
    }

    private static long readJobFinishTimestamp_i(File finishStateFile) {
        try (FileInputStream in = new FileInputStream(finishStateFile)) {
            PropertiesEx p = new PropertiesEx();
            p.load(in);
            long ts = p.getProperty_Long("timestamp", 0);
            if (ts > 0) {
                return ts;
            }
        } catch (IOException e) {
            ;
        }
        return finishStateFile.lastModified();
    }

    private static void writeJsonFile(File destFile, JsonNode jsonNode) throws Exception {
        String jsonText = jsonNode.toPrettyString(2, 2);
        PrintWriter pw = new PrintWriter(destFile);
        pw.print(jsonText);
        pw.close();
    }

    /**
     * 将给定的“作业数据目录结构”从源目录移动到目的目录
     * @param jobCategory 作业类别
     * @param jobId 作业Id
     * @param fromLevel2Dir 如 ${jobDbTopDir}/pending
     * @param toLevel2Dir 如 ${jobDbTopDir}/finished
     */
    private static boolean moveJobDir_i(String jobCategory,
                                     String jobId,
                                     File fromLevel2Dir,
                                     File toLevel2Dir) {
        File fromJobDir = locateJobDir_i(jobCategory, jobId, fromLevel2Dir);
        if (fromJobDir != null) {
            // 移动目录
            return moveJobDir_ii(fromLevel2Dir, toLevel2Dir, fromJobDir);
        }
        return false;
    }

    /**
     * 移动目录
     * @param fromLevel2Dir 如 ${jobDbTopDir}/pending
     * @param toLevel2Dir 如 ${jobDbTopDir}/finished
     * @param jobDir 待移动的作业数据目录，应为fromLevel2Dir的子孙目录
     */
    private static boolean moveJobDir_ii(File fromLevel2Dir, File toLevel2Dir, File jobDir) {
        final int fromDirLen = fromLevel2Dir.getAbsolutePath().length();
        String relativeDirName = jobDir.getAbsolutePath().substring(fromDirLen);
        File targetDir = new File(toLevel2Dir, relativeDirName);
        targetDir.getParentFile().mkdirs();
        Path source = jobDir.toPath();
        Path target = targetDir.toPath();

        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            LOG.error("移动目录失败, source={}, target={}, error={}", source, target, e.getMessage());
            return false;
        }
    }

    /**
     * 确定给定作业对应的目录。
     * @param jobCategory 作业类别
     * @param jobId 作业Id
     * @param topLevel2Dir 如 ${jobDbTopDir}/pending
     * @return 作业对应的目录
     */
    private static File locateJobDir_i(String jobCategory,
                                     String jobId,
                                     File topLevel2Dir) {
        File cateDir = new File(topLevel2Dir, jobCategory);
        if (!cateDir.exists() || !cateDir.isDirectory()) {
            return null;
        }

        File[] jobDirs = cateDir.listFiles();
        if (jobDirs == null || jobDirs.length == 0) {
            return null;
        }

        // 遍历每个"作业"目录
        final String suffix = "-" + jobId;
        for (File jobDir : jobDirs) {
            if (!jobDir.isDirectory()) {
                continue;
            }
            // 看文件名是否形如${timestamp}-${jobId}
            String name = jobDir.getName();
            if (name.endsWith(suffix)) {
                return jobDir;
            }
        }

        return null;
    }

    private static final byte[] LN = String.format("%n").getBytes();

    @Override
    public void writeSubjobEventLog(SubjobEntry subjob, SubjobBaseEvent event) {
        SubjobCookie c = (SubjobCookie)subjob.getPersisterCookie();
        if (c == null) {
            return;
        }
        String line = event.formatToLine();
        File logFile = new File(c.subjobDir, "events.log");
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write(line.getBytes());
            fos.write(LN);
        } catch (IOException ex) {
            LOG.error("write subjob event log error", ex);
        }
    }

    @Override
    public byte[] readSubjobBodyBytes(SubjobEntry subjob) {
        SubjobCookie c = (SubjobCookie)subjob.getPersisterCookie();
        if (c == null) {
            return null;
        }

        // 读子作业数据体(BLOB)
        try {
            File subjobBodyFile = new File(c.subjobDir, "subjob_body.blob");
            return FileUtils.loadFile(subjobBodyFile.getAbsolutePath());
        } catch (IOException ex) {
            LOG.error("write subjob body-file error", ex);
            return null;
        }
    }

    @Override
    public void readSubjobEventLogs(SubjobEntry subjob, LinesConsumer consumer) {
        SubjobCookie c = (SubjobCookie)subjob.getPersisterCookie();
        if (c == null) {
            return;
        }

        File eventLogFile = new File(c.subjobDir, "events.log");
        if (!eventLogFile.exists() || !eventLogFile.isFile()) {
            return;
        }

        try (FileReader freader = new FileReader(eventLogFile)) {
            try (LineNumberReader linereader = new LineNumberReader(freader)) {
                consumer.consume(linereader);
            }
        } catch (Exception ex) {
            LOG.error("read subjob event log error", ex);
        }
    }

    @Override
    public void writeSubjobFinishState(SubjobEntry subjob, JobState state) {
        SubjobCookie c = (SubjobCookie)subjob.getPersisterCookie();
        if (c == null) {
            return;
        }
        writeJobState_i(c.subjobDir, state);
    }

    @Override
    public void writeJobFinishState(JobEntry job, JobState state) {
        JobCookie jobCookie = (JobCookie)job.getPersisterCookie();
        if (jobCookie == null) {
            return;
        }
        writeJobState_i(jobCookie.jobDir, state);

        // 将作业目录从pendings下移动到finihsed下
        final File pedingDir = new File(this.jobDbTopDir, LEVEL2DIR_Pending);
        final File finishedDir = new File(this.jobDbTopDir, LEVEL2DIR_Finished);
        final String cate = job.getControlBlock().getJobCategory();
        final String jobId = job.getControlBlock().getJobId();
        if (!moveJobDir_i(cate, jobId, pedingDir/*from*/, finishedDir/*to*/)) {
            // 移动失败
            return;
        }

        // job原来存储在Pending目前下，调整成现在存储在Finished目录下
        do {
            final String oldPrefix = pedingDir.getAbsolutePath();
            final String newPrefix = finishedDir.getAbsolutePath();
            final String newJobDirName = jobCookie.jobDir.getAbsolutePath().replace(oldPrefix, newPrefix);
            job.setPersisterCookie(new JobCookie(newJobDirName));

            for (SubjobEntry subjob : job.getSubjobs()) {
                SubjobCookie subjobCookie = (SubjobCookie)subjob.getPersisterCookie();
                final String newSubjobDirName = subjobCookie.subjobDir.getAbsolutePath().replace(oldPrefix, newPrefix);
                subjob.setPersisterCookie(new SubjobCookie(newSubjobDirName));
            }
        }
        while (false);
    }

    File getJobDbTopDir() {
        return jobDbTopDir;
    }

}
