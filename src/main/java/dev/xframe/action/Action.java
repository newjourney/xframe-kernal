package dev.xframe.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.xframe.metric.Guage;
import dev.xframe.metric.Scriber;
import dev.xframe.utils.XTimeFormatter;

public abstract class Action implements Runnable, Scriber {
    
    protected static final Logger logger = LoggerFactory.getLogger(Action.class);
    
    protected ActionLoop loop;
    protected long createTime;

    public Action(ActionLoop loop) {
        this.loop = loop;
        this.createTime = System.currentTimeMillis();
    }

    public void checkin() {
        this.loop.checkin(this);
    }
    
    public long getCreateTime() {
		return createTime;
	}

	@Override
    public final void run() {
        try {
        	ActionLoop.setCurrent(loop);
            if(runable()) {
            	Guage g = Guage.of(getClazz()).creating(createTime).beginning();
                this.exec();
                g.ending().apply();
            }
        } catch (Throwable e) {
            logger.error("Execute exception: " + getClazz().getName(), e);
            failure(e);
        } finally {
        	loop.checkout(this);
        	ActionLoop.unsetCurrent();
            done();
        }
    }
    
    public final void execute() {
        this.exec();
    }
    
    protected boolean runable() {
        return true;
    }

    protected void done() {
        //do nothing
    }
    
    protected void failure(Throwable e) {
        //do nothing
    }

    protected abstract void exec();
    
    protected Class<?> getClazz() {
    	return this.getClass();
    }
    
	@Override
	public void onExecSlow(Guage g) {
		logger.warn("Execute slow [" + g.name() + "] used: " + g.used() + ", waited: " + g.waited());
	}

	@Override
	public void onWaitLong(Guage g) {
		logger.warn("Execute slow [" + g.name() + "] used: " + g.used() + ", waited: " + g.waited() + ", size: " + loop.size());
	}

	@Override
    public String toString() {
        return getClazz().getName() + "[" + XTimeFormatter.from(createTime) + "]";
    }

}
