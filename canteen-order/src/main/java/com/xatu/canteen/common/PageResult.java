package com.xatu.canteen.common;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一分页结果。
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private long total;
    private long pages;
    private long pageNum;
    private long pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long pages, long pageNum, long pageSize, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPages(pages);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setList(list == null ? new ArrayList<>() : list);
        return result;
    }
}
