package gtcloud.jobman.core.processor;

import java.util.concurrent.Future;

import platon.BooleanHolder;

/**
 * 为"子作业"运行提供上下文环境
 */
public interface SubjobContext {
	/**
	 * 报告子作业处理过程中的某个步骤开始。
     * @param jh "子作业"描述子, 给出报告的是哪个"子作业"的处理进度; 
	 * @param stepId 步骤标识。
	 */
	void reportStepBegin(SubjobHandle jh, String stepId);
	
	/**
	 * 报告子作业处理过程中的进度。
     * @param jh "子作业"描述子, 给出报告的是哪个"子作业"的处理进度; 
	 * @param completedWorkload 迄今完成的工作量。
	 */
	void reportProgress(SubjobHandle jh, double completedWorkload);

	/**
	 * 报告子作业处理过程中的某个步骤完成。
     * @param jh "子作业"描述子, 给出报告的是哪个"子作业"的处理进度; 
	 * @param stepId 步骤标识。
	 */
	void reportStepEnd(SubjobHandle jh, String stepId);
	
    /**
     * 使用给定名字的线程池来执行一个命令.
     *
     * @param threadPoolName 线程池的名称, 若为null则使用默认名字"general";
     * @param cmd 要执行的任务命令;
     * @param isBusy 返回是否忙 。
     */
    Future<?> executeCommand(String threadPoolName, Runnable cmd, BooleanHolder isBusy);

}
