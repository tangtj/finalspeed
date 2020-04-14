// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.io.InputStream;
import java.io.OutputStream;

import net.fs.rudp.ConnectionUDP;
import net.fs.rudp.GlobalProp;
import net.fs.rudp.UDPInputStream;
import net.fs.rudp.UDPOutputStream;
import net.fs.utils.MLog;
import net.fs.utils.RunMode;

public class Pipe {

	int readerLength;
	
	private int dstPort=-1;

	public void pipe(InputStream is,UDPOutputStream tos,int initSpeed,final Pipe p2) throws Exception{
		int len=0;
		byte[] buf=new byte[100*1024];
		while((len=is.read(buf))>0){
			tos.write(buf, 0, len);
		}
	}
	

	
	void sendSleep(long startTime,int speed,int length){
		long needTime=(long) (1000f*length/speed);
		long usedTime=System.currentTimeMillis()-startTime;
		if(usedTime<needTime){
			try {
				Thread.sleep(needTime-usedTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


	public void pipe(UDPInputStream tis,OutputStream os,int maxSpeed,ConnectionUDP conn) throws Exception{
		int len=0;
		byte[] buf=new byte[1000];
		boolean sended=false;
		boolean sendedb=false;
		int n=0;
		boolean msged=false;
		while((len=tis.read(buf, 0, buf.length))>0){
			readerLength +=len;
			if(dstPort>0){
				if (GlobalProp.getInstance().getRunMode() == RunMode.Client) {
					if(!msged){
						msged=true;
						String msg="端口"+dstPort+"连接成功";
						ClientUI.ui.setMessage(msg);
						MLog.println(msg);
					}
					
				}
			}
			os.write(buf, 0, len);
		}
	}



	public int getReaderLength() {
		return readerLength;
	}



	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}

}
