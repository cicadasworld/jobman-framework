package gtcloud.jobman.core.scheduler;

import gtcloud.jobman.core.pdo.SubjobControlBlockDO;
import platon.PropSet;

/**
 * 子作业对象(对外，用于插件中的拆分子作业)。
 */
public class Subjob {
	
    // 子作业控制块
	protected final SubjobControlBlockDO subjobCB;

    // 子作业数据
	protected byte[] bodyBytes = null;

    // 子作业数据对象
	protected Object bodyDO = null;

    // 其它属性
    protected final PropSet props;
    private static final String PROP_totalWorkload = "totalWorkload"; //子作业的总工作量
    
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
