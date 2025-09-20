package tech.lock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class MyLock {

    AtomicBoolean flag = new AtomicBoolean(false);

    Thread owner = null;

    AtomicReference<Node> head = new AtomicReference<>(new Node());

    AtomicReference<Node> tail = new AtomicReference<>(head.get());

    public void lock() {
        if (Thread.currentThread() == this.owner) {
            return;
        }
        // 不直接尝试获取锁就是公平锁
        /*if (flag.compareAndSet(false, true)) {
            owner = Thread.currentThread();
            return;
        }*/
        Node current = new Node();
        current.thread = Thread.currentThread();
        while (true) {
            Node currentTail = tail.get();
            if (tail.compareAndSet(currentTail, current)) {
                current.pre = currentTail;
                currentTail.next = current;
                break;
            }
        }
        while (true) {
            if (current.pre == head.get() && flag.compareAndSet(false, true)) {
                owner = Thread.currentThread();
                head.set(current);
                current.pre.next = null;
                current.pre = null;
                return;
            }
            LockSupport.park();
        }
    }

    public void unLock() {
        if (Thread.currentThread() != this.owner) {
            throw new IllegalStateException("当前线程未持有锁");
        }
        Node headNode = head.get();
        Node next = headNode.next;
        flag.set(false);
        if (next != null) {
            LockSupport.unpark(next.thread);
        }
    }

    class Node {
        Node pre;
        Node next;
        Thread thread;
    }

}
