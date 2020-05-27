package gtcloud.jobman.core.scheduler;

import java.io.LineNumberReader;

@FunctionalInterface
public interface LinesConsumer {
	/**
	 * 消费给定的LineNumberReader中提供的文本行
	 * @param lineReader
	 * @throws Exception
	 */
	void consume(LineNumberReader lineReader) throws Exception;
}
