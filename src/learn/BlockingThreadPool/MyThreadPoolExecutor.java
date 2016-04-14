package learn.BlockingThreadPool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by dimitar.katarov on 4/12/2016.
 */
class MyThreadPoolExecutor extends ThreadPoolExecutor {
    private AtomicInteger tasksInProcess = new AtomicInteger();
    private Synchronizer synchronizer = new Synchronizer();

    public MyThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public void execute(Runnable task) {
        tasksInProcess.incrementAndGet();
        try {
            super.execute(task);
        } catch (RuntimeException | Error e) {
            tasksInProcess.decrementAndGet();
            throw e;
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        synchronized (this) {
            tasksInProcess.decrementAndGet();
            if (tasksInProcess.intValue() == 0) {
                synchronizer.signalAll();
            }
        }
    }

    private class Synchronizer {
        private final Lock lock = new ReentrantLock();
        private final Condition done = lock.newCondition();
        private boolean isDone = false;

        private void signalAll() {
            lock.lock();
            try {
                isDone = true;
                done.signalAll();
            } finally {
                lock.unlock();
            }
        }

        public void await() throws InterruptedException {
            lock.lock();
            try {
                while (!isDone) {
                    done.await();
                }
            } finally {
                isDone = false;
                lock.unlock();
            }
        }
    }
}