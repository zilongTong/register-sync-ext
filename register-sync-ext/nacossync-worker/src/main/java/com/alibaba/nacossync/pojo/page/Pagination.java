package com.alibaba.nacossync.pojo.page;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by liaomengge on 2020/6/15.
 */
@Getter
@Setter
public class Pagination<T> {

    /**
     * 当前页数
     */
    private int pageNo;

    /**
     * 每页显示个数
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPage;

    /**
     * 总记录数
     */
    private long totalCount;

    /**
     * 结果列表
     */
    private List<T> result;

    public Pagination(int pageNo, int pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public void buildTotalPage() {
        int divisor = (int) (totalCount / getPageSize());
        int remainder = (int) (totalCount % getPageSize());
        setTotalPage(remainder == 0 ? divisor == 0 ? 1 : divisor : divisor + 1);
    }
}
