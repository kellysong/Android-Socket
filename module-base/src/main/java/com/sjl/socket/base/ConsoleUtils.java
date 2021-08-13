package com.sjl.socket.base;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 控制台日志工具类
 *
 * @author Kelly
 * @version 1.0.0
 * @filename ConsoleUtils
 * @time 2021/8/11 11:24
 * @copyright(C) 2021 song
 */
public class ConsoleUtils {

    private static final int LOG_DEBUG = 1;
    private static final int LOG_INFO = 2;
    private static final int LOG_WARN = 3;
    private static final int LOG_ERROR = 4;
    private static final int PLATFORM_ANDROID = 0;
    private static final int PLATFORM_PC = 1;

    /**
     * 是否显示日志
     */
    private static boolean debug = true;


    public static void d(Object msg) {
        printLog(LOG_DEBUG, msg, null);
    }


    public static void i(Object msg) {
        printLog(LOG_INFO, msg, null);
    }


    public static void w(Object msg) {
        printLog(LOG_WARN, msg, null);
    }


    public static void w(Object msg, Throwable tr) {
        printLog(LOG_WARN, msg, tr);
    }

    public static void e(Object msg) {
        printLog(LOG_ERROR, msg, null);
    }

    public static void e(Object msg, Throwable tr) {
        printLog(LOG_ERROR, msg, tr);
    }

    /**
     * 打印日志
     *
     * @param type
     * @param msgObj
     * @param tr
     * @return
     */
    private static int printLog(int type, Object msgObj, Throwable tr) {
        if (!debug) {
            return -1;
        }
        String level = "--";
        switch (type) {
            case LOG_DEBUG:
                level = "D";
                break;
            case LOG_INFO:
                level = "I";
                break;
            case LOG_WARN:
                level = "W";
                break;
            case LOG_ERROR:
                level = "E";
                break;
        }
        int platformFlag = checkPlatform();
        String content = createLog(msgObj.toString(), platformFlag) + (tr != null ? ('\n' + getStackTraceString(tr)) : "");
        StringBuilder sb = new StringBuilder();
        String formatDate = getFormatTime();
        if (platformFlag == PLATFORM_ANDROID) {
            sb.append(content);
        } else {
            sb.append(formatDate).append("/").append(level).append(":").append(content);
        }

        System.out.println(sb.toString());
        return 0;
    }

    /**
     * 创建日志定位信息
     *
     * @param msg
     * @param platformFlag
     * @return
     */
    private static String createLog(String msg, int platformFlag) {
        StringBuilder builder = new StringBuilder();
        try {
            Thread thread = Thread.currentThread();
            int stackTraceIndex;
            if (platformFlag == PLATFORM_ANDROID) { //这里根据把ConsoleUtils所在的位置确认，可以打个断点确认
                stackTraceIndex = 5;
            } else {
                stackTraceIndex = 4;
            }
            StackTraceElement[] stackTrace = thread.getStackTrace();
            String className = stackTrace[stackTraceIndex].getFileName();
            String methodName = stackTrace[stackTraceIndex].getMethodName();
            int lineNumber = stackTrace[stackTraceIndex].getLineNumber();
            builder.append(methodName);
            builder.append("(").append(className).append(":").append(lineNumber).append(")");
            builder.append(msg);
            builder.append("  ---->").append("Thread:").append(thread.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static int checkPlatform() {
        try {
            Class.forName("android.os.Build");
            return PLATFORM_ANDROID;
        } catch (ClassNotFoundException ignored) {
            return PLATFORM_PC;
        }
    }

    /**
     * 获取日志异常栈信息
     *
     * @param tr
     * @return
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, false);
        tr.printStackTrace(pw);
        pw.flush();
        pw.close();
        return sw.toString();
    }

    private static String getFormatTime() {
        Date date = new Date();
        String strDateFormat = "yyyy-MM-dd mm:ss:SSS";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        return sdf.format(date);
    }
}
