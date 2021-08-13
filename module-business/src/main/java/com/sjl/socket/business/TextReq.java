package com.sjl.socket.business;


import com.sjl.socket.base.DataReq;
import com.sjl.socket.base.DataType;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename TextReq
 * @time 2021/8/6 11:13
 * @copyright(C) 2021 song
 */
public class TextReq extends DataReq {

    public TextReq(Object data) {
        super(data);
    }

    @Override
    public DataType geDataType() {
        return DataType.TEXT_ONLY;
    }

    @Override
    public int getCmd() {
        return SampleCmd.NORMAL_TEXT.getValue();
    }

    @Override
    public int getSeq() {
        return SeqUtils.nextSeq();
    }
}
