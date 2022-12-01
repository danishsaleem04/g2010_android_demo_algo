package com.oxi.g2010.demo;

/**
 * Created by lei.feng.chn@gmail.com on 2017/9/25.
 */

public class ErrorCode {
    /**
     * success
     */
    public static final int OXI_OK = 1;    // 正确, success


    /**
     *param_err
     */
    public static final int OXI_PARAM_ERR = -1; //参数错误, param_err

    public static final int OXI_MEMORY_ERR = -2; //内存错误
    public static final int OXI_NOT_DEVELOP = -3; //功能不存在

    /**
     * No device
     */
    public static final int OXI_NO_DEVICE = -4;

    /**
     * Device not init
     */
    public static final int OXI_DEV_NO_INIT = -5;

    public static final int OXI_ILLEGAL_ERR = -6; //非法错误号
    public static final int OXI_OTHER_ERR = -9; //其他错误

    public static final int OXI_SEND_FAIL = -7;  //发送失败
    public static final int OXI_RCV_FAIL = - 8;  //接收失败

    /**
     * No touch
     */
    public static final int OXI_NO_TOUCH = -10;

    /**
     * check status fail
     */
    public static final int OXI_STATUS_FAIL = -11;

    /**
     * no permission
     */
    public static final int OXI_NO_PERMISSION = -12;
}
