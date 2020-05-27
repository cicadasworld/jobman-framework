package gtcloud.jobman.core.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import gtcloud.common.SharedOkHttpClient;
import gtcloud.common.basetypes.StatusCodeException;
import gtcloud.common.utils.MiscUtils;
import gtcloud.jobman.core.pdo.JobControlBlockDO;
import gtcloud.jobman.core.pdo.JobStatusDO;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import platon.ByteStream;
import platon.PropSet;

/**
 * 该类用于向调度器提交一个作业。
 */
public class JobClient {

    private final String schedulerBaseURL;

    private final OkHttpClient httpClient;

    private static final MediaType BLOB = MediaType.parse("application/octet-stream");

    /**
     * 构造JobClient示例。
     * @param schedulerBaseURL 调度器的服务基URL，形如'http://127.0.0.1:44850/gtjobsched'
     */
    public JobClient(String schedulerBaseURL) {
        this.schedulerBaseURL = schedulerBaseURL;
        this.httpClient = SharedOkHttpClient.get().newBuilder()
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
           .build();
    }

    /**
     * 提交一个作业。
     * @param jobCB 作业控制块;
     * @param jobBody 作业数据字节数组.
     * @param offset 作业数据在数组中的偏移量.
     * @param length 作业数据字节长度.
     * @throws StatusCodeException 若提交失败将抛出异常。
     */
    public void submitJob(JobControlBlockDO jobCB,
                          byte[] jobBody, int offset, int length) throws StatusCodeException {
        try {
            ByteStream stream = new ByteStream();
            jobCB.freeze(stream);
            stream.writeBlob(jobBody, offset, length);

            final int len = stream.length();
            RequestBody body = RequestBody.create(BLOB, stream.array(), 0, len);

            String endpoint = this.schedulerBaseURL + "/job";
            Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Length", String.valueOf(len))
                .post(body)
                .build();

            try (Response response = this.httpClient.newCall(request).execute()) {
                byte[] blob = response.body().bytes();
                PropSet ack = new PropSet();
                ack.defreeze(new ByteStream(blob));
                String code = ack.get("retcode");
                String msg = ack.get("retmsg");
                if (code == null || msg == null) {
                    throw new StatusCodeException(-1, "从调度器返回的应答格式错误");
                }

                int ncode = Integer.parseInt(code);
                if (ncode != 0) {
                    throw new StatusCodeException(ncode, msg);
                }
            }
        }
        catch (Exception ex) {
            throw new StatusCodeException(-1, "提交作业失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 提交一个作业。
     * @param jobCB
     * @param jobBody
     * @throws StatusCodeException
     */
    public void submitJob(JobControlBlockDO jobCB, byte[] jobBody) throws StatusCodeException {
        submitJob(jobCB, jobBody, 0, jobBody.length);
    }

    /**
     * 提交一个作业。
     * @param jobCB
     * @param jobBody
     * @throws StatusCodeException
     */
    public void submitJob(JobControlBlockDO jobCB, ByteStream jobBody) throws StatusCodeException {
        submitJob(jobCB, jobBody.array(), 0, jobBody.length());
    }

    /**
     * 访问调度器，获取给定作业的状态。
     * @param jobId 作业ID.
     * @return
     * @throws StatusCodeException
     */
    public JobStatusDO getJobStatus(String jobId) throws StatusCodeException {
        try {
            String endpoint = this.schedulerBaseURL + "/job/status/" + jobId;
            Request request = new Request.Builder()
                .url(endpoint)
                .build();

            try (Response response = this.httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    int code = response.code();
                    String emsg = String.format("rest调用失败: endpoint=%s, httpcode=%d, httpmsg=%s",
                            endpoint, code, response.message());
                    throw new StatusCodeException(code, emsg);
                }
                InputStream is = response.body().byteStream();
                JobStatusDO jobStatusDO = new JobStatusDO();
                MiscUtils.parseAndCheckRestResult(is, jobStatusDO);
                return jobStatusDO;
            }
        }
        catch (IOException ex) {
            throw new StatusCodeException(-1, "获取作业状态失败: " + ex.getMessage(), ex);
        }
    }
}

