package com.alibaba.nacossync.template.processor;

import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.pojo.request.TaskInitRequest;
import com.alibaba.nacossync.pojo.result.BaseResult;
import com.alibaba.nacossync.service.factory.SyncFactory;
import com.alibaba.nacossync.template.Processor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by liaomengge on 2020/6/17.
 */
@Slf4j
@Service
public class TaskInitProcessor implements Processor<TaskInitRequest, BaseResult> {

    public static final String INIT_VALUE = "1";

    @Value("${sync.etcd.init.token}")
    private String token;

    @Autowired
    private SyncFactory syncFactory;

    private AtomicBoolean atomicBoolean = new AtomicBoolean(false);

    @Override
    public void process(TaskInitRequest taskInitRequest, BaseResult baseResult, Object... others) throws Exception {
        String reqToken = taskInitRequest.getToken();
        if (!StringUtils.equals(token, reqToken)) {
            baseResult.setResultMessage("Token Invalid");
            return;
        }

        if (atomicBoolean.compareAndSet(false, true)) {
            try {
                //eureka sync data to etcd
                syncFactory.getSync(ClusterTypeEnum.EUREKA).syncClusterData();
            } catch (Exception e) {
                log.error("init eureka data to etcd fail", e);
            }

            try {
                //eureka sync data to etcd
                syncFactory.getSync(ClusterTypeEnum.NACOS).syncClusterData();
            } catch (Exception e) {
                log.error("init nacos data to etcd fail", e);
            }

            baseResult.setResultMessage("Sync Data Success");
            return;
        }
        baseResult.setResultMessage("No Allow Repeat Init Etcd Data");
        log.warn("Single Process No Allow Multi Init Etcd Data...");
    }
}
