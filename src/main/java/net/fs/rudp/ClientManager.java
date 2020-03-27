// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import net.fs.utils.MLog;
import net.fs.utils.TimerExecutor;


public class ClientManager {

	//TODO: 需要改成多线程版
	HashMap<Integer, ClientControl> clientTable=new HashMap<Integer, ClientControl>();
	
	Route route;
	
	int receivePingTimeout=8*1000;
	
	int sendPingInterval=1*1000;
	
	Object syn_clientTable=new Object();
	
	ClientManager(Route route){
		this.route=route;
		TimerExecutor.submitTimerTask(this::scanClientControl,1, TimeUnit.SECONDS);
	}
	
	void scanClientControl(){
		Iterator<Integer> it=getClientTableIterator();
		long current=System.currentTimeMillis();
		//MLog.println("ffffffffffff "+clientTable.size());
		while(it.hasNext()){
			ClientControl cc=clientTable.get(it.next());
			if(cc!=null){
				if(current-cc.getLastReceivePingTime()<receivePingTimeout){
					if(current-cc.getLastSendPingTime()>sendPingInterval){
						cc.sendPingMessage();
					}
				}else {
					//超时关闭client
					MLog.println("超时关闭client "+cc.dstIp.getHostAddress()+":"+cc.dstPort+" "+new Date());
//					System.exit(0);
					synchronized (syn_clientTable) {
						cc.close();
					}
				}
			}
		}
	}
	
	void removeClient(int clientId){
		clientTable.remove(clientId);
	}
	
	Iterator<Integer> getClientTableIterator(){
		Iterator<Integer> it=null;
		synchronized (syn_clientTable) {
			it=new CopiedIterator(clientTable.keySet().iterator());
		}
		return it;
	}
	
	ClientControl getClientControl(int clientId,InetAddress dstIp,int dstPort){
		ClientControl c=clientTable.get(clientId);
		if(c==null){
			c=new ClientControl(route,clientId,dstIp,dstPort);
			synchronized (syn_clientTable) {
				clientTable.put(clientId, c);
			}
		}
		return c;
	}
	
}
