package com.sjl.socket.business;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename FileInfo
 * @time 2021/8/5 21:10
 * @copyright(C) 2021 song
 */
public class FileInfo {
    public String msg;
    public String desc = "这是一个apk：" + System.currentTimeMillis();

    @Override
    public String toString() {
        return "FileInfo{" +
                "desc='" + desc + '\'' +
                '}';
    }
}
