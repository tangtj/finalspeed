package net.fs.server;

import net.fs.utils.SystemType;
import net.fs.utils.ThreadUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FireWallOperate {

    private boolean successFirewallWindows = true;

    private SystemType systemType;

    private int routePort;

    private String systemName;

    public FireWallOperate(int routePort, SystemType type, String systemName) {
        this.routePort = routePort;
        this.systemType = type;
        this.systemName = systemName;
    }

    public void init() {
        if (systemType == SystemType.Linux) {
            startFirewall_linux();
            setFireWall_linux_udp();
        } else if (systemType == SystemType.Windows) {
            startFirewall_windows();
        }

        ThreadUtils.execute(() -> {
            try {
                if (systemType == SystemType.Linux) {
                    setFireWall_linux_tcp();
                } else if (systemType == SystemType.Windows) {
                    if (successFirewallWindows) {
                        setFireWall_windows_tcp();
                    } else {
                        System.out.println("启动windows防火墙失败,请先运行防火墙服务.");
                    }
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        });
    }

    private void startFirewall_windows() {

        String runFirewall = "netsh advfirewall set allprofiles state on";
        Thread standReadThread = null;
        Thread errorReadThread = null;
        try {
            final Process p = Runtime.getRuntime().exec(runFirewall, null);
            standReadThread = new Thread(() -> {
                InputStream is = p.getInputStream();
                BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
                while (true) {
                    String line;
                    try {
                        line = localBufferedReader.readLine();
                        if (line == null) {
                            break;
                        } else {
                            if (line.contains("Windows")) {
                                successFirewallWindows = false;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        //error();
                        break;
                    }
                }
            });
            standReadThread.start();

            errorReadThread = new Thread(() -> {
                InputStream is = p.getErrorStream();
                readSkipStream(is);
            });
            errorReadThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            successFirewallWindows = false;
            //error();
        }

        if (standReadThread != null) {
            try {
                standReadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (errorReadThread != null) {
            try {
                errorReadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void setFireWall_windows_tcp() {
        cleanRule_windows();
        try {
            if (systemName.contains("xp") || systemName.contains("2003")) {
                String cmd_add1 = "ipseccmd -w REG -p \"tcptun_fs_server\" -r \"Block TCP/" + routePort + "\" -f *+0:" + routePort + ":TCP " + " -n BLOCK -x ";
                final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                p2.waitFor();
            } else {
                String cmd_add1 = "netsh advfirewall firewall add rule name=tcptun_fs_server protocol=TCP dir=out localport=" + routePort + " action=block ";
                final Process p2 = Runtime.getRuntime().exec(cmd_add1, null);
                p2.waitFor();
                String cmd_add2 = "netsh advfirewall firewall add rule name=tcptun_fs_server protocol=TCP dir=in localport=" + routePort + " action=block ";
                Process p3 = Runtime.getRuntime().exec(cmd_add2, null);
                p3.waitFor();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void cleanRule_windows() {
        try {
            if (systemName.contains("xp") || systemName.contains("2003")) {
                String cmd_delete = "ipseccmd -p \"tcptun_fs_server\" -w reg -y";
                final Process p1 = Runtime.getRuntime().exec(cmd_delete, null);
                p1.waitFor();
            } else {
                String cmd_delete = "netsh advfirewall firewall delete rule name=tcptun_fs_server ";
                final Process p1 = Runtime.getRuntime().exec(cmd_delete, null);
                p1.waitFor();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void startFirewall_linux() {
        String cmd1 = "service iptables start";
        runCommand(cmd1);
    }

    private void setFireWall_linux_udp() {
        cleanUdpTunRule();
        String cmd2 = "iptables -I INPUT -p udp --dport " + routePort + " -j ACCEPT"
                + " -m comment --comment udptun_fs_server";
        runCommand(cmd2);
    }

    private void cleanUdpTunRule() {
        while (true) {
            int row = getRow("udptun_fs_server");
            if (row > 0) {
                // MLog.println("删除行 "+row);
                String cmd = "iptables -D INPUT " + row;
                runCommand(cmd);
            } else {
                break;
            }
        }
    }

    private void setFireWall_linux_tcp() {
        cleanTcpTunRule();
        String cmd2 = "iptables -I INPUT -p tcp --dport " + routePort + " -j DROP"
                + " -m comment --comment tcptun_fs_server ";
        runCommand(cmd2);

    }

    private void cleanTcpTunRule() {
        while (true) {
            int row = getRow("tcptun_fs_server");
            if (row > 0) {
                // MLog.println("删除行 "+row);
                String cmd = "iptables -D INPUT " + row;
                runCommand(cmd);
            } else {
                break;
            }
        }
    }

    private int getRow(String name) {
        int row_delect = -1;
        String cme_list_rule = "iptables -L -n --line-number";
        // String [] cmd={"netsh","advfirewall set allprofiles state on"};
        try {
            final Process p = Runtime.getRuntime().exec(cme_list_rule, null);

            Thread errorReadThread = new Thread(() -> {
                InputStream is = p.getErrorStream();
                readSkipStream(is);
            });
            errorReadThread.start();

            InputStream is = p.getInputStream();
            BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String line;
                try {
                    line = localBufferedReader.readLine();
                    // System.out.println("standaaa "+line);
                    if (line == null) {
                        break;
                    } else {
                        if (line.contains(name)) {
                            int index = line.indexOf("   ");
                            if (index > 0) {
                                String n = line.substring(0, index);
                                try {
                                    if (row_delect < 0) {
                                        // System.out.println("standaaabbb
                                        // "+line);
                                        row_delect = Integer.parseInt(n);
                                    }
                                } catch (Exception ignored) {

                                }
                            }
                        }
                        ;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }

            errorReadThread.join();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            // error();
        }
        return row_delect;
    }

    void runCommand(String command) {
        Thread standReadThread = null;
        Thread errorReadThread = null;
        try {
            final Process p = Runtime.getRuntime().exec(command, null);
            standReadThread = new Thread(() -> {
                InputStream is = p.getInputStream();
                readSkipStream(is);
            });
            standReadThread.start();

            errorReadThread = new Thread(() -> {
                InputStream is = p.getErrorStream();
                readSkipStream(is);
            });
            errorReadThread.start();
            standReadThread.join();
            errorReadThread.join();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            // error();
        }
    }

    private void readSkipStream(InputStream is) {
        if (is == null) {
            return;
        }
        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
        while (true) {
            String line;
            try {
                line = localBufferedReader.readLine();
                if (line == null) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
