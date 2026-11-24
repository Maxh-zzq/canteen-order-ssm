package com.xatu.canteen.vo;

import com.xatu.canteen.entity.Stall;
import lombok.Data;

import java.io.Serializable;

/**
 * 档口信息。
 */
@Data
public class StallVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String floor;
    private String windowNo;
    private String openTime;
    private String closeTime;
    private Integer status;

    public static StallVO from(Stall stall) {
        if (stall == null) {
            return null;
        }
        StallVO vo = new StallVO();
        vo.setId(stall.getId());
        vo.setName(stall.getName());
        vo.setFloor(stall.getFloor());
        vo.setWindowNo(stall.getWindowNo());
        vo.setOpenTime(stall.getOpenTime());
        vo.setCloseTime(stall.getCloseTime());
        vo.setStatus(stall.getStatus());
        return vo;
    }
}
