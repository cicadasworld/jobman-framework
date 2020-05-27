package gtcloud.jobman.core.test;

import java.util.Iterator;
import java.util.TreeSet;

import gtcloud.jobman.core.scheduler.SubjobEntry;
import gtcloud.jobman.core.scheduler.SubjobComparator;

public class SortedSubjobQueueTest {

	public static void main(String[] args) {
		TreeSet<SubjobEntry> q = new TreeSet<>(new SubjobComparator());
		{
			SubjobEntry s = new SubjobEntry(null);
			s.getSubjobCB().setJobPriority(100);
			s.getSubjobCB().setJobId("10000.04");
			s.getSubjobCB().setSubjobSeqNo(4);			
			s.setBornTime(1000);		
			q.add(s);
		}
		
		{
			SubjobEntry s = new SubjobEntry(null);
			s.getSubjobCB().setJobPriority(200);
			s.getSubjobCB().setJobId("10000.01");
			s.getSubjobCB().setSubjobSeqNo(1);			
			s.setBornTime(1000);		
			q.add(s);
		}
		
		{
			SubjobEntry s = new SubjobEntry(null);
			s.getSubjobCB().setJobPriority(100);
			s.getSubjobCB().setJobId("20000.03");
			s.getSubjobCB().setSubjobSeqNo(3);			
			s.setBornTime(800);		
			q.add(s);
		}
		
		{
			SubjobEntry s = new SubjobEntry(null);
			s.getSubjobCB().setJobPriority(400);
			s.getSubjobCB().setJobId("10000.02");
			s.getSubjobCB().setSubjobSeqNo(2);			
			s.setBornTime(1000);		
			q.add(s);
		}
		
		int itWay = 1;
		if (itWay == 1) {
	        while (!q.isEmpty()) {
	        	SubjobEntry s = q.pollFirst();
	            System.out.format("p=%d, born=%d, jobId=%s, seqno=%d%n", 
	            		s.getSubjobCB().getJobPriority(),
	            		s.getBornTime(),
	            		s.getSubjobCB().getJobId(),
	            		s.getSubjobCB().getSubjobSeqNo());
	        }
		}
		else {
	        Iterator<SubjobEntry> it = q.iterator();
	        while (it.hasNext()) {
	        	SubjobEntry s = it.next();
	            System.out.format("p=%d, born=%d, jobId=%s, seqno=%d%n", 
	            		s.getSubjobCB().getJobPriority(),
	            		s.getBornTime(),
	            		s.getSubjobCB().getJobId(),
	            		s.getSubjobCB().getSubjobSeqNo());
	        }
		}
	}
}
