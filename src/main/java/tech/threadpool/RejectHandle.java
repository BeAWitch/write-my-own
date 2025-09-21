package tech.threadpool;

public interface RejectHandle {

    void reject(Runnable rejectCommand, MyThreadPool threadPool);

}
