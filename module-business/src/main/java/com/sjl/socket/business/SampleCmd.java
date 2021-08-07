package com.sjl.socket.business;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename SampleCmd
 * @time 2021/8/5 18:25
 * @copyright(C) 2021 song
 */
public enum  SampleCmd {
    /**
     * 上传apk
     */
    UPLOAD_APK(100, "上传apk"),
    NORMAL_TEXT(101, "普通文本");
    private int value;
    private String desc;

    SampleCmd(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return this.value;
    }

    public String getDesc() {
        return desc;
    }

    public static SampleCmd fromValue(int value) {
        for (SampleCmd SampleCmd : values()) {
            if (SampleCmd.getValue() == value) {
                return SampleCmd;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SampleCmd{" +
                "value=" + value +
                ", desc='" + desc + '\'' +
                '}';
    }

}
