package gtcloud.common.basetypes;

import java.util.HashMap;

public interface Lifecycle {
	/**
	 * ���г�ʼ������.
	 * @param params ��ʼ�����Բ���
	 * @param options ��ѡ����
	 * @throws Exception ����ʼ��ʧ�ܽ��׳��쳣.
	 */
	default void initialize(PropertiesEx params, HashMap<String, Object> options) throws Exception {
		;
	}
	
	/**
	 * ����������.
	 */
	default void dispose() {
		;
	}
}
