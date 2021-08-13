package com.sjl.socket.base;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename DataResponseListener
 * @time 2021/8/5 17:42
 * @copyright(C) 2021 song
 */
public interface DataResponseListener {

    /**
     * 发送成功
     * @param cmd
     * @param requestPacket
     * @param responsePacket
     */
    void sendOnSuccess(int cmd, DataPacket requestPacket, DataPacket responsePacket);

    /**
     * 发送失败
     * @param cmd
     * @param t
     */
    void sendOnError(int cmd, Throwable t);

}
