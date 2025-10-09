package tech.timerwheel;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class TimerWheel {

    private volatile long startTime;

    private final MpscTackQueue[] wheel;

    private final Ticker ticker;

    private final AtomicBoolean started;

    private final CountDownLatch startTimeLatch;

    private final ExecutorService executor;

    public TimerWheel() {
        wheel = new MpscTackQueue[10];
        ticker = new Ticker();
        started = new AtomicBoolean(false);
        startTimeLatch = new CountDownLatch(1);
        executor = Executors.newFixedThreadPool(6);
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new MpscTackQueue();
        }
    }

    public void addDelayTask(Runnable runnable, long delayMs) {
        start();
        DelayTask task = new DelayTask(runnable, delayMs);
        int index = Math.toIntExact(((task.deadline - startTime) / 100) % wheel.length);
        MpscTackQueue queue = wheel[index];
        queue.pushTask(task);
    }

    private void start() {
        if (started.compareAndSet(false, true)) {
            ticker.start();
        }
        // 等待 Ticker 启动
        try {
            startTimeLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            LockSupport.unpark(ticker);
        }
    }

    public static class DelayTask {

        final Runnable runnable;
        long deadline;
        DelayTask next;
        DelayTask pre;

        public DelayTask(Runnable runnable, long delayMs) {
            this.runnable = runnable;
            this.deadline = System.currentTimeMillis() + delayMs;
        }

    }

    public class Slot {

        DelayTask head;
        DelayTask tail;

        public void runWithDeadline(long ticktime) {
            DelayTask current = head;
            while (current != null) {
                DelayTask next = current.next;
                if (current.deadline <= ticktime) {
                    removeTask(current);
                    executor.execute(current.runnable);
                }
                current = next;
            }
        }

        private void removeTask(DelayTask current) {
            if (current.pre != null) {
                current.pre.next = current.next;
            }
            if (current.next != null) {
                current.next.pre = current.pre;
            }
            if (current == head) {
                head = current.next;
            }
            if (current == tail) {
                tail = current.pre;
            }
            current.pre = null;
            current.next = null;
        }

        public void pushDelayTask(DelayTask delayTask) {
            if (head == null) {
                head = tail = delayTask;
            } else {
                tail.next = delayTask;
                delayTask.pre = tail;
                tail = delayTask;
            }
        }

    }

    public class Ticker extends Thread {

        int tickCount = 0;

        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            startTimeLatch.countDown();
            while (started.get()) {
                long tickTime = startTime + (tickCount + 1) * 100L;
                while (System.currentTimeMillis() <= tickTime) {
                    LockSupport.parkUntil(tickTime);
                    if (!started.get()) {
                        return;
                    }
                }
                int index = tickCount % wheel.length;
                MpscTackQueue queue = wheel[index];
                List<Runnable> runnables = queue.removeAndReturnShouldRun(tickTime);
                runnables.forEach(executor::execute);
                tickCount++;
            }
        }

    }


}
