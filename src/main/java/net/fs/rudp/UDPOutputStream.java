// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.net.InetAddress;

public class UDPOutputStream {
	private final Sender sender;
	
	UDPOutputStream (ConnectionUDP conn){
		this.sender=conn.sender;
	}
	
	public void write(byte[] data,int offset,int length) throws ConnectException, InterruptedException {
		sender.sendData(data, offset,length);
	}
	
	public void closeStreamLocal(){
		sender.closeStreamLocal();
	}
	
}
