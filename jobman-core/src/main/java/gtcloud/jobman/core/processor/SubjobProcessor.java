package gtcloud.jobman.core.processor;

import gtcloud.common.basetypes.Lifecycle;

/**
 * ����ҵ�������ӿڡ�
 */
public interface SubjobProcessor extends Lifecycle {
    /**
     * ���и�������ҵ, �÷���ʵ��ʱ���뱣֤�̰߳�ȫ��
     * @param ctx ����ҵ����������;
     * @param job �����е�����ҵ����
     * @throws SubjobException
     */
    void processSubjob(SubjobContext ctx, SubjobEntity job) throws SubjobException;
    
    /**
     * ������β��������ҵ��
     * @param ctx ����ҵ����������;
     * @param job �����е�����ҵ����
     * @throws SubjobException
     */
    default void processReduce(SubjobContext ctx, SubjobEntity job) throws SubjobException {
    	//Ĭ����ʲôҲ����
    }
}
