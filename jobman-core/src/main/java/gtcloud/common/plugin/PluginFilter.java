package gtcloud.common.plugin;

public interface PluginFilter {
	/**
	 * �Ƿ�֧�ֵĸ����Ĳ����ǩ.
	 * @param tag �����ǩ
	 * @return ��֧�ָ����Ĳ����ǩ����true�����򷵻�false��
	 */
	boolean supportPluginTag(String tag);

	/**
	 * �Ƿ�֧�ָ����Ĳ����ѡ�߶���.
	 * @param obj �����ѡ�߶���
	 * @return ��֧�ָ����Ĳ����ѡ�߶��󷵻�true�����򷵻�false��
	 */
	boolean supportPluginObject(Object obj);
}
