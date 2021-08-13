package com.sjl.socket.business;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SeqUtils
 * @time 2021/8/10 14:16
 * @copyright(C) 2021 song
 */
public class SeqUtils {
    static volatile int currentSeq = 0;

    /**
     * 可从数据库取
     * @return
     */
    public synchronized static int nextSeq() {
        currentSeq++;
        if (currentSeq == Integer.MAX_VALUE) {
            currentSeq = 1;
        }
        return currentSeq;
    }
}
