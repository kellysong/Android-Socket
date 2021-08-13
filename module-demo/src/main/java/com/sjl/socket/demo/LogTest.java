package com.sjl.socket.demo;


import com.sjl.socket.base.ConsoleUtils;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename LogTest
 * @time 2021/8/11 11:17
 * @copyright(C) 2021 song
 */
public class LogTest {
    public static void main(String[] args) {

        testLogPrintAfter();

    }

    private static void testLogPrintAfter() {

        ConsoleUtils.d("hello world!");
        ConsoleUtils.i("hello world!");
        ConsoleUtils.w("hello world!");

        try {
            LogTest logTest = null;
            logTest.clone();
        } catch (Exception e) {
            ConsoleUtils.w("发生了一个错误", e);
        }

        ConsoleUtils.e("hello world!");
        ConsoleUtils.e("hello world!", new RuntimeException("发生了一个错误"));
        try {
            Class.forName("java.util.Test");
        } catch (ClassNotFoundException ignored) {
            ConsoleUtils.e("hello world!", ignored);
        }
    }


    private static void testLogPrintBefore() {
        System.out.println("hello world!");
        try {
            LogTest logTest = null;
            logTest.clone();
        } catch (Exception e) {
            System.out.println("发生了一个错:" + e.getMessage());
        }

    }

}
