package com.freak.commonappframework.net;



/**
 * @author freak
 * @date 2019/2/19
 */

public class HttpResult<T>  {
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
        StringBuffer sb = new StringBuffer();
        if (null != data) {
            sb.append(data.toString());
        }
        return sb.toString();
    }
}
