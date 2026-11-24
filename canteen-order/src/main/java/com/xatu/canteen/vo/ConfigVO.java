package com.xatu.canteen.vo;

import com.xatu.canteen.entity.SysConfig;
import lombok.Data;

import java.io.Serializable;

/**
 * 参数配置展示。
 */
@Data
public class ConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String configKey;
    private String configValue;
    private String remark;

    public static ConfigVO from(SysConfig config) {
        if (config == null) {
            return null;
        }
        ConfigVO vo = new ConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigValue(config.getConfigValue());
        vo.setRemark(config.getRemark());
        return vo;
    }
}
