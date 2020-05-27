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
 * ���ฺ����ҵ����д�뵽�����ļ��У���ӱ����ļ��ж�ȡ��ҵ���ݡ�
 */
public class FlatJobPersister implements JobPersister {

    private static final Logger LOG = LoggerFactory.getLogger(FlatJobPersister.class);

    // ��ҵ�־û���������ļ����ڵĵ�һ��Ŀ¼��ȫ·��
    private final File jobDbTopDir;

    // �Ѿ�ִ����ɵ���ҵ�鵵ǰ��"finishedĿ¼"����������? ֮����ƶ���"archivedĿ¼"
    private final long finishedJobKeepDays;

    // �ϴι鵵��ʱ��, �ϴ�����pendingĿ¼��ʱ��
    private long whenLastArchive = 0;
    private long whenLastMoveFinishedJobFromPendingDir = 0;

    // �ڶ���Ŀ¼��
    private static final String LEVEL2DIR_Archived = "Archived"; //��Ŀ¼�´��7��֮ǰ�Ѿ���ɵ���ҵ
    private static final String LEVEL2DIR_Finished = "Finished"; //��Ŀ¼�´��7��֮���Ѿ���ɵ���ҵ
    private static final String LEVEL2DIR_Pending  = "Pending";  //��Ŀ¼�´�Ż�δ��ɵ���ҵ

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
     * ������������ҵд�뵽�ļ��С�
     * @param job ��д�������ҵ��
     */
    public void insertNewJob(JobEntry job) throws Exception {
        //-------------------------------------------------------------------------------
        // ȷ����ҵ���·��: ${jobDbTopDir}/pending/${jobCategory}/${timestamp}-${jobId}
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
            String emsg = "����Ŀ¼ʧ��: " + jobDir.getAbsolutePath();
            LOG.error(emsg);
            throw new IOException(emsg);
        }
        job.setPersisterCookie(new JobCookie(jobDir));

        //----------------------
        // д��ҵ���ƿ鵽�ļ���
        //----------------------
        try {
            File jobcbFile = new File(jobDir, "job_cb.json");
            JsonNode jsonNode = jcb.freezeToJSON();
            writeJsonFile(jobcbFile, jsonNode);
        }
        catch (Exception ex) {
            LOG.error("д��ҵ���ƿ鵽�ļ������з�������", ex);
            throw ex;
        }

        //---------------------
        // д��������ҵ���ļ���
        //---------------------
        for (SubjobEntry subjob : job.getSubjobs()) {
            writeSubjob(jobDir, subjob);
        }
    }

    private void writeSubjob(final File jobDir, final SubjobEntry subjob) throws Exception {
        //----------------------
        // ȷ��������ҵ�Ķ���Ŀ¼
        //----------------------
        SubjobControlBlockDO subcb = subjob.getSubjobCB();
        String subjobKey = String.format("%06d-%06d", subcb.getSubjobCount(), subcb.getSubjobSeqNo());
        File subjobDir = new File(jobDir, subjobKey);
        subjobDir.mkdirs();
        if (!subjobDir.exists()) {
            String emsg = "����Ŀ¼ʧ��: " + subjobDir.getAbsolutePath();
            LOG.error(emsg);
            throw new IOException(emsg);
        }
        subjob.setPersisterCookie(new SubjobCookie(subjobDir));

        //-----------------------
        // д����ҵ���ƿ鵽�ļ���
        //-----------------------
        try {
            File subjobcbFile = new File(subjobDir, "subjob_cb.json");
            JsonNode jsonNode = subcb.freezeToJSON();
            writeJsonFile(subjobcbFile, jsonNode);
        }
        catch (Exception ex) {
            LOG.error("д����ҵ���ƿ鵽�ļ������з�������", ex);
            throw ex;
        }

        //-----------------------------
        // д����ҵ������(BLOB)���ļ���
        //-----------------------------
        try {
            File subjobBodyFile = new File(subjobDir, "subjob_body.blob");
            byte[] body = subjob.getBodyBytes();
            try (FileOutputStream fos = new FileOutputStream(subjobBodyFile)) {
                fos.write(body);
            }
        }
        catch (Exception ex) {
            LOG.error("д����ҵ������(BLOB)���ļ������з�������", ex);
            throw ex;
        }

        //------------------------------------------
        // д����ҵ������(JSON)���ļ���, �����ڵ���
        //------------------------------------------
        Object bodydo = subjob.getBodyDO();
        if (bodydo != null && bodydo instanceof FreezerJSON) {
            try {
                File subjobJsonFile = new File(subjobDir, "subjob_body.json");
                JsonNode jsonNode = ((FreezerJSON)bodydo).freezeToJSON();
                writeJsonFile(subjobJsonFile, jsonNode);
            }
            catch (Exception ex) {
                LOG.error("д����ҵ������(JSON)���ļ������з�������", ex);
                throw ex;
            }
        }

        //--------------------------
        // д����ҵ�������Ե��ļ���
        //--------------------------
        try {
            File subjobPropFile = new File(subjobDir, "subjob_props.json");
            JsonNode jsonNode = subjob.getProps().freezeToJSON();
            writeJsonFile(subjobPropFile, jsonNode);
        }
        catch (Exception ex) {
            LOG.error("д����ҵ�������Ե��ļ������з�������", ex);
            throw ex;
        }
    }

    /**
     * ���ļ��м�����ҵ��
     * @param pendingJobs ������������ػ�δ��ɵ���ҵ������Map��KeyΪjobId;
     * @param finishedJobs �������������7��֮����ɵ���ҵ������Map��KeyΪjobId.
     */
    public void readJobs(Map<String, JobEntry> pendingJobs, Map<String, JobEntry> finishedJobs) {
        // ��֮ǰά��һ��
        doPeriodicalWork();

        // �����Ѿ���ɵ���ҵ
        readJobsInLevel2Directory(LEVEL2DIR_Finished, finishedJobs);

        // ������δ��ɵ���ҵ
        readJobsInLevel2Directory(LEVEL2DIR_Pending, pendingJobs);
    }

    /**
     * ��${jobDbTopDir}/${level2dirName}�¼��ظ�����ҵ
     * @param level2dirName �ڶ���Ŀ¼������"pending"��
     * @param jobs �������
     */
    private void readJobsInLevel2Directory(String level2dirName, Map<String, JobEntry> jobs) {
        File level2dir = new File(this.jobDbTopDir, level2dirName);
        File[] cateDirs = level2dir.listFiles();
        if (cateDirs == null || cateDirs.length == 0) {
            return;
        }

        // ����ÿ��"��ҵ���"Ŀ¼
        for (File cateDir : cateDirs) {
            readJobsInJobCategoryDirectory(cateDir, jobs);
        }
    }

    private void readJobsInJobCategoryDirectory(File cateDir, Map<String, JobEntry> jobs) {
        File[] jobDirs = cateDir.listFiles();
        if (jobDirs == null || jobDirs.length == 0) {
            return;
        }

        // ����ÿ��"��ҵ"Ŀ¼
        for (File jobDir : jobDirs) {
            JobEntry job = readJob_i(jobDir);
            if (job != null) {
                jobs.put(job.getControlBlock().getJobId(), job);
            }
        }
    }

    private JobEntry readJob_i(File jobDir) {
        try {
            // ��"��ҵ���ƿ�"
            File jobcbFile = new File(jobDir, "job_cb.json");
            JsonNode jsonNode = JsonNode.parseJsonFile(jobcbFile.getAbsolutePath());
            JobControlBlockDO jcb = new JobControlBlockDO();
            jcb.defreezeFromJSON(jsonNode);

            // ������"����ҵ"
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
            JobState jobState = readJobState_i(jobDir, whenFinished); //��"��ҵ���״̬"
            JobEntry job = new JobEntry(this, jcb, subjobs, jobState, jobcbFile.lastModified());
            job.setPersisterCookie(new JobCookie(jobDir));
            if (whenFinished.value > 0) {
                job.setFinishedTime(whenFinished.value);
            }

            return job;

        } catch (Exception ex) {
            String emsg = String.format("��%sĿ¼�¶���ҵ���ݴ���", jobDir.getAbsolutePath());
            LOG.error(emsg, ex);
            return null;
        }
    }

    private SubjobEntry readSubjob_i(File subjobDir) throws Exception {
        // ������ҵ���״̬
        final LongHolder whenFinished = new LongHolder();
        final JobState state = readJobState_i(subjobDir, whenFinished);

        // ������ҵ���ƿ�
        final SubjobEntry subjob = new SubjobEntry(this, state);
        File subjobcbFile = new File(subjobDir, "subjob_cb.json");
        JsonNode jcbJsonNode = JsonNode.parseJsonFile(subjobcbFile.getAbsolutePath());
        subjob.getSubjobCB().defreezeFromJSON(jcbJsonNode);

        // ������ҵ������(BLOB)
        // ��Ҫʱ�ż�ʱ����, ��readSubjobBodyBytes()

        // ������ҵ��������
        File subjobPropFile = new File(subjobDir, "subjob_props.json");
        JsonNode propsJsonNode = JsonNode.parseJsonFile(subjobPropFile.getAbsolutePath());
        subjob.getProps().defreezeFromJSON(propsJsonNode);

        // ������ҵ�¼���־
        // ��Ҫʱ�ż�ʱ����, ��readSubjobEventLogs()

        subjob.setPersisterCookie(new SubjobCookie(subjobDir));

        // ����ҵ���ʱ��
        if (whenFinished.value > 0) {
            subjob.setFinishedTime(whenFinished.value);
        }

        return subjob;
    }

    /**
     * ִ�������Դ�������"�������ҵ�鵵"�ȡ�
     */
    public void doPeriodicalWork() {
        final long now = System.currentTimeMillis();
        final long elapsedMillis1 = now - this.whenLastArchive;
        final long ONE_DAYS = 24*3600*1000L;
        if (elapsedMillis1 >= ONE_DAYS) {
            if (LOG.isInfoEnabled()) {
                LOG.info("��ʼarchive()...");
            }
            archive();
            this.whenLastArchive = now;
            if (LOG.isInfoEnabled()) {
                LOG.info("����archive().");
            }
        }

        // ����λ��pendingĿ¼�¡����Ѿ���ɵ���ҵ�ƶ���finishedĿ¼��
        final long elapsedMillis2 = now - this.whenLastMoveFinishedJobFromPendingDir;
        final long FIVE_MINUTES = 5*60*1000L;
        if (elapsedMillis2 >= FIVE_MINUTES) {
            moveFinishedJobFromPendingDir();
            this.whenLastMoveFinishedJobFromPendingDir = now;
        }
    }

    /**
     * ������������ǰ��ɵ���ҵ�ƶ����鵵Ŀ¼�¡�
     */
    private void archive() {
        final File finishedDir = new File(this.jobDbTopDir, LEVEL2DIR_Finished);
        File[] cateDirs = finishedDir.listFiles();
        if (cateDirs == null || cateDirs.length == 0) {
            return;
        }

        final File archivedDir = new File(this.jobDbTopDir, LEVEL2DIR_Archived);

        // ����ÿ��"��ҵ���"Ŀ¼
        final long T0 = System.currentTimeMillis() - (this.finishedJobKeepDays * 24*3600*1000L);
        for (File cateDir : cateDirs) {
            File[] jobDirs = cateDir.listFiles();
            if (jobDirs == null || jobDirs.length == 0) {
                continue;
            }

            // ����ÿ��"��ҵ"Ŀ¼
            for (File jobDir : jobDirs) {
                JobState jobState = readJobState_i(jobDir, null);
                if (jobState == JobState.PENDING) {
                    // ����ҵ��δִ����
                    continue;
                }

                // �Ƿ��Ѿ������˸�������
                File jobcbFile = new File(jobDir, "job_cb.json");
                long jobBornTime = jobcbFile.lastModified();
                if (jobBornTime > T0) {
                    continue;
                }

                // �ƶ�Ŀ¼
                moveJobDir_ii(finishedDir/*from*/, archivedDir/*to*/, jobDir);
            }
        }
    }

    /**
     * ����λ��pendingĿ¼�¡����Ѿ���ɵ���ҵ�ƶ���finishedĿ¼��
     */
    private void moveFinishedJobFromPendingDir() {
        final File pedingDir = new File(this.jobDbTopDir, LEVEL2DIR_Pending);
        File[] cateDirs = pedingDir.listFiles();
        if (cateDirs == null || cateDirs.length == 0) {
            return;
        }

        // ����ÿ��"��ҵ���"Ŀ¼
        final File finishedDir = new File(this.jobDbTopDir, LEVEL2DIR_Finished);
        for (File cateDir : cateDirs) {
            File[] jobDirs = cateDir.listFiles();
            if (jobDirs == null || jobDirs.length == 0) {
                continue;
            }
            for (File jobDir : jobDirs) { //����ÿ��"��ҵ"Ŀ¼
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
     * �������ġ���ҵ����Ŀ¼�ṹ����ԴĿ¼�ƶ���Ŀ��Ŀ¼
     * @param jobCategory ��ҵ���
     * @param jobId ��ҵId
     * @param fromLevel2Dir �� ${jobDbTopDir}/pending
     * @param toLevel2Dir �� ${jobDbTopDir}/finished
     */
    private static boolean moveJobDir_i(String jobCategory,
                                     String jobId,
                                     File fromLevel2Dir,
                                     File toLevel2Dir) {
        File fromJobDir = locateJobDir_i(jobCategory, jobId, fromLevel2Dir);
        if (fromJobDir != null) {
            // �ƶ�Ŀ¼
            return moveJobDir_ii(fromLevel2Dir, toLevel2Dir, fromJobDir);
        }
        return false;
    }

    /**
     * �ƶ�Ŀ¼
     * @param fromLevel2Dir �� ${jobDbTopDir}/pending
     * @param toLevel2Dir �� ${jobDbTopDir}/finished
     * @param jobDir ���ƶ�����ҵ����Ŀ¼��ӦΪfromLevel2Dir������Ŀ¼
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
            LOG.error("�ƶ�Ŀ¼ʧ��, source={}, target={}, error={}", source, target, e.getMessage());
            return false;
        }
    }

    /**
     * ȷ��������ҵ��Ӧ��Ŀ¼��
     * @param jobCategory ��ҵ���
     * @param jobId ��ҵId
     * @param topLevel2Dir �� ${jobDbTopDir}/pending
     * @return ��ҵ��Ӧ��Ŀ¼
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

        // ����ÿ��"��ҵ"Ŀ¼
        final String suffix = "-" + jobId;
        for (File jobDir : jobDirs) {
            if (!jobDir.isDirectory()) {
                continue;
            }
            // ���ļ����Ƿ�����${timestamp}-${jobId}
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

        // ������ҵ������(BLOB)
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

        // ����ҵĿ¼��pendings���ƶ���finihsed��
        final File pedingDir = new File(this.jobDbTopDir, LEVEL2DIR_Pending);
        final File finishedDir = new File(this.jobDbTopDir, LEVEL2DIR_Finished);
        final String cate = job.getControlBlock().getJobCategory();
        final String jobId = job.getControlBlock().getJobId();
        if (!moveJobDir_i(cate, jobId, pedingDir/*from*/, finishedDir/*to*/)) {
            // �ƶ�ʧ��
            return;
        }

        // jobԭ���洢��PendingĿǰ�£����������ڴ洢��FinishedĿ¼��
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
