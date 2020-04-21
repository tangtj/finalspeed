// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.net.InetAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.fs.utils.MLog;
import net.fs.utils.RunMode;
import net.fs.utils.TimerExecutor;


public class ClientManager {

	private final ConcurrentHashMap<Integer, ClientControl> clientTable= new ConcurrentHashMap<>();
	
	private final Route route;
	
	private final static int RECEIVE_PING_TIMEOUT =8*1000;
	
	private final static int SEND_PING_INTERVAL = 1000;
	
	private final Object objLock =new Object();

	ClientManager(Route route){
		this.route=route;
		GlobalProp prop = GlobalProp.getInstance();
		if (prop.getRunMode() != RunMode.Server) {
			//服务端不需要主动发心跳包给客户端
			TimerExecutor.submitTimerTask(this::scanClientControl, 5, TimeUnit.SECONDS);
		}
	}
	
	void scanClientControl(){
		Iterator<Integer> it=getClientTableIterator();
		long current=System.currentTimeMillis();
		while(it.hasNext()){
			ClientControl cc=clientTable.get(it.next());
			if(cc!=null){
				if(current-cc.getLastReceivePingTime()< RECEIVE_PING_TIMEOUT){
					if(current-cc.getLastSendPingTime()> SEND_PING_INTERVAL){
						cc.sendPingMessage();
					}
				}else {
					//超时关闭client
					MLog.println("超时关闭client "+cc.dstIp.getHostAddress()+":"+cc.dstPort+" "+new Date());
//					System.exit(0);
					synchronized (objLock) {
						cc.close();
					}
				}
			}
		}
	}
	
	void removeClient(int clientId){
		clientTable.remove(clientId);
	}
	
	private Iterator<Integer> getClientTableIterator(){
		return new CopiedIterator<>(clientTable.keySet().iterator());
	}
	
	public ClientControl getClientControl(int clientId,InetAddress dstIp,int dstPort){
		return clientTable.computeIfAbsent(clientId, k-> new ClientControl(route,clientId,dstIp,dstPort));
	}
	
}
