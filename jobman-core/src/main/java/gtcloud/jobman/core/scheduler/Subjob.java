package gtcloud.jobman.core.scheduler;

import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import platon.PropSet;

/**
 * ����ҵ����(���⣬���ڲ���еĲ������ҵ)��
 */
public class Subjob {
	
    // ����ҵ���ƿ�
	protected final SubjobControlBlockDO subjobCB;

    // ����ҵ����
	protected byte[] bodyBytes = null;

    // ����ҵ���ݶ���
	protected Object bodyDO = null;

    // ��������
    protected final PropSet props;
    private static final String PROP_totalWorkload = "totalWorkload"; //����ҵ���ܹ�����
    
    public Subjob() {
    	this.subjobCB = new SubjobControlBlockDO();
    	this.props = new PropSet();
    }

    public Subjob(Subjob src) {
    	this.subjobCB = src.subjobCB;
    	this.bodyBytes = src.bodyBytes;
    	this.bodyDO = src.bodyDO;    	
    	this.props = src.props;    	
    }
    
    public SubjobControlBlockDO getSubjobCB() {
        return subjobCB;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public void setBodyBytes(byte[] body) {
        this.bodyBytes = body;
    }

    public void setBodyBytes(byte[] body, int offset, int len) {
        this.bodyBytes = new byte[len];
        System.arraycopy(body, offset, this.bodyBytes, 0, len);
    }

    public double getTotalWorkload() {
    	String v = this.props.get(PROP_totalWorkload);
        return v != null ? Double.parseDouble(v) : 0;
    }

    public void setTotalWorkload(double totalWorkload) {
    	this.props.put(PROP_totalWorkload, String.valueOf(totalWorkload));
    }
 
    public void setBodyDO(Object bodyDO) {
        this.bodyDO = bodyDO;
    }
    
    public Object getBodyDO() {
    	return this.bodyDO;
    }

    public PropSet getProps() {
    	return this.props;
    }
}
