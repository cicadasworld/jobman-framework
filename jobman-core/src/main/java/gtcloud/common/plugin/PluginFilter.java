package gtcloud.common.plugin;

public interface PluginFilter {
	/**
	 * 是否支持的给定的插件标签.
	 * @param tag 插件标签
	 * @return 若支持给定的插件标签返回true，否则返回false。
	 */
	boolean supportPluginTag(String tag);

	/**
	 * 是否支持给定的插件候选者对象.
	 * @param obj 插件候选者对象。
	 * @return 若支持给定的插件候选者对象返回true，否则返回false。
	 */
	boolean supportPluginObject(Object obj);
}
