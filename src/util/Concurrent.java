package util;

import java.util.concurrent.locks.ReentrantLock;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;

public class Concurrent<T> {

	private T data;
	private ReentrantLock lock = new ReentrantLock(true);

	private static Logger log = Logger.getLogger("CONCURRENT");
	private static Handler logH;

	public Concurrent(T t) {
		if (logH == null) {
			logH = new ConsoleHandler();
			log.addHandler(logH);
		}
		data = t;
	}

	public T lockAndGet() {
		lock.lock();
		return data;
	}

	public void set(T t) {
		lock.lock();
		data = t;
		lock.unlock();
	}

	public void unlock() {
		lock.unlock();
	}

	public boolean heldByMe() {
		return lock.isHeldByCurrentThread();
	}

}
