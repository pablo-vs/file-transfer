package util;

import java.util.concurrent.locks.ReentrantLock;

public class Concurrent<T> {

	private T data;
	private ReentrantLock lock = new ReentrantLock(true);

	public Concurrent(T t) {
		data = t;
	}

	public Concurrent() {
		data = new T();
	}

	public T lockAndGet() throws InterruptedException {
		lock.lock();
		return data;
	}

	public lockAndSet(T t) throws InterruptedException {
		lock.lock();
		data = t;
	}

	public void unlock() throws InterruptedException {
		lock.unlock();
	}

}
