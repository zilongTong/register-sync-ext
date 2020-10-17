/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacossync.template.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.event.DeleteTaskEvent;
import com.alibaba.nacossync.exception.SkyWalkerException;
import com.alibaba.nacossync.extension.ha.ConsistentHashSyncShardingEtcdProxy;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.pojo.request.TaskUpdateRequest;
import com.alibaba.nacossync.pojo.result.BaseResult;
import com.alibaba.nacossync.template.Processor;
import com.alibaba.nacossync.util.IPUtils;
import com.alibaba.nacossync.util.OkHttpUtil;
import com.alibaba.nacossync.util.RequestHolderUtil;
import com.alibaba.nacossync.util.SkyWalkerUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * @author NacosSync
 * @version $Id: TaskUpdateProcessor.java, v 0.1 2018-10-17 PM11:11 NacosSync Exp $$
 */
@Slf4j
@Service
public class TaskUpdateProcessor implements Processor<TaskUpdateRequest, BaseResult> {

    private static final String HEADER_SIGN = "1";

    @Autowired
    private TaskAccessService taskAccessService;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private ConsistentHashSyncShardingEtcdProxy proxy;

    @Autowired
    private ServerProperties serverProperties;

    @Override
    public void process(TaskUpdateRequest taskUpdateRequest, BaseResult baseResult,
                        Object... others) throws Exception {
        TaskDO taskDO = taskAccessService.findByTaskId(taskUpdateRequest.getTaskId());

        if (!TaskStatusEnum.contains(taskUpdateRequest.getTaskStatus())) {
            throw new SkyWalkerException(
                    "taskUpdateRequest.getTaskStatus() is not exist , value is :"
                            + taskUpdateRequest.getTaskStatus());
        }

        if (null == taskDO) {
            throw new SkyWalkerException("taskDo is null ,taskId is :"
                    + taskUpdateRequest.getTaskId());
        }

        if (!StringUtils.equals(taskDO.getTaskStatus(), taskUpdateRequest.getTaskStatus())) {
            taskDO.setTaskStatus(taskUpdateRequest.getTaskStatus());
            taskDO.setOperationId(SkyWalkerUtil.generateOperationId());

            taskAccessService.addTask(taskDO);
        }

        if (TaskStatusEnum.DELETE.getCode().equals(taskUpdateRequest.getTaskStatus())) {
            // 1.发布event
            log.info("publish event ===> {}", JSON.toJSONString(taskDO));
            eventBus.post(new DeleteTaskEvent(taskDO));

            // 2.同步到其他节点
            String headerSign = RequestHolderUtil.getHttpServletRequest().getHeader("HEADER_SIGN");
            if (!HEADER_SIGN.equals(headerSign)) {
                Set<String> nodeCaches = proxy.getNodeCaches();
                Set<String> tmpNodes = new HashSet<>(nodeCaches);
                try {
                    String localIp = IPUtils.getIpAddress();
                    tmpNodes.remove(localIp);
                } catch (Exception e) {
                    log.warn("get current ip fail", e);
                }
                tmpNodes.forEach(val -> {
                    try {
                        String url = "http://" + val + ":" + serverProperties.getPort() + "/v1/task/update";
                        TaskUpdateRequest request = new TaskUpdateRequest();
                        request.setTaskId(taskUpdateRequest.getTaskId());
                        request.setTaskStatus(taskUpdateRequest.getTaskStatus());
                        String result = OkHttpUtil.postJsonParams(url, JSON.toJSONString(taskUpdateRequest),
                                ImmutableMap.of("HEADER_SIGN", HEADER_SIGN));
                        log.info("request url[{}], params[{}], result[{}]", url, JSON.toJSONString(request), result);
                    } catch (Exception e) {
                        log.error("sync request other node[{}] fail", val, e);
                    }
                });

            }
        }
    }
}
