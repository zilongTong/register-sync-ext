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
package com.alibaba.nacossync.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacossync.constant.EtcdConstants;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.pojo.page.Pagination;
import com.alibaba.nacossync.util.*;
import com.google.common.collect.Lists;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.GetOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author NacosSync
 * @version $Id: TaskAccessService.java, v 0.1 2018-09-25 AM12:07 NacosSync Exp $$
 */
@Service
public class TaskAccessService {

    @Autowired
    private Client client;

    public TaskDO findByTaskId(String taskId) {
        ByteSequence key = ByteSequenceUtil.fromString(generateTaskIdNode(taskId));
        CompletableFuture<GetResponse> future = client.getKVClient().get(key);
        return CompletableFutureUtil.handle(future, resp -> EtcdResponseUtil.get(resp, TaskDO.class));
    }

    public List<TaskDO> findByServiceName(String serviceName) {
        List<TaskDO> taskDOList = buildTaskCaffeineCache();
        return taskDOList.stream().filter(val -> org.apache.commons.lang3.StringUtils.equals(val.getServiceName(),
                serviceName)).collect(Collectors.toList());
    }

    public TaskDO findByOperationId(String operationId) {
        List<TaskDO> taskDOList = buildTaskCaffeineCache();
        return taskDOList.stream().filter(val -> org.apache.commons.lang3.StringUtils.equals(val.getOperationId(),
                operationId)).findFirst().orElse(null);
    }

    public Long deleteTaskById(String taskId) {
        ByteSequence key = ByteSequenceUtil.fromString(generateTaskIdNode(taskId));
        CompletableFuture<DeleteResponse> future = client.getKVClient().delete(key);
        return CompletableFutureUtil.handle(future, EtcdResponseUtil::get);
    }

    public boolean deleteTaskInBatch(List<String> taskIds) {
        long affectCount = taskIds.stream().mapToLong(this::deleteTaskById).sum();
        return affectCount == taskIds.size();
    }

    public List<TaskDO> findAll() {
        return buildTaskCaffeineCache();
    }

    public TaskDO addTask(TaskDO taskDO) {
        String taskId = taskDO.getTaskId();
        ByteSequence key = ByteSequenceUtil.fromString(generateTaskIdNode(taskId));
        ByteSequence value = ByteSequenceUtil.fromString(JSON.toJSONString(taskDO));
        CompletableFuture<PutResponse> future = client.getKVClient().put(key, value);
        return CompletableFutureUtil.handle(future, resp -> EtcdResponseUtil.get(resp, TaskDO.class));
    }

    public Pagination<TaskDO> findPage(Integer pageNum, Integer pageSize, String serviceName) {
        List<TaskDO> taskDOList = buildTaskCaffeineCache();
        if (!StringUtils.isEmpty(serviceName)) {
            taskDOList =
                    taskDOList.stream().filter(Objects::nonNull).filter(val -> val.getServiceName().contains(serviceName)).collect(Collectors.toList());
        }
        return RamPageUtil.page(taskDOList, pageNum, pageSize);
    }

    public List<TaskDO> buildTaskCaffeineCache() {
        String cacheKey = EtcdConstants.ETCD_TASK_CAFFEINE;
        return Optional.ofNullable(CaffeineUtil.get(cacheKey, val -> {
            String rootNode = EtcdConstants.ETCD_TASK_ROOT_NODE;
            ByteSequence key = ByteSequenceUtil.fromString(rootNode);
            CompletableFuture<GetResponse> future = client.getKVClient()
                    .get(key,
                            GetOption.newBuilder().withPrefix(key).withSortOrder(GetOption.SortOrder.DESCEND).build());
            return CompletableFutureUtil.handle(future, resp -> EtcdResponseUtil.getAll(resp, TaskDO.class));
        })).orElse(Lists.newArrayList());
    }

    private String generateTaskIdNode(String taskId) {
        return EtcdConstants.ETCD_TASK_ROOT_NODE + EtcdConstants.DELIMITER + taskId;
    }

}
