// Copyright (c) 2015 D1SM.net

package net.fs.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import net.fs.client.Pipe;
import net.fs.rudp.ConnectionProcessor;
import net.fs.rudp.ConnectionUDP;
import net.fs.rudp.Constant;
import net.fs.rudp.UDPInputStream;
import net.fs.rudp.UDPOutputStream;
import net.fs.utils.MLog;

import com.alibaba.fastjson.JSONObject;
import net.fs.utils.ThreadUtils;


public class MapTunnelProcessor implements ConnectionProcessor{

	private Socket dstSocket=null;

	private boolean closed=false;

	private ConnectionUDP conn;


	private UDPInputStream  tis;

	private UDPOutputStream tos;

	private InputStream sis;

	private OutputStream sos;

	private final static String LOCAL_HOST = "127.0.0.1";

	@Override
	public void process(final ConnectionUDP conn){
		this.conn=conn;
		ThreadUtils.execute(this::process);
	}


	void process(){

		tis=conn.uis;
		tos=conn.uos;

		byte[] headData;
		try {
			headData = tis.read2();
			String hs=new String(headData, StandardCharsets.UTF_8);
			JSONObject requestJson=JSONObject.parseObject(hs);
			final int dstPort=requestJson.getIntValue("dst_port");
			String message="";
			JSONObject responseJson=new JSONObject();
			int code =Constant.code_success;
			responseJson.put("code", code);
			responseJson.put("message", message);
			byte[] responeData=responseJson.toJSONString().getBytes(StandardCharsets.UTF_8);
			tos.write(responeData, 0, responeData.length);
			if(code!=Constant.code_success){
				close();
				return;
			}

			dstSocket = new Socket(LOCAL_HOST, dstPort);
			dstSocket.setTcpNoDelay(true);
			sis=dstSocket.getInputStream();
			sos=dstSocket.getOutputStream();

			final Pipe p1=new Pipe();
			final Pipe p2=new Pipe();

			ThreadUtils.execute(() -> {
				try {
					p1.pipe(sis, tos,100*1024,p2);
				}catch (Exception e) {
					//e.printStackTrace();
				}finally{
					close();
					if(p1.getReaderLength()==0){
						MLog.println("端口"+dstPort+"无返回数据");
					}
				}
			});
			ThreadUtils.execute(() -> {
				try {
					p2.pipe(tis,sos,100*1024*1024,conn);
				}catch (Exception e) {
					//e.printStackTrace();
				}finally{
					close();
				}
			});


		} catch (Exception e2) {
			//e2.printStackTrace();
			close();
		}



	}

	void close(){
		if(!closed){
			closed=true;
			if(sis!=null){
				try {
					sis.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
			if(sos!=null){
				try {
					sos.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
			if(tos!=null){
				tos.closeStreamLocal();
			}
			if(tis!=null){
				tis.closeStreamLocal();
			}
			if(conn!=null){
				conn.closeLocal();
			}
			if(dstSocket!=null){
				try {
					dstSocket.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
		}
	}

}
