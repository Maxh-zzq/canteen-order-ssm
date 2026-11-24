package com.xatu.canteen.vo;

import com.xatu.canteen.entity.Review;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 评价展示。
 */
@Data
public class ReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private Integer score;
    private String content;
    private List<String> images;
    private LocalDateTime createTime;

    public static ReviewVO from(Review review) {
        if (review == null) {
            return null;
        }
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setUsername(review.getUsername());
        vo.setScore(review.getScore());
        vo.setContent(review.getContent());
        vo.setCreateTime(review.getCreateTime());
        List<String> images = new ArrayList<>();
        if (StringUtils.hasText(review.getImages())) {
            Arrays.stream(review.getImages().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(images::add);
        }
        vo.setImages(images);
        return vo;
    }
}
