package gtcloud.jobman.core.scheduler;

import java.io.LineNumberReader;

@FunctionalInterface
public interface LinesConsumer {
	/**
	 * ���Ѹ�����LineNumberReader���ṩ���ı���
	 * @param lineReader
	 * @throws Exception
	 */
	void consume(LineNumberReader lineReader) throws Exception;
}
