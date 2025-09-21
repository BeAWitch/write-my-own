package tech.schedule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.LockSupport;

public class ScheduleService {

    Trigger trigger = new Trigger();

    ExecutorService executorService = Executors.newFixedThreadPool(6);

    void schedule(Runnable task, long delay) {
        Job job = new Job();
        job.setTask(task);
        job.setStartTime(System.currentTimeMillis() + delay);
        job.setDelay(delay);
        trigger.queue.offer(job);
        trigger.wakeUp();
    }

    // 等待合适的时间，将对应任务放入线程池中
    class Trigger {

        PriorityBlockingQueue<Job> queue = new PriorityBlockingQueue<>();

        Thread thread = new Thread(() -> {
           while (true) {
               // while 循环防止虚假唤醒后拿到空的 Job
               while (queue.isEmpty()) {
                   LockSupport.park();
               }
               Job latelyJob = queue.peek();
               if (latelyJob.getStartTime() < System.currentTimeMillis()) {
                   latelyJob = queue.poll();
                   executorService.execute(latelyJob.getTask());
                   Job nextJob = new Job();
                   nextJob.setTask(latelyJob.getTask());
                   nextJob.setStartTime(System.currentTimeMillis() + latelyJob.getDelay());
                   nextJob.setDelay(latelyJob.getDelay());
                   queue.offer(nextJob);
               } else {
                   LockSupport.parkUntil(latelyJob.getStartTime());
               }
           }
        });

        {
            thread.start();
            System.out.println("触发器启动");
        }

        void wakeUp() {
            LockSupport.unpark(thread);
        }

    }

}
