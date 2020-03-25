package net.fs.utils;

public enum RunMode {

    /**
     *  客户端
     */
    Client(1),

    /**
     *  服务端
     */
    Server(2);

    public final int code;

    RunMode(int code){
        this.code = code;
    }
}
