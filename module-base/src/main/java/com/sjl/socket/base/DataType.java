package com.sjl.socket.base;

/**
 * TODO
 *
 * @author Kelly
 * @version 1.0.0
 * @filename CmdType
 * @time 2021/8/4 12:07
 * @copyright(C) 2021 song
 */
public enum DataType {
    TEXT_ONLY((byte) 0x01), FILE_AND_TEXT((byte) 0x02),HEART((byte) 0x99);
    private final byte dataType;

    DataType(byte dataType) {
        this.dataType = dataType;
    }

    public byte getDataType() {
        return dataType;
    }
    public static DataType parseType(byte type) {
        DataType dataType = null;
        for (DataType item : DataType.values()) {
            if (item.dataType == type) {
                dataType = item;
                break;
            }
        }
        return dataType;
    }
}
