package net.fs.utils;

public class SystemUtils {

    public static boolean isLinux(String systemName){
        return SystemType.Linux.getType().equals(systemName);
    }

    public static boolean isWindows(String systemName){
        if (systemName == null || systemName.isEmpty()){
            return false;
        }
        return systemName.contains(SystemType.Windows.getType());
    }

    public static SystemType getSystem(String systemName){
        if (isLinux(systemName)){
            return SystemType.Linux;
        }else if (isWindows(systemName)){
            return SystemType.Windows;
        }
        //先默认返回linux客户端
        return SystemType.Linux;
    }
}
