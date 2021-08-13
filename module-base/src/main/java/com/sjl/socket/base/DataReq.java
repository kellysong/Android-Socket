package com.sjl.socket.base;

import java.io.File;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename DataReq
 * @time 2021/8/5 18:33
 * @copyright(C) 2021 song
 */
public abstract class DataReq {
    public abstract DataType geDataType();

    public Object data;
    public File file;
    public String fileName;


    /**
     * 1已经占用，作为心跳的命令号
     *
     * @return
     */
    public abstract int getCmd();

    /**
     * 报文序列号，调用一次增加一次
     *
     * @return
     */
    public abstract int getSeq();


    public DataReq(Object data) {
        this.data = data;
    }

    public DataReq(Object data, File file, String fileName) {
        this.data = data;
        this.file = file;
        this.fileName = fileName;
    }


}
