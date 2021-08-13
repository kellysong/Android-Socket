package com.sjl.socket;

import com.google.gson.Gson;
import com.sjl.socket.base.DataRes;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename BaseSocket
 * @time 2021/8/7 11:32
 * @copyright(C) 2021 song
 */
public class BaseSocket {

    protected boolean running;
    protected static Gson gson = new Gson();
    private static DataRes dataRes = new DataRes();

    protected static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(500));



    protected File fileSaveDir;

    protected byte REQUEST_FLAG;


    public boolean isRunning() {
        return running;
    }

    /**
     * 文件保存目录
     *
     * @param fileSaveDir
     */
    public void setFileSaveDir(String fileSaveDir) {
        File dir = new File(fileSaveDir);
        if (!dir.exists()) {
            boolean mkdir = dir.mkdir();
            if (!mkdir) {
                throw new RuntimeException("创建文件目录失败");
            }
        }
        this.fileSaveDir = dir;
    }

    public static DataRes getDataRes() {
        return dataRes;
    }

}
