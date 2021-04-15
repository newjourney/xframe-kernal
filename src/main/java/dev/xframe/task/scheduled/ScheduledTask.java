package dev.xframe.task.scheduled;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import dev.xframe.task.DelayTask;
import dev.xframe.task.TaskLoop;
import dev.xframe.utils.XLambda;

public abstract class ScheduledTask extends DelayTask {
	
	protected final int period;
	
	public ScheduledTask(TaskLoop loop, int delay, int period) {
		super(loop, delay);
		this.period = period;
	}
	
	@Override
	protected void done() {
		if(period > 0) {//从执行开始时间计算
			long periodBegin = execTime > 0 ? execTime : System.currentTimeMillis();
			recheckin(periodBegin, period);
		}
	}
	
	public static ScheduledTask once(TaskLoop loop, int delay, Runnable runnable) {
		return period(loop, delay, -1, runnable);
	}
	
	public static ScheduledTask period(TaskLoop loop, int period, Runnable runnable) {
		return period(loop, period, period, runnable);
	}
	public static ScheduledTask period(TaskLoop loop, int delay, int period, Runnable runnable) {
		return new Simple(loop, delay, period, runnable);
	}
	
	public static final class Simple extends ScheduledTask {
		final Runnable runnable;
		public Simple(TaskLoop loop, int delay, int period, Runnable runnable) {
			super(loop, delay, period);
			this.runnable = runnable;
		}
		protected void exec() {
			runnable.run();
		}
		protected Class<?> getClazz() {
			return runnable.getClass();
		}
	}
	
	public static class MethodBased extends ScheduledTask {
		protected final String name;
		protected final Object delegate;
		protected final Consumer<Object> runner;
		@SuppressWarnings("unchecked")
		public MethodBased(String name, TaskLoop loop, int delay, int period, Object delegate, Method method) {
			super(loop, delay, period);
			this.name = name;
			this.delegate = delegate;
			this.runner = XLambda.create(Consumer.class, method);
		}
		protected void exec() {
			runner.accept(delegate);
		}
		protected String getName() {
			return name;
		}
	}
}
