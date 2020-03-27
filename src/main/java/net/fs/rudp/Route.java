// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.fs.cap.CapEnv;
import net.fs.cap.VDatagramSocket;
import net.fs.rudp.message.MessageType;
import net.fs.utils.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class Route {

    private DatagramSocket ds;
    public HashMap<Integer, ConnectionUDP> connTable;
    Thread mainThread;

    public AckListManage delayAckManage;

    private static final Object LOCK_OBJ = new Object();

    public int localclientId = Math.abs(RandomUtils.randomInt());

//	LinkedBlockingQueue<DatagramPacket> packetBuffer= new LinkedBlockingQueue<>();

    //1客户端,2服务端
    public int mode = RunMode.Client.code;

    String pocessName = "";

    HashSet<Integer> setedTable = new HashSet<>();

    HashSet<Integer> closedTable = new HashSet<>();

    Cache<Integer, Object> closeCache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(50).build();

    public static int localDownloadSpeed, localUploadSpeed;

    ClientManager clientManager;

    public CapEnv capEnv = null;

    public ClientControl lastClientControl;

    public ProtocolType protocolType;

    private static List<Trafficlistener> listenerList = new Vector<>();

    {

        delayAckManage = new AckListManage();
    }

    public Route(String pocessName, short routePort, RunMode mode2, boolean tcp, boolean tcpEnvSuccess) throws Exception {

        this.mode = mode2.code;
        if (tcp) {
            protocolType = ProtocolType.TCP;
        } else {
            protocolType = ProtocolType.UDP;
        }
        this.pocessName = pocessName;
        if (protocolType == ProtocolType.TCP) {
            if (mode == RunMode.Server.code) {
                //服务端
                VDatagramSocket d = new VDatagramSocket(routePort);
                d.setClient(false);
                try {
                    capEnv = new CapEnv(false, tcpEnvSuccess);
                    capEnv.setListenPort(routePort);
                    capEnv.init();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
                d.setCapEnv(capEnv);

                ds = d;
            } else {
                //客户端
                VDatagramSocket d = new VDatagramSocket();
                d.setClient(true);
                try {
                    capEnv = new CapEnv(true, tcpEnvSuccess);
                    capEnv.init();
                } catch (Exception e) {
                    //e.printStackTrace();
                    throw e;
                }
                d.setCapEnv(capEnv);

                ds = d;
            }
        } else {
            if (mode == RunMode.Server.code) {
                MLog.info("Listen udp port: " + CapEnv.toUnsigned(routePort));
                ds = new DatagramSocket(CapEnv.toUnsigned(routePort));
            } else {
                ds = new DatagramSocket();
            }
        }

        connTable = new HashMap<>();
        clientManager = new ClientManager(this);

        PacketReceiver packetReceiver = new PacketReceiver(ds);

        packetReceiver.init();

        mainThread = new Thread(() -> {
            while (true) {
                DatagramPacket dp = packetReceiver.getPacket();
                if (dp == null) {
                    continue;
                }
                long t1 = System.currentTimeMillis();
                byte[] dpData = dp.getData();

                int sType = 0;
                if (dp.getData().length < 4) {
                    return;
                }
                sType = MessageCheck.checkSType(dp);
                //MLog.println("route receive MessageType111#"+sType+" "+dp.getAddress()+":"+dp.getPort());

                final int connectId = ByteIntConvert.toInt(dpData, 4);
                int remote_clientId = ByteIntConvert.toInt(dpData, 8);

                if (connectId != 0 && closeCache.getIfPresent(connectId) != null){
                    continue;
                }
//
//                if (closedTable.contains(connectId) && connectId != 0) {
//                    //#MLog.println("忽略已关闭连接包 "+connectId);
//                    continue;
//                }

                if (sType == MessageType.sType_PingMessage
                        || sType == MessageType.sType_PingMessage2) {
                    ClientControl clientControl = null;
                    if (mode == RunMode.Server.code) {
                        //发起
                        clientControl = clientManager.getClientControl(remote_clientId, dp.getAddress(), dp.getPort());
                    } else if (mode == RunMode.Client.code) {
                        //接收
                        String key = dp.getAddress().getHostAddress() + ":" + dp.getPort();
                        int sim_clientId = Math.abs(key.hashCode());
                        clientControl = clientManager.getClientControl(sim_clientId, dp.getAddress(), dp.getPort());
                    }
                    clientControl.onReceivePacket(dp);
                } else {
                    //发起
                    if (mode == RunMode.Client.code) {
                        if (!setedTable.contains(remote_clientId)) {
                            String key = dp.getAddress().getHostAddress() + ":" + dp.getPort();
                            int sim_clientId = Math.abs(key.hashCode());
                            ClientControl clientControl = clientManager.getClientControl(sim_clientId, dp.getAddress(), dp.getPort());
                            if (clientControl.getClientId_real() == -1) {
                                clientControl.setClientId_real(remote_clientId);
                                //#MLog.println("首次设置clientId "+remote_clientId);
                            } else {
                                if (clientControl.getClientId_real() != remote_clientId) {
                                    //#MLog.println("服务端重启更新clientId "+sType+" "+clientControl.getClientId_real()+" new: "+remote_clientId);
                                    clientControl.updateClientId(remote_clientId);
                                }
                            }
                            //#MLog.println("cccccc "+sType+" "+remote_clientId);
                            setedTable.add(remote_clientId);
                        }
                    }


                    //udp connection
                    if (mode == RunMode.Server.code) {
                        //接收
                        try {
                            getConnection2(dp.getAddress(), dp.getPort(), connectId, remote_clientId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    final ConnectionUDP ds3 = connTable.get(connectId);
                    if (ds3 != null) {
                        final DatagramPacket dp2 = dp;
                        ds3.receiver.onReceivePacket(dp2);
                        if (sType == MessageType.sType_DataMessage) {
                            TrafficEvent event = new TrafficEvent("", RandomUtils.randomLong(), dp.getLength(), TrafficEvent.type_downloadTraffic);
                            fireEvent(event);
                        }
                    }

                }
            }
        });
        mainThread.start();

    }

    public static void addTrafficlistener(Trafficlistener listener) {
        listenerList.add(listener);
    }

    static void fireEvent(TrafficEvent event) {
        for (Trafficlistener listener : listenerList) {
            int type = event.getType();
            if (type == TrafficEvent.type_downloadTraffic) {
                listener.trafficDownload(event);
            } else if (type == TrafficEvent.type_uploadTraffic) {
                listener.trafficUpload(event);
            }
        }
    }

    public void sendPacket(DatagramPacket dp) throws IOException {
        ds.send(dp);
    }

    public ConnectionProcessor createTunnelProcessor() {
        ConnectionProcessor o = null;
        try {
            Class onwClass = Class.forName(pocessName);
            o = (ConnectionProcessor) onwClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return o;
    }

    void removeConnection(ConnectionUDP conn) {
        synchronized (LOCK_OBJ) {
            //closedTable.add(conn.connectId);
            closeCache.put(conn.connectId,conn.connectId);
            connTable.remove(conn.connectId);
        }
    }

    //接收连接
    public ConnectionUDP getConnection2(InetAddress dstIp, int dstPort, int connectId, int clientId) throws Exception {
        ConnectionUDP conn = connTable.get(connectId);
        if (conn == null) {
            ClientControl clientControl = clientManager.getClientControl(clientId, dstIp, dstPort);
            conn = new ConnectionUDP(this, dstIp, dstPort, 2, connectId, clientControl);
            synchronized (LOCK_OBJ) {
                connTable.put(connectId, conn);
            }
            clientControl.addConnection(conn);
        }
        return conn;
    }

    //发起连接
    public ConnectionUDP getConnection(String address, int dstPort, String password) throws Exception {
        InetAddress dstIp = InetAddress.getByName(address);
        int connectId = Math.abs(RandomUtils.randomInt());
        String key = dstIp.getHostAddress() + ":" + dstPort;
        int remote_clientId = Math.abs(key.hashCode());
        ClientControl clientControl = clientManager.getClientControl(remote_clientId, dstIp, dstPort);
        clientControl.setPassword(password);
        ConnectionUDP conn = new ConnectionUDP(this, dstIp, dstPort, 1, connectId, clientControl);
        synchronized (LOCK_OBJ) {
            connTable.put(connectId, conn);
        }
        clientControl.addConnection(conn);
        lastClientControl = clientControl;
        return conn;
    }

    public boolean isUseTcpTun() {
        return protocolType == ProtocolType.TCP;
    }

    public void setUseProtocolType(ProtocolType type) {
        if (type == null) {
            return;
        }
        this.protocolType = type;
    }

}


