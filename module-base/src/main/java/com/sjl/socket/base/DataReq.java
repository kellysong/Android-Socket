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

    public Object data;
    public File file;
    public abstract DataType geDataType();

    public abstract int getCmd();


    public DataReq(Object data) {
        this.data = data;
    }
    public DataReq(Object data, File file) {
        this.data = data;
        this.file = file;
    }


}
