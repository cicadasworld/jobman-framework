package gtcloud.common.basetypes;

import java.util.HashMap;

public interface Lifecycle {
	/**
	 * 进行初始化处理.
	 * @param params 初始化属性参数
	 * @param options 可选参数
	 * @throws Exception 若初始化失败将抛出异常.
	 */
	default void initialize(PropertiesEx params, HashMap<String, Object> options) throws Exception {
		;
	}
	
	/**
	 * 进行清理处理.
	 */
	default void dispose() {
		;
	}
}
