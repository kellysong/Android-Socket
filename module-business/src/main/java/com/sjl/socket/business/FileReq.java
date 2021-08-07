package com.sjl.socket.business;


import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataType;

import java.io.File;

/**
 *
 *
 * @author Kelly
 * @version 1.0.0
 * @filename FileReq
 * @time 2021/8/4 12:05
 * @copyright(C) 2021 song
 */
public class FileReq extends DataReq {



    public FileReq(Object data, File file) {
        super(data, file);
    }

    @Override
    public String toString() {
        return "ApkReq{" +
                ", cmd=" + getCmd() +
                ", dataType='" + geDataType() + '\'' +
                ", apkName='" + file.getName() + '\'' +
                ", apkLength='" + file.length() + '\'' +
                ", data='" + data+ '\'' +
                '}';
    }

    @Override
    public DataType geDataType() {
        return DataType.FILE_AND_TEXT;
    }

    @Override
    public int getCmd() {
        return SampleCmd.UPLOAD_APK.getValue();
    }
}
