// Copyright (c) 2015 D1SM.net

package net.fs.cap;

import net.fs.rudp.CopiedIterator;
import net.fs.utils.MLog;
import net.fs.utils.TimerExecutor;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TunManager {

    private ConcurrentHashMap<String, TCPTun> connTable = new ConcurrentHashMap<>();

    private final TunManager tunManager;

    TCPTun defaultTcpTun;

    private final Object lock = new Object();

    CapEnv capEnv;

    TunManager(CapEnv capEnv) {
        tunManager = this;
        this.capEnv = capEnv;
        TimerExecutor.submitTimerTask(this::scan, 1, TimeUnit.SECONDS);
    }

    void scan() {
        Iterator<String> it = getConnTableIterator();
        while (it.hasNext()) {
            String key = it.next();
            TCPTun tun = connTable.get(key);
            if (tun != null) {
                if (tun.preDataReady) {
                    //无数据超时
                    long t = System.currentTimeMillis() - tun.lastReceiveDataTime;
                    if (t > 6000) {
                        connTable.remove(key);
                        if (capEnv.isClient) {
                            defaultTcpTun = null;
                            MLog.println("tcp隧道超时");
                        }
                    }
                } else {
                    //连接中超时
                    if (System.currentTimeMillis() - tun.createTime > 5000) {
                        connTable.remove(key);
                    }
                }
            }
        }
    }

    public void removeTun(TCPTun tun) {
        connTable.remove(tun.key);
    }

    Iterator<String> getConnTableIterator() {
        Iterator<String> it;
        synchronized (lock) {
            it = new CopiedIterator<>(connTable.keySet().iterator());
        }
        return it;
    }

//    public static TunManager get() {
//        return tunManager;
//    }

    public TCPTun getTcpConnection_Client(String remoteAddress, short remotePort, short localPort) {
        return connTable.get(remoteAddress + ":" + remotePort + ":" + localPort);
    }

    public void addConnection_Client(TCPTun conn) {
        String key = conn.remoteAddress.getHostAddress() + ":" + conn.remotePort + ":" + conn.localPort;
        //MLog.println("addConnection "+key);
        conn.setKey(key);
        connTable.put(key, conn);
    }

    public TCPTun getTcpConnection_Server(String remoteAddress, short remotePort) {
        return connTable.get(remoteAddress + ":" + remotePort);
    }

    public void addConnection_Server(TCPTun conn) {
        String key = conn.remoteAddress.getHostAddress() + ":" + conn.remotePort;
        //MLog.println("addConnection "+key);
        conn.setKey(key);
        connTable.put(key, conn);
    }

    public TCPTun getDefaultTcpTun() {
        return defaultTcpTun;
    }

    public void setDefaultTcpTun(TCPTun defaultTcpTun) {
        this.defaultTcpTun = defaultTcpTun;
    }

}
