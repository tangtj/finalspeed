package net.fs.utils;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  线程相关方法
 */
public class ThreadUtils {
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 500, 10 * 1000, TimeUnit.MILLISECONDS, new SynchronousQueue<>());

    public static void sleep(int millis,boolean withException) throws InterruptedException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            if (withException) {
                throw e;
            }
        }
    }

    public static void sleep(int millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public static void execute(Runnable runnable){
        executor.execute(runnable);
    }
}
