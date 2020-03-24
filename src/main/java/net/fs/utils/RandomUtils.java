package net.fs.utils;

import java.util.Random;

/**
 *  随机数方法
 *
 * @author TANG
 */
public class RandomUtils {

    private static final Random RANDOM =new Random();

    public static int randomInt(){
        return RANDOM.nextInt();
    }

    public static int randomInt(int bound){
        return RANDOM.nextInt(bound);
    }

    public static long randomLong(){
        return RANDOM.nextLong();
    }
}
