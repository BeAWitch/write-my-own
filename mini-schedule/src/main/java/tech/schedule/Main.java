package tech.schedule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss SSS");
        ScheduleService scheduleService = new ScheduleService();
        scheduleService.schedule(() -> {
            System.out.println(LocalDateTime.now().format(dateTimeFormatter) + " 100ms 执行一次的任务");
        }, 100);

        Thread.sleep(1000);
        System.out.println("添加一个 200ms 执行一次的任务");
        scheduleService.schedule(() -> {
            System.out.println(LocalDateTime.now().format(dateTimeFormatter) + " 200ms 执行一次的任务");
        }, 200);
    }

}
