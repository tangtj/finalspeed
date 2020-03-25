package net.fs.rudp;

import net.fs.utils.MLog;
import net.fs.utils.ThreadUtils;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author tang
 * @date 2019/9/30
 */
public class NetworkInterfaceOperate {

    private final int COUNT = -1;
    private static final int READ_TIMEOUT = 1;
    private static final int SNAPLEN = 10 * 1024;

    /**
     *  外国网站 google 效果更好
     */
    private static final String url = "www.google.com";


    private static InetAddress checkNetWorkInterface() {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(url);
        } catch (UnknownHostException e2) {
            e2.printStackTrace();
        }
        if (address == null) {
            MLog.println("域名解析失败,请检查DNS设置!");
            return null;
        }
        final int por = 80;
        String tcpTestIp = address.getHostAddress();
        InetAddress addr = null;
        try (Socket socket = new Socket(tcpTestIp, por)) {
            socket.getLocalAddress();
            addr = socket.getLocalAddress();
            System.out.println(socket.getLocalAddress().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addr;
    }

//    static PcapNetworkInterface detectInterface() {
//        List<PcapNetworkInterface> allDev;
//        try {
//            allDev = Pcaps.findAllDevs();
//        } catch (PcapNativeException e1) {
//            e1.printStackTrace();
//            return null;
//        }
//        InetAddress address = checkNetWorkInterface();
//        PcapNetworkInterface networkInterface = null;
//        for (final PcapNetworkInterface pi : allDev) {
//            List<PcapAddress> addresses = pi.getAddresses();
//            if (addresses.size() == 0){
//                continue;
//            }
//            for (PcapAddress pcapAddress : addresses) {
//                if (Objects.equals(address, pcapAddress.getAddress())){
//                    networkInterface = pi;
//                    break;
//                }
//            }
//        }
//
//        return networkInterface;
//    }

    PcapNetworkInterface.PromiscuousMode getMode(PcapNetworkInterface pi) {
        PcapNetworkInterface.PromiscuousMode mode = null;
        String string = (pi.getDescription() + ":" + pi.getName()).toLowerCase();
        if (string.contains("wireless")) {
            mode = PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS;
        } else {
            mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
        }
        return mode;
    }


    private NetworkInfo detectInterface() {
        List<PcapNetworkInterface> interfaces;
        HashMap<PcapNetworkInterface, PcapHandle> handleTable = new HashMap<>();
        try {
            interfaces = Pcaps.findAllDevs();
        } catch (PcapNativeException e1) {
            e1.printStackTrace();
            return null;
        }
        NetworkInfo networkInfo = new NetworkInfo();
        for (final PcapNetworkInterface pi : interfaces) {
            try {
                final PcapHandle handle = pi.openLive(SNAPLEN, getMode(pi), READ_TIMEOUT);
                handle.setFilter("ip and tcp", BpfProgram.BpfCompileMode.OPTIMIZE);
                handleTable.put(pi, handle);
                final PacketListener listener = packet -> {
                    if (networkInfo.isCatch){
                        return;
                    }

                    EthernetPacket ethPacket = packet.get(EthernetPacket.class);
                    EthernetPacket.EthernetHeader packetHeader = ethPacket.getHeader();

                    IpV4Packet ipV4Packet = packet.getPayload().get(IpV4Packet.class);


                    IpV4Packet.IpV4Header ipV4Header = ipV4Packet.getHeader();

                    if (ipV4Header.getSrcAddr().getHostAddress().equals(networkInfo.getConnectIpStr())) {
                        networkInfo.setLocalMacAddress(packetHeader.getDstAddr());
                        networkInfo.setGatewayMacAddress(packetHeader.getSrcAddr());
                        networkInfo.setLocalAddress(ipV4Header.getDstAddr());
                        networkInfo.setNetworkInterface(pi);
                        networkInfo.isCatch = true;
                    } else if (ipV4Header.getDstAddr().getHostAddress().equals(networkInfo.getConnectIpStr())) {
                        networkInfo.setLocalMacAddress(packetHeader.getSrcAddr());
                        networkInfo.setGatewayMacAddress(packetHeader.getDstAddr());
                        networkInfo.setLocalAddress(ipV4Header.getSrcAddr());
                        networkInfo.setNetworkInterface(pi);
                        networkInfo.isCatch = true;
                    } else if (ipV4Header.getDstAddr().getHostAddress().equals(networkInfo.getConnectIpStr())) {
                        networkInfo.setLocalMacAddress(packetHeader.getSrcAddr());
                        networkInfo.setGatewayMacAddress(packetHeader.getDstAddr());
                        networkInfo.setLocalAddress(ipV4Header.getSrcAddr());
                        networkInfo.setNetworkInterface(pi);
                        networkInfo.isCatch = true;
                    }

                };

                Thread thread = new Thread(() -> {
                    try {
                        handle.loop(COUNT, listener);
                        PcapStat ps = handle.getStats();
                        handle.close();
                    } catch (Exception ignored) {
                    }
                });
                thread.start();
            } catch (PcapNativeException | NotOpenException e1) {

            }

        }

        //detectMac_udp();
        FutureTask<NetworkInfo> infoFuture = detectNetWorkInfo(networkInfo);

        ThreadUtils.execute(infoFuture);

        NetworkInfo info = null;
        try {
            info = infoFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println(info);

        for (PcapNetworkInterface pi : handleTable.keySet()) {
            PcapHandle handle = handleTable.get(pi);
            try {
                handle.breakLoop();
            } catch (NotOpenException e) {
                e.printStackTrace();
            }
            //handle.close();//linux下会阻塞
        }
        return networkInfo;
    }

    public NetworkInfo initInterface() {
        return detectInterface();
    }


    private FutureTask<NetworkInfo> detectNetWorkInfo(NetworkInfo networkInfo) {

        Callable<NetworkInfo> callable = () -> {
            InetAddress address = null;
            try {
                address = InetAddress.getByName(url);
            } catch (UnknownHostException ignored) {
            }
            if (address == null) {
                MLog.println("域名解析失败,请检查DNS设置!");
            }
            final int por = 80;
            String networkConnectTestIp = address.getHostAddress();
            networkInfo.setConnectIpStr(networkConnectTestIp);
            for (int i = 0; i < 5; i++) {
                try {
                    ThreadUtils.execute(() -> {
                        try {
                            Socket socket = new Socket(networkConnectTestIp, por);
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    Thread.sleep(500);
                    if (networkInfo.isCatch) {
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ThreadUtils.sleep(1);
                }
            }
            return networkInfo;
        };
        return new FutureTask<>(callable);
    }
}
