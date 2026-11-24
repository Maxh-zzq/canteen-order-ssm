package com.xatu.canteen.vo;

import com.xatu.canteen.entity.SysOperLog;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志展示。
 */
@Data
public class OperLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String traceId;
    private String username;
    private String module;
    private String method;
    private String params;
    private String ip;
    private Long costMs;
    private Integer success;
    private String errorMsg;
    private LocalDateTime createTime;

    public static OperLogVO from(SysOperLog log) {
        if (log == null) {
            return null;
        }
        OperLogVO vo = new OperLogVO();
        vo.setId(log.getId());
        vo.setTraceId(log.getTraceId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setMethod(log.getMethod());
        vo.setParams(log.getParams());
        vo.setIp(log.getIp());
        vo.setCostMs(log.getCostMs());
        vo.setSuccess(log.getSuccess());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
