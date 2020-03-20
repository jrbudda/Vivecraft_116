package org.vivecraft.utils;

public class Semaphore {
	private boolean locked=true;
	final private Object lock=new Object();
	long timeout=-1;

	public Semaphore(){}

	public Semaphore(long timeout){
		this.timeout=timeout;
	}

	public void waitFor(){
		synchronized (lock){
			if(!locked)
				return;
			Thread watchDog=null;
			if (timeout!=-1) {
				watchDog = new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								Thread.sleep(timeout);
								wakeUp();
							} catch (InterruptedException e) {

						}
					}
				});
				watchDog.start();
			}

			try {
				lock.wait();
				if(watchDog!=null)
					watchDog.interrupt();
			} catch (InterruptedException e) {}
		}
	}

	public void wakeUp(){
		synchronized (lock){
			if(!locked)
				return;
			locked=false;
			lock.notifyAll();
		}
	}

	public void reactivate(){
		synchronized (lock){
			locked=true;
		}
	}

	public boolean isActive(){
		return locked;
	}

	public long getTimeout() {
		synchronized (lock) {
			return timeout;
		}
	}

	public void setTimeout(long timeout) {
		synchronized (lock) {
			this.timeout = timeout;
		}
	}
}
