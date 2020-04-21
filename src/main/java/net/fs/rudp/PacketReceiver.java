package net.fs.rudp;

import net.fs.utils.ThreadUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author tang
 * @date 2019/10/3
 */
public class PacketReceiver {

    private final DatagramSocket socket;

    private final LinkedBlockingQueue<DatagramPacket> packetBuffer;

    public PacketReceiver(DatagramSocket ds) {
        this.socket = ds;
        packetBuffer = new LinkedBlockingQueue<>();
    }

    public void init() {
        ThreadUtils.execute(() -> {
            while (true) {
                byte[] b = new byte[1500];
                DatagramPacket dp = new DatagramPacket(b, b.length);
                try {
                    socket.receive(dp);
                    packetBuffer.add(dp);
                } catch (IOException e) {
                    e.printStackTrace();
                    ThreadUtils.sleep(1);
                }
            }
        });
    }

    public DatagramPacket getPacket() {
        try {
            return packetBuffer.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
