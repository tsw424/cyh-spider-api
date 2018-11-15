package cn.zero.spider.util;

import java.io.Serializable;

/**
 * @author 蔡元豪
 * @date 2018/11/15 15:45
 */
public class Ajax implements Serializable {
    private int code=200;
    private Object data;
    private String message;

    public Ajax(Object data) {
        this.data = data;
    }

    public Ajax(Object data, String message) {
        this.data = data;
        this.message = message;
    }

    public Ajax(int code, Object data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Ajax{" +
                "code=" + code +
                ", data=" + data +
                ", message='" + message + '\'' +
                '}';
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
