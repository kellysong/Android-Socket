package com.sjl.socket.base;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * 数据包定义：数据包=类型+命令+包序列号+消息体+结束符
 * 消息体：长度+数据
 * <p>
 * <pre>
 *      1.纯文本：
 *      包类型byte，标志byte，命令int,包序列号int,文本长度 int,文本体 byte[]，结束符 byte
 * <pre>
 * <pre>
 *      2.文本和文件：
 *      包类型byte，标志byte，命令int,包序列号int,文本长度 int，文本体 byte[]，
 *      分隔符 byte，文件名长度 int，文件名byte[]，
 *      分隔符 byte,文件数据长度 long,文件数据 byte[]，结束符 byte
 * <pre>
 *
 * @author Kelly
 * @version 1.0.0
 * @filename DataPacket
 * @time 2021/8/4 14:56
 * @copyright(C) 2021 song
 */
public class DataPacket {
    public static final String ENCODE = "UTF-8";

    public static final byte CLIENT_REQUEST = 0x00;
    public static final byte SERVER_REQUEST = 0x01;

    /**
     * 报文类型
     */
    public byte dataType;
    /**
     * 标志：0客服端发起的请求，1服务端发起的请求，响应请求时把flag带回去
     */
    public byte flag;
    /**
     * 命令，响应请求时把cmd带回去
     */
    public int cmd;

    /**
     * 包序列号,数据包的唯一索引，响应请求时把seq带回去
     */
    public int seq;

    /**
     * 长度
     */
    public int textLength;
    /**
     * 文本体
     */
    public byte[] textData;


    /**
     * 文件分隔符
     */
    public static final byte Spilt = (byte) 0x63;


    /**
     * 文件名长度
     */
    public int fileNameLength;
    /**
     * 文件名
     */
    public byte[] fileNameData;

    /**
     * 长度
     */
    public long fileLength;

    /**
     * 文件,暂时支持单个文件传输
     */
    public File file;
    /**
     * 暂时不需要，因为边读编写,一次写文件容易oom
     */
    public byte[] fileData;
    /**
     * 结束符
     */
    public byte end;

    /**
     * 报文结束符
     */
    public static final byte End = (byte) 0xA;

    public DataPacket() {
    }

    public DataPacket(byte dataType, byte flag, int cmd, int seq) {
        this.dataType = dataType;
        this.flag = flag;
        this.cmd = cmd;
        this.seq = seq;
    }

    public DataPacket(byte dataType, byte flag, int cmd, int seq, byte[] textData) {
        this.dataType = dataType;
        this.flag = flag;
        this.cmd = cmd;
        this.seq = seq;
        this.textLength = textData.length;
        this.textData = textData;

    }


    public DataPacket(byte dataType, byte flag, int cmd, int seq, byte[] textData, byte[] fileNameData, File file) {
        this.dataType = dataType;
        this.flag = flag;
        this.cmd = cmd;
        this.seq = seq;
        this.textLength = textData.length;
        this.textData = textData;
        this.fileNameLength = fileNameData.length;
        this.fileNameData = fileNameData;
        this.fileLength = file.length();
        this.file = file;

    }


    public void sendHeart(DataOutputStream out) throws IOException {
        //占用1个字节
        out.writeByte(dataType);
        out.writeByte(flag);
        out.writeInt(cmd);
        out.writeInt(seq);
        //结束符,占用1个字节
        out.writeByte(End);
        out.flush();
    }

    public void writeText(DataOutputStream out) throws IOException {
        //占用1个字节
        out.writeByte(dataType);
        out.writeByte(flag);
        out.writeInt(cmd);
        out.writeInt(seq);
        out.writeInt(textLength);
        out.write(textData);

        //结束符,占用1个字节
        out.writeByte(End);
        out.flush();
    }

    public void writeTextAndFile(DataOutputStream out) throws IOException {
        //占用1个字节
        out.writeByte(dataType);
        out.writeByte(flag);
        out.writeInt(cmd);
        out.writeInt(seq);
        out.writeInt(textLength);
        out.write(textData);

        if (fileLength > 0) {
            //文件分隔符和文本数据
            out.writeByte(DataPacket.Spilt);//占用1个字节
            //发送文件名
            out.writeInt(fileNameLength);
            out.write(fileNameData);
            //发送文件
            out.writeByte(DataPacket.Spilt);//占用1个字节
            out.writeLong(fileLength);//文件的长度，占用8个字节
            writeFile(out, file);
        }
        //结束符,占用1个字节
        out.writeByte(End);
        out.flush();
    }

    private void writeFile(DataOutputStream out, File file) throws IOException {
        InputStream is = new FileInputStream(file.getPath());
        byte[] c = new byte[1024 * 4];
        int b;
        while ((b = is.read(c)) > 0) {
            out.write(c, 0, b);
        }
        is.close();
    }


    public void readHeart(byte dataType, DataInputStream inputStream) throws IOException {
        byte flag = inputStream.readByte();
        int cmd = inputStream.readInt();
        int seq = inputStream.readInt();
        byte end = inputStream.readByte();

        this.dataType = dataType;
        this.flag = flag;
        this.cmd = cmd;
        this.seq = seq;
        this.end = end;
    }

    /**
     * 按顺序解析
     *
     * @param dataType
     * @param inputStream
     * @throws IOException
     */
    public void readText(byte dataType, DataInputStream inputStream) throws IOException {
        byte flag = inputStream.readByte();
        int cmd = inputStream.readInt();
        int seq = inputStream.readInt();
        int textLength = inputStream.readInt();
        byte[] data = new byte[textLength];
        inputStream.readFully(data);
        byte end = inputStream.readByte();

        this.dataType = dataType;
        this.flag = flag;
        this.cmd = cmd;
        this.seq = seq;
        this.textLength = textLength;
        this.textData = data;
        this.end = end;
    }


    public void readTextAndFile(byte dataType, DataInputStream inputStream, File dir) throws IOException {
        long start = System.currentTimeMillis();
        byte flag = inputStream.readByte();
        int cmd = inputStream.readInt();
        int seq = inputStream.readInt();
        //解析文本
        int textLength = inputStream.readInt();
        byte[] data = new byte[textLength];
        inputStream.readFully(data);

        byte spiltChar = inputStream.readByte();
        if (spiltChar != DataPacket.Spilt) {
            throw new IllegalArgumentException("非法字节，无法解析文件名：" + spiltChar);
        }
        //解析文件名
        int fileNameLength = inputStream.readInt();
        byte[] fileNameData = new byte[fileNameLength];
        inputStream.readFully(fileNameData);

        spiltChar = inputStream.readByte();
        if (spiltChar != DataPacket.Spilt) {
            throw new IllegalArgumentException("非法字节，无法文件数据:" + spiltChar);
        }
        //解析文件数据
        long fileLength = inputStream.readLong();
        ConsoleUtils.i("fileLength:" + fileLength);

        String fileName = new String(fileNameData);
        File file = new File(dir, fileName);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream os = new FileOutputStream(file);

        byte[] buffer = new byte[1024 * 10];
        int ret;
        int readLength = 0;
        int surplus = buffer.length;
        //80864108
        if (fileLength <= buffer.length) {
            surplus = (int) fileLength;
        }
        //非堵塞读取，读多一个字节都会卡死
        while ((ret = inputStream.read(buffer, 0, surplus)) != -1) {
            os.write(buffer, 0, ret);
            readLength += ret;
            surplus = (int) (fileLength - readLength);
            if (surplus >= buffer.length) {
                surplus = buffer.length;
            }
            ConsoleUtils.i("readLength:" + readLength);
            if (readLength == fileLength) {
                ConsoleUtils.i("读取文件完毕");
                break;
            }
        }
        os.close();
        ConsoleUtils.i("文件读取耗时：" + (System.currentTimeMillis() - start) / 1000.0 + "s");
        byte end = inputStream.readByte();

        this.dataType = dataType;
        this.flag = flag;
        this.cmd = cmd;
        this.seq = seq;
        this.textLength = textLength;
        this.textData = data;
        this.fileNameLength = fileNameLength;
        this.fileNameData = fileNameData;
        this.fileLength = fileLength;
        this.file = file;
        this.end = end;
        ConsoleUtils.i("发送文件成功：" + fileName);
    }

    @Override
    public String toString() {
        return "DataPacket{" +
                "dataType=" + dataType +
                ", flag=" + flag +
                ", cmd=" + cmd +
                ", seq=" + seq +
                ", textData=" + (textData != null ? new String(textData) : null) +
                ", file=" + (file != null ? file.getAbsolutePath() : null) +
                '}';
    }
}
