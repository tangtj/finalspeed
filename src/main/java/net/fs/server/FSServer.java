// Copyright (c) 2015 D1SM.net

// Copyright (c) 2015 D1SM.net

package net.fs.server;

import net.fs.rudp.GlobalProp;
import net.fs.rudp.Route;
import net.fs.utils.MLog;
import net.fs.utils.RunMode;
import net.fs.utils.SystemType;
import net.fs.utils.SystemUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.BindException;
import java.nio.charset.StandardCharsets;

public class FSServer {

    private Route routeUdp;

    private Route routeTcp;

    private int defaultRoutePort = 150;

    static FSServer udpServer;

    public static void main(String[] args) {
        new GlobalProp.Holder(RunMode.Server);
        try {
            FSServer fs = new FSServer();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof BindException) {
                MLog.println("Udp port already in use.");
            }
            MLog.println("Start failed.");
            System.exit(0);
        }
    }

    static FSServer get() {
        return udpServer;
    }

    public FSServer() throws Exception {
        MLog.info("");
        MLog.info("FinalSpeed server starting... ");
        udpServer = this;
        String systemName = System.getProperty("os.name").toLowerCase();
        SystemType systemType = SystemUtils.getSystem(systemName);
        MLog.info("System Name: " + systemType);
        final MapTunnelProcessor mp = new MapTunnelProcessor();

        String ports = readFileData("./cnf/listen_port");
        if (ports != null && !"".equals(ports.trim())) {
            ports = ports.replaceAll("\n", "").replaceAll("\r", "");
            defaultRoutePort = Integer.parseInt(ports);
        }
        routeUdp = new Route(mp.getClass(), (short) defaultRoutePort, RunMode.Server, false, true);
        routeTcp = new Route(mp.getClass(), (short) defaultRoutePort, RunMode.Server, true, true);

        new FireWallOperate(defaultRoutePort, systemType, systemName).init();

    }

    private String readFileData(String path) {
        String content = null;
        FileInputStream fis = null;
        DataInputStream dis = null;
        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
            byte[] data = new byte[(int) file.length()];
            dis.readFully(data);
            content = new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // e.printStackTrace();
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content;
    }

    public int getDefaultRoutePort() {
        return defaultRoutePort;
    }

}
