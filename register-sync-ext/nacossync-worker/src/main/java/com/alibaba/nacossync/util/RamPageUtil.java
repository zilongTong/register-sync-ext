package com.alibaba.nacossync.util;

import com.alibaba.nacossync.pojo.page.Pagination;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * Created by liaomengge on 2020/6/15.
 */
@UtilityClass
public class RamPageUtil {

    public static final int DEFAULT_PAGE_NO = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 从第一页开始
     *
     * @param list
     * @param pageNo
     * @param pageSize
     * @param <T>
     * @return
     */
    public <T> Pagination<T> page(List<T> list, Integer pageNo, Integer pageSize) {
        Pagination<T> pagination = new Pagination<>(pageNo, pageSize);
        if (CollectionUtils.isEmpty(list)) {
            pagination.setResult(Lists.newArrayList());
            return pagination;
        }
        if (Objects.isNull(pageNo)) {
            pageNo = DEFAULT_PAGE_NO;
        }
        if (Objects.isNull(pageSize)) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageNo < 1) {
            pageNo = 1;
        }
        int from = (pageNo - 1) * pageSize;
        int to = Math.min(pageNo * pageSize, list.size());
        if (from > to) {
            from = to;
        }
        pagination.setTotalCount(list.size());
        pagination.buildTotalPage();
        pagination.setResult(list.subList(from, to));
        return pagination;
    }
}
