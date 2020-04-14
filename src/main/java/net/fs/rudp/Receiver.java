// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.fs.rudp.message.AckListMessage;
import net.fs.rudp.message.CloseMessage_Conn;
import net.fs.rudp.message.CloseMessage_Stream;
import net.fs.rudp.message.DataMessage;
import net.fs.utils.MessageCheck;


public class Receiver {
    private final ConnectionUDP conn;

    private final Cache<Integer, DataMessage> receiveTable = CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.MINUTES).build();
    int lastRead = -1;
    private final Object availOb = new Object();
    private int lastRead2 = -1;

    private final int availWin = RUDPConfig.maxWin;

    private int currentRemoteTimeId;

    private int closeOffset;

    private boolean streamClose = false;

    private boolean receivedClose = false;

    private int nw;

    Receiver(ConnectionUDP conn) {
        this.conn = conn;
        InetAddress dstIp = conn.dstIp;
        int dstPort = conn.dstPort;
    }

    /**
     *  接收数据包
     * @return
     * @throws ConnectException
     */
    public byte[] receive() throws ConnectException {

        if (!conn.isConnected()) {
            throw new ConnectException("连接未建立");
        }
        final int nextSeq = lastRead + 1;
        DataMessage me = receiveTable.getIfPresent(nextSeq);
        if (me == null) {
            synchronized (availOb) {

                try {
                    availOb.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            me = receiveTable.getIfPresent(nextSeq);
        }

        if (!streamClose) {
            checkCloseOffset_Remote();
            if (me == null) {
                throw new ConnectException("连接已断开ccccccc");
            }
            conn.sender.sendLastReadDelay();

            lastRead++;
            receiveTable.invalidate(me.getSequence());

            //System.out.println("received "+received/1024/1024+"MB");
            return me.getData();
        } else {
            throw new ConnectException("连接已断开");
        }
    }

    public void onReceivePacket(DatagramPacket dp) {
        DataMessage me;
        if (dp != null) {
            if (conn.isConnected()) {
                int ver = MessageCheck.checkVer(dp);
                int sType = MessageCheck.checkSType(dp);
                if (ver == RUDPConfig.protocal_ver) {
                    conn.live();
                    if (sType == net.fs.rudp.message.MessageType.sType_DataMessage) {
                        me = new DataMessage(dp);
                        int timeId = me.getTimeId();
                        SendRecord record = conn.clientControl.sendRecordTable_remote.get(timeId);
                        if (record == null) {
                            record = new SendRecord();
                            record.setTimeId(timeId);
                            conn.clientControl.sendRecordTable_remote.put(timeId, record);
                        }
                        record.addSended(me.getData().length);

                        if (timeId > currentRemoteTimeId) {
                            currentRemoteTimeId = timeId;
                        }

                        // 数据包序号
                        int sequence = me.getSequence();

                        conn.sender.sendAckDelay(me.getSequence());
                        if (sequence > lastRead) {
                            receiveTable.put(sequence, me);
                            synchronized (availOb) {
                                if (receiveTable.getIfPresent(lastRead + 1) != null) {
                                    availOb.notifyAll();
                                }
                            }
                        }
                    } else if (sType == net.fs.rudp.message.MessageType.sType_AckListMessage) {
                        AckListMessage alm = new AckListMessage(dp);
                        int lastRead3 = alm.getLastRead();
                        if (lastRead3 > lastRead2) {
                            lastRead2 = lastRead3;
                        }
                        ArrayList<Integer> ackList = alm.getAckList();

                        for (Integer integer : ackList) {
                            int sequence = integer;
                            conn.sender.removeSended_Ack(sequence);
                        }
                        SendRecord rc1 = conn.clientControl.getSendRecord(alm.getR1());
                        if (rc1 != null) {
                            if (alm.getS1() > rc1.getAckedSize()) {
                                rc1.setAckedSize(alm.getS1());
                            }
                        }

                        SendRecord rc2 = conn.clientControl.getSendRecord(alm.getR2());
                        if (rc2 != null) {
                            if (alm.getS2() > rc2.getAckedSize()) {
                                rc2.setAckedSize(alm.getS2());
                            }
                        }

                        SendRecord rc3 = conn.clientControl.getSendRecord(alm.getR3());
                        if (rc3 != null) {
                            if (alm.getS3() > rc3.getAckedSize()) {
                                rc3.setAckedSize(alm.getS3());
                            }
                        }

                        if (checkWin()) {
                            conn.sender.play();
                        }
                    } else if (sType == net.fs.rudp.message.MessageType.sType_CloseMessage_Stream) {
                        CloseMessage_Stream cm = new CloseMessage_Stream(dp);
                        receivedClose = true;
                        int n = cm.getCloseOffset();
                        closeStream_Remote(n);
                    } else if (sType == net.fs.rudp.message.MessageType.sType_CloseMessage_Conn) {
                        CloseMessage_Conn cm2 = new CloseMessage_Conn(dp);
                        conn.closeRemote();
                    } else {
                        ////#MLog.println("未处理数据包 "+sType);
                    }
                }

            }
        }

    }

    public void destroy() {
        //#MLog.println("destroy destroy destroy");
        receiveTable.invalidateAll();
    }

    boolean checkWin() {
        nw = conn.sender.sendOffset - lastRead2;
        boolean b = false;
        if (nw < availWin) {
            b = true;
        } else {
        }
        return b;
    }

    void closeStream_Remote(int closeOffset) {
        this.closeOffset = closeOffset;
        if (!streamClose) {
            checkCloseOffset_Remote();
        }
    }

    void checkCloseOffset_Remote() {
        if (!streamClose) {
            if (receivedClose) {
                if (lastRead >= closeOffset - 1) {
                    streamClose = true;
                    synchronized (availOb) {
                        availOb.notifyAll();
                    }
                    conn.sender.closeStream_Remote();
                }
            }
        }
    }

    void closeStreamLocal() {
        if (streamClose) {
            return;
        }
        //c++;
        streamClose = true;
        synchronized (availOb) {
            availOb.notifyAll();
        }
        conn.sender.closeStreamLocal();
    }

    public int getCurrentTimeId() {
        return currentRemoteTimeId;
    }

    public void setCurrentTimeId(int currentTimeId) {
        this.currentRemoteTimeId = currentTimeId;
    }


    public int getCloseOffset() {
        return closeOffset;
    }


    public void setCloseOffset(int closeOffset) {
        this.closeOffset = closeOffset;
    }

}
