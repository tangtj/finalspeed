package net.fs.utils;

public enum SystemType {

    Windows("windows"),

    Linux("linux");

    private final String type;

    SystemType(String name){
        this.type = name;
    }

    public String getType(){
        return type;
    }
}
