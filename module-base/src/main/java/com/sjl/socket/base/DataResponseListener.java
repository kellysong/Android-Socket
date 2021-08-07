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

    void heartBeatPacket(DataPacket dataPacket);

    void dataPacket(int cmd, DataPacket requestPacket, DataPacket responsePacket);
}
