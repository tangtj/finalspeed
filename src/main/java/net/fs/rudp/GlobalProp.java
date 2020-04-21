package net.fs.rudp;

import net.fs.utils.RunMode;
import net.fs.utils.SystemType;
import net.fs.utils.SystemUtils;

/**
 * @author TANG
 */
public class GlobalProp {

    public final SystemType systemType = SystemUtils.getSystem();

    private static volatile RunMode runMode = RunMode.Server;

    private GlobalProp(){

    }

    public static GlobalProp getInstance(){
        return Holder.INSTANCE;
    }

    public RunMode getRunMode(){
        return runMode;
    }

    public static final class Holder{

        private static final GlobalProp INSTANCE = new GlobalProp();

        public Holder(RunMode mode){
            runMode = mode;
        }

    }


}
