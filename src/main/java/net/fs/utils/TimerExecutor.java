package net.fs.utils;

import java.util.concurrent.*;

public class TimerExecutor {

    /**
     *  定时任务线程池,将所有需要 一直循环指定的丢到这个里面
     */
    private final static ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(3, r -> new Thread(r,"周期定时任务"));

    public static void submitTimerTask(Runnable run,
                                       long delay, TimeUnit unit){
        scheduledExecutorService.scheduleAtFixedRate(run,delay,delay,unit);
    }
}
