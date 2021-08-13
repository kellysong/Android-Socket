package com.sjl.socket.base;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename HeartbeatListener
 * @time 2021/8/13 11:31
 * @copyright(C) 2021 song
 */
public interface HeartbeatListener {
    /**
     * 心跳回调
     * @param requestPacket
     * @param responsePacket
     * @param source
     */
    void heartBeatPacket(DataPacket requestPacket, DataPacket responsePacket,String source);
}
