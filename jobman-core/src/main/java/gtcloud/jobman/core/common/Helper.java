package gtcloud.jobman.core.common;

import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import platon.IntHolder;
import platon.StringHolder;

public class Helper {

    public static String makeSubjobKey(SubjobControlBlockDO cb) {
        String subjobKey = String.format("%s/%06d", cb.getJobId(), cb.getSubjobSeqNo());
        return subjobKey;
    }

    public static String makeSubjobKey(String jobId, int subjobSeqNo) {
        String subjobKey = String.format("%s/%06d", jobId, subjobSeqNo);
        return subjobKey;
    }

    public static String makeMissionItemId(String jobId, int subjobSeqNo) {
        String subjobKey = String.format("%s|%d", jobId, subjobSeqNo);
        return subjobKey;
    }

    public static boolean parseMissionItemId(final String missionItemId,
    		                                 StringHolder jobId,
    		                                 IntHolder subjobSeqNo) {
		if (missionItemId == null) {
			return false;
		}
		int pos = missionItemId.indexOf('|');
		if (pos < 0) {
			return false;
		}
		
		jobId.value = missionItemId.substring(0, pos);
		String seqNoStr = missionItemId.substring(pos + 1);
		subjobSeqNo.value = Integer.parseInt(seqNoStr);
		return true;
    }
    
}
