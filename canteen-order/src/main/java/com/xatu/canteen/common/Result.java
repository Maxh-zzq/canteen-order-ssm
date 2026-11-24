package com.xatu.canteen.common;

import lombok.Data;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一响应体。
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务码：200 成功，其余见 {@link ResultCode} */
    private int code;
    private String message;
    private T data;
    /** 链路追踪号，排查问题时提供给运维 */
    private String traceId;
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return fail(resultCode.getCode(), message);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }
}
