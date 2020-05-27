package gtcloud.jobman.core.scheduler;

public interface JobDispatcherContext {
	/**
	 * 获得给定名称的参数值。
	 * @param name
	 * @param defaultVal
	 * @return
	 */
	String getProperty(String name, String defaultVal);
}
