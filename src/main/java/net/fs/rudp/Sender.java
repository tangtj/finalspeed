// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import net.fs.rudp.message.AckListMessage;
import net.fs.rudp.message.CloseMessage_Conn;
import net.fs.rudp.message.CloseMessage_Stream;
import net.fs.rudp.message.DataMessage;
import net.fs.utils.RandomUtils;

public class Sender {
    DataMessage me2 = null;
    int interval;

    public int sum = 0;
    int sleepTime = 100;
    ConnectionUDP conn;
    //	boolean bussy=false;
//	Object bussyOb=new Object();
//	boolean isHave=false;
    public ConcurrentHashMap<Integer, DataMessage> sendTable = new ConcurrentHashMap<>();
    //	boolean isReady=false;
//	Object readyOb=new Object();
    final Object winOb = new Object();
    public InetAddress dstIp;
    public int dstPort;
    public int sequence = 0;
    int sendOffset = -1;
    //	boolean pause=false;
//	int unAckMin=0;
    int unAckMax = -1;
    int sendSum = 0;

    long lastSendTime = -1;

    boolean closed = false;

    boolean streamClosed = false;

//    static int s = 0;

    //Object syn_send_table=new Object();

    //HashMap<Integer, DataMessage> unAckTable= new HashMap<>();

    Sender(ConnectionUDP conn) {
        this.conn = conn;
        this.dstIp = conn.dstIp;
        this.dstPort = conn.dstPort;
    }

    void sendData(byte[] data, int offset, int length) throws ConnectException, InterruptedException {
        int packetLength = RUDPConfig.packageSize;
        int sum = (length / packetLength);
        if (length % packetLength != 0) {
            sum += 1;
        }
        if (sum == 0) {
            sum = 1;
        }
        int len = packetLength;
        if (length <= len) {
            sendNata(data, 0, length);
        } else {
            for (int i = 0; i < sum; i++) {
                byte[] b = new byte[len];
                System.arraycopy(data, offset, b, 0, len);
                sendNata(b, 0, b.length);
                offset += packetLength;
                if (offset + len > length) {
                    len = length - (sum - 1) * packetLength;
                }
            }
        }
    }

    void sendNata(byte[] data, int offset, int length) throws ConnectException, InterruptedException {

        if (closed) {
            throw new ConnectException("RDP连接已经关闭");
        }


        if (streamClosed) {
            throw new ConnectException("RDP连接已断开sendData");
        }
        DataMessage me = new DataMessage(sequence, data, 0, (short) length, conn.connectId, conn.route.localclientId);
        me.setDstAddress(dstIp);
        me.setDstPort(dstPort);
        sendTable.put(me.getSequence(), me);


        if (!conn.receiver.checkWin()) {
            synchronized (winOb) {
                try {
                    winOb.wait();
                } catch (InterruptedException e) {
                    throw e;
                }
            }
        }

        boolean twice = false;
        if (RUDPConfig.twice_tcp) {
            twice = true;
        }
        if (RUDPConfig.double_send_start) {
            if (me.getSequence() <= 5) {
                twice = true;
            }
        }
        sendDataMessage(me, false, twice, true);
        lastSendTime = System.currentTimeMillis();
        sendOffset++;
        conn.clientControl.resendMange.addTask(conn, sequence);
        sequence++;//必须放最后
    }

    public void closeStreamLocal() {
        if (streamClosed) {
            return;
        }
        streamClosed = true;
        conn.receiver.closeStreamLocal();
        if (!conn.stopnow) {
            sendCloseStreamMessage();
        }
    }

    public void closeStream_Remote() {
        if (!streamClosed) {
            streamClosed = true;
        }
    }

    void sendDataMessage(DataMessage me, boolean resend, boolean twice, boolean block) {
        synchronized (conn.clientControl.getSynlock()) {
            long startTime = System.nanoTime();
            long t1 = System.currentTimeMillis();
            conn.clientControl.onSendDataPacket(conn);

            int timeId = conn.clientControl.getCurrentTimeId();

            me.create(timeId);

            SendRecord record_current = conn.clientControl.getSendRecord(timeId);
            if (!resend) {
                //第一次发，修改当前时间记录
                me.setFirstSendTimeId(timeId);
                me.setFirstSendTime(System.currentTimeMillis());
                record_current.addSended_First(me.getData().length);
                record_current.addSended(me.getData().length);
            } else {
                //重发，修改第一次发送时间记录
                SendRecord record = conn.clientControl.getSendRecord(me.getFirstSendTimeId());
                record.addResended(me.getData().length);
                record_current.addSended(me.getData().length);
            }

            try {
                sendSum++;
                sum++;
                unAckMax++;

                long t = System.currentTimeMillis();
                send(me.getDatagramPacket());

                if (twice) {
                    send(me.getDatagramPacket());//发两次
                }
                if (block) {
                    conn.clientControl.sendSleep(startTime, me.getData().length);
                }
                TrafficEvent event = new TrafficEvent("", RandomUtils.randomLong(), me.getData().length, TrafficEvent.type_uploadTraffic);
                Route.fireEvent(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    void sendAckDelay(int ackSequence) {
        conn.route.delayAckManage.addAck(conn, ackSequence);
    }

    void sendLastReadDelay() {
        conn.route.delayAckManage.addLastRead(conn);
    }

    DataMessage getDataMessage(int sequence) {
        return sendTable.get(sequence);
    }

    public void reSend(int sequence, int count) {
        if (sendTable.containsKey(sequence)) {
            DataMessage dm = sendTable.get(sequence);
            if (dm != null) {
                sendDataMessage(dm, true, false, true);
            }
        }
    }

    public void destroy() {
        sendTable.clear();
    }

    //删除后不会重发
    void removeSended_Ack(int sequence) {
        sendTable.remove(sequence);
    }

    void play() {
        synchronized (winOb) {
            winOb.notifyAll();
        }
    }

    void close() {
        synchronized (winOb) {
            closed = true;
            winOb.notifyAll();
        }
    }

    void sendCloseStreamMessage() {
        CloseMessage_Stream cm = new CloseMessage_Stream(conn.connectId, conn.route.localclientId, sequence);
        cm.setDstAddress(dstIp);
        cm.setDstPort(dstPort);
        int sendTimes = 2;
        for (int i = 0; i < sendTimes; i++) {
            try {
                send(cm.getDatagramPacket());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void sendCloseConnMessage() {
        CloseMessage_Conn cm = new CloseMessage_Conn(conn.connectId, conn.route.localclientId);
        cm.setDstAddress(dstIp);
        cm.setDstPort(dstPort);
        try {
            send(cm.getDatagramPacket());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            send(cm.getDatagramPacket());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendALMessage(ArrayList ackList) {
        int currentTimeId = conn.receiver.getCurrentTimeId();
        AckListMessage alm = new AckListMessage(conn.connectionId, ackList, conn.receiver.lastRead, conn
                .clientControl.sendRecordTable_remote, currentTimeId,
                conn.connectId, conn.route.localclientId);
        alm.setDstAddress(dstIp);
        alm.setDstPort(dstPort);
        try {
            send(alm.getDatagramPacket());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void send(DatagramPacket dp) throws IOException {
        sendPacket(dp, conn.connectId);
    }

    public void sendPacket(DatagramPacket dp, Integer di) throws IOException {
        conn.clientControl.sendPacket(dp);
    }

}
