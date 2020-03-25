// Copyright (c) 2015 D1SM.net

package net.fs.cap;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.fs.rudp.NetworkInfo;
import net.fs.rudp.NetworkInterfaceOperate;
import net.fs.utils.ByteShortConvert;
import net.fs.utils.MLog;

import net.fs.utils.ThreadUtils;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.PcapStat;
import org.pcap4j.core.Pcaps;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.EthernetPacket.EthernetHeader;
import org.pcap4j.packet.IllegalPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV4Packet.IpV4Header;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.TcpPacket.TcpHeader;
import org.pcap4j.util.MacAddress;


public class CapEnv {

	public MacAddress gatewayMac;

	public MacAddress localMac;

	Inet4Address localIpv4;
	
	public PcapHandle sendHandle;
	
	VDatagramSocket vDatagramSocket;
	
	String testIp_tcp="";
	
	String testIp_udp="5.5.5.5";
	
	String selectedInterfaceName=null;
	
	String selectedInterfaceDes="";

	private PcapNetworkInterface nif;

	private static final int COUNT=-1;

	private static final int READ_TIMEOUT=1;

	private static final int SNAPLEN= 10*1024;

//	HashMap<Integer, TCPTun> tunTable=new HashMap<Integer, TCPTun>();
//
//	Random random=new Random();

	boolean client=false;
	
	short listenPort;
	
	TunManager tcpManager=null;
	
	CapEnv capEnv;
	
	Thread versinMonThread;
	
	boolean detect_by_tcp=true;
	
	public boolean tcpEnable=false;
	
	public boolean fwSuccess=true;
	
	boolean ppp=false;
	
	{
		capEnv=this;
	}
	
	public CapEnv(boolean isClient,boolean fwSuccess){
		this.client=isClient;
		this.fwSuccess=fwSuccess;
		tcpManager=new TunManager(this);
	}

	public void init() throws Exception{
		initInterface();
		
		Thread systemSleepScanThread= new Thread(() -> {
			long t=System.currentTimeMillis();
			while(true){
				if(System.currentTimeMillis()-t>5*1000){
					for(int i=0;i<10;i++){
						MLog.info("休眠恢复... "+(i+1));
						try {
							boolean success=initInterface();
							if(success){
								MLog.info("休眠恢复成功 "+(i+1));
								break;
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}

						try {
							Thread.sleep(5*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

				}
				t=System.currentTimeMillis();
				try {
					Thread.sleep(1*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		systemSleepScanThread.start();
	}
	
	void processPacket(Packet packet) throws Exception{
		EthernetPacket packet_eth=(EthernetPacket) packet;
		EthernetHeader head_eth=packet_eth.getHeader();
		
		IpV4Packet ipV4Packet=null;
		if(ppp){
			ipV4Packet=getIpV4Packet_pppoe(packet_eth);
		}else {
			if(packet_eth.getPayload() instanceof IpV4Packet){
				ipV4Packet=(IpV4Packet) packet_eth.getPayload();
			}
		}
		if(ipV4Packet!=null){
			IpV4Header ipV4Header=ipV4Packet.getHeader();
			if(ipV4Packet.getPayload() instanceof TcpPacket){
				TcpPacket tcpPacket=(TcpPacket) ipV4Packet.getPayload();
				TcpHeader tcpHeader=tcpPacket.getHeader();
				if(client){
					TCPTun conn=tcpManager.getTcpConnection_Client(ipV4Header.getSrcAddr().getHostAddress(),tcpHeader.getSrcPort().value(), tcpHeader.getDstPort().value());
					if(conn!=null){
						conn.process_client(capEnv,packet,head_eth,ipV4Header,tcpPacket,false);
					}
				}else {
					TCPTun conn=null;conn = tcpManager.getTcpConnection_Server(ipV4Header.getSrcAddr().getHostAddress(),tcpHeader.getSrcPort().value());
					if(
							tcpHeader.getDstPort().value()==listenPort){
						if(tcpHeader.getSyn()&&!tcpHeader.getAck()&&conn==null){
							conn=new TCPTun(capEnv,ipV4Header.getSrcAddr(),tcpHeader.getSrcPort().value());
							tcpManager.addConnection_Server(conn);
						}
						conn = tcpManager.getTcpConnection_Server(ipV4Header.getSrcAddr().getHostAddress(),tcpHeader.getSrcPort().value());
						if(conn!=null){
							conn.process_server(packet,head_eth,ipV4Header,tcpPacket,true);
						}
					}
				}
			}else if(packet_eth.getPayload() instanceof IllegalPacket){
				MLog.println("IllegalPacket!!!");
			}
		}
	
	}
	
	PromiscuousMode getMode(PcapNetworkInterface pi){
		PromiscuousMode mode=null;
		String string=(pi.getDescription()+":"+pi.getName()).toLowerCase();
		if(string.contains("wireless")){
			mode=PromiscuousMode.NONPROMISCUOUS;
		}else {
			mode=PromiscuousMode.PROMISCUOUS;
		}
		return mode;
	}
	
	boolean initInterface() throws Exception{
		boolean success=false;
		NetworkInfo networkInfo = new NetworkInterfaceOperate().initInterface();

		localMac = networkInfo.getLocalMacAddress();
		gatewayMac = networkInfo.getGatewayMacAddress();
		localIpv4 = networkInfo.getLocalAddress();

		nif = networkInfo.getNetworkInterface();

		if(nif!=null){
			String desString="";
			if(nif.getDescription()!=null){
				desString=nif.getDescription();
			}
			success=true;
			MLog.info("Selected Network Interface:\n"+"  "+desString+"   "+nif.getName());
			if(fwSuccess){
				tcpEnable=true;
			}
		}else {
			tcpEnable=false;
			MLog.info("Select Network Interface failed,can't use TCP protocal!\n");
		}
		if(tcpEnable){
			sendHandle = nif.openLive(SNAPLEN, getMode(nif), READ_TIMEOUT);
//			final PcapHandle handle= nif.openLive(SNAPLEN, getMode(nif), READ_TIMEOUT);
			
			String filter="";
			if(!client){
				//服务端
				filter="tcp dst port "+toUnsigned(listenPort);
			}else{
				//客户端
				filter="tcp";
			}
			sendHandle.setFilter(filter, BpfCompileMode.OPTIMIZE);

			final PacketListener listener= packet -> {

				try {
					if(packet instanceof EthernetPacket){
						processPacket(packet);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			};

			Thread thread= new Thread(() -> {
				try {
					sendHandle.loop(COUNT, listener);
					PcapStat ps = sendHandle.getStats();
					sendHandle.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			thread.start();
		}
		
		if(!client){
			MLog.info("FinalSpeed server start success.");
		}
		return success;
	
	}
	
	IpV4Packet getIpV4Packet_pppoe(EthernetPacket packet_eth) throws IllegalRawDataException{
		IpV4Packet ipV4Packet=null;
		byte[] pppData=packet_eth.getPayload().getRawData();
		if(pppData.length>8&&pppData[8]==0x45){
			byte[] b2=new byte[2];
			System.arraycopy(pppData, 4, b2, 0, 2);
			short len=(short) ByteShortConvert.toShort(b2, 0);
			int ipLength=toUnsigned(len)-2;
			byte[] ipData=new byte[ipLength];
			//设置ppp参数
			PacketUtils.pppHead_static[2]=pppData[2];
			PacketUtils.pppHead_static[3]=pppData[3];
			if(ipLength==(pppData.length-8)){
				System.arraycopy(pppData, 8, ipData, 0, ipLength);
				ipV4Packet=IpV4Packet.newPacket(ipData, 0, ipData.length);
			}else {
				MLog.println("长度不符!");
			}
		}
		return ipV4Packet;
	}
	
	
	
	public static String printHexString(byte[] b) {
		StringBuffer sb=new StringBuffer();
        for (int i = 0; i < b.length; i++)
        {
            String hex = Integer.toHexString(b[i] & 0xFF);
            hex=  hex.replaceAll(":", " ");
            if (hex.length() == 1)
            {
                hex = '0' + hex;
            }
            sb.append(hex + " ");
        }
        return sb.toString();
    }
	
	public void createTcpTun_Client(String dstAddress,short dstPort) throws Exception{
		Inet4Address serverAddress=(Inet4Address) Inet4Address.getByName(dstAddress);
		TCPTun conn=new TCPTun(this,serverAddress,dstPort, localMac, gatewayMac);
		tcpManager.addConnection_Client(conn);
		boolean success=false;
		for(int i=0;i<6;i++){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(conn.preDataReady){
				success=true;
				break;
			}
		}
		if(success){
			tcpManager.setDefaultTcpTun(conn);
		}else {
			tcpManager.removeTun(conn);
			tcpManager.setDefaultTcpTun(null);
			throw new Exception("创建隧道失败!");
		}
	}


	public short getListenPort() {
		return listenPort;
	}

	public void setListenPort(short listenPort) {
		this.listenPort = listenPort;
		if(!client){
			MLog.info("Listen tcp port: "+toUnsigned(listenPort));
		}
	}
	
	public static int toUnsigned(short s) {  
	    return s & 0x0FFFF;  
	}
	
}
