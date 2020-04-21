// Copyright (c) 2015 D1SM.net

package net.fs.rudp;


public class UDPInputStream{

	private final Receiver receiver;
	
	boolean streamClosed=false;

	UDPInputStream(ConnectionUDP conn){
		receiver=conn.receiver;
	}
	
	public int read(byte[] b, int off, int len) throws ConnectException {
		byte[] b2 = read2();
		if(len<b2.length){
			throw new ConnectException("error5");
		}else{
			System.arraycopy(b2, 0, b, off, b2.length);
			return b2.length;
		}
	}
	
	public byte[] read2() throws ConnectException {
		return receiver.receive();
	}
	
	public void closeStreamLocal(){
		if(!streamClosed){
			receiver.closeStreamLocal();
		}
	}


}
