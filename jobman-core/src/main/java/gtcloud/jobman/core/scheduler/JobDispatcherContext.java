package gtcloud.jobman.core.scheduler;

public interface JobDispatcherContext {
	/**
	 * ��ø������ƵĲ���ֵ��
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	String getProperty(String name, String defaultVal);
}
