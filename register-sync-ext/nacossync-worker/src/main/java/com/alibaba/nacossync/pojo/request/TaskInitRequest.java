package com.alibaba.nacossync.pojo.request;

import lombok.Data;

/**
 * Created by liaomengge on 2020/6/17.
 */
@Data
public class TaskInitRequest extends BaseRequest {
    private static final long serialVersionUID = -8087969062326244219L;

    private String token;
}
