package net.fs.rudp;

import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;

public class NetworkInfo {

    public volatile boolean isCatch;

    private MacAddress localMacAddress;

    private MacAddress gatewayMacAddress;

    private Inet4Address localAddress;

    private Inet4Address connectIp;

    private String connectIpStr;

    private PcapNetworkInterface networkInterface;

    public MacAddress getLocalMacAddress() {
        return localMacAddress;
    }

    public void setLocalMacAddress(MacAddress localMacAddress) {
        this.localMacAddress = localMacAddress;
    }

    public MacAddress getGatewayMacAddress() {
        return gatewayMacAddress;
    }

    public void setGatewayMacAddress(MacAddress gatewayMacAddress) {
        this.gatewayMacAddress = gatewayMacAddress;
    }

    public Inet4Address getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(Inet4Address localAddress) {
        this.localAddress = localAddress;
    }

    public Inet4Address getConnectIp() {
        return connectIp;
    }

    public void setConnectIp(Inet4Address connectIp) {
        this.connectIp = connectIp;
    }

    public String getConnectIpStr() {
        return connectIpStr;
    }

    public void setConnectIpStr(String connectIpStr) {
        this.connectIpStr = connectIpStr;
    }

    public PcapNetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(PcapNetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }
}
