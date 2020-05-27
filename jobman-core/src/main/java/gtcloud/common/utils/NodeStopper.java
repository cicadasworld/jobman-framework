package gtcloud.common.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NodeStopper {

    private final CountDownLatch latch = new CountDownLatch(1);

    private String stopFlagFile = null;

    public NodeStopper(final NodeId nodeId) {
        this.stopFlagFile = PathUtils.getTempDir() + File.separator + nodeId + ".stop";
    }

    public void reset() {
        // 删除停止标志文件
        File of = new File(this.stopFlagFile);
        if (of.exists()) {
            of.delete();
        }
    }

    public void waitForStopEvent() throws InterruptedException {
        // 等待退出通知
        while (!isStopEventFired()) {
            // noop
        }
    }

    public void fireStopEvent() {
        this.latch.countDown();
    }

    public CountDownLatch getStopLatch() {
        return this.latch;
    }

    private boolean isStopEventFired() throws InterruptedException {
        boolean done = this.latch.await(5000, TimeUnit.MILLISECONDS);
        if (done) {
            return true;
        }
        File of = new File(this.stopFlagFile);
        if (of.exists()) {
            return true;
        }
        return false;
    }

    public void stopPreviousInstance() {
        File of = new File(this.stopFlagFile);
        File odir = of.getParentFile();
        if (!odir.exists()) {
            odir.mkdirs();
        }
        try {
	        FileOutputStream fos = null;
	        try {
	            fos = new FileOutputStream(of);
	            fos.write("stop\n".getBytes());
	        } finally {
	        	StreamUtils.close(fos);
	        }
        } catch(Exception ex) {
        	ex.printStackTrace();
        }
    }
}
