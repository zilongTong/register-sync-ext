package com.alibaba.nacossync.template.processor;

import com.alibaba.nacossync.constant.EtcdConstants;
import com.alibaba.nacossync.pojo.request.TaskInvalidateRequest;
import com.alibaba.nacossync.pojo.result.BaseResult;
import com.alibaba.nacossync.template.Processor;
import com.alibaba.nacossync.util.CaffeineUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Created by liaomengge on 2020/6/17.
 */
@Slf4j
@Service
public class TaskInvalidateProcessor implements Processor<TaskInvalidateRequest, BaseResult> {

    @Value("${sync.etcd.init.token}")
    private String token;

    @Override
    public void process(TaskInvalidateRequest taskInvalidateRequest, BaseResult baseResult, Object... others) throws Exception {
        String reqToken = taskInvalidateRequest.getToken();
        if (!StringUtils.equals(token, reqToken)) {
            baseResult.setResultMessage("Token Invalid");
            return;
        }
        CaffeineUtil.invalidate(EtcdConstants.ETCD_TASK_CAFFEINE);
        baseResult.setResultMessage("Invalidate Task Cache Success");
    }
}
