// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import net.fs.utils.ThreadUtils;

import java.util.concurrent.ConcurrentHashMap;

public class AckListManage implements Runnable {
	Thread mainThread;
	private ConcurrentHashMap<Integer, AckListTask> taskTable;

	public AckListManage() {
		taskTable = new ConcurrentHashMap<>();
		mainThread = new Thread(this);
		mainThread.start();
	}

	void addAck(ConnectionUDP conn, int sequence) {
		AckListTask at = taskTable.computeIfAbsent(conn.connectId, i -> new AckListTask(conn));
		at.addAck(sequence);
	}

	void addLastRead(ConnectionUDP conn) {
		taskTable.putIfAbsent(conn.connectId, new AckListTask(conn));
	}

	@Override
	public void run() {
		while (true) {
			synchronized (this) {
				taskTable.values().parallelStream().forEach(AckListTask::run);
				taskTable.clear();
			}
			ThreadUtils.sleep(RUDPConfig.ackListDelay);
		}
	}
}
