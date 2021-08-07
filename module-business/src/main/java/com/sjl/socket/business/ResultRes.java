package com.sjl.socket.business;


/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ResultRes
 * @time 2021/8/6 10:11
 * @copyright(C) 2021 song
 */
public class ResultRes<T> {
    private int code;
    private String msg;
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DataRes{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
