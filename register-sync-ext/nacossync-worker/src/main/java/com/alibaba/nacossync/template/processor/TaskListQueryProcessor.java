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

import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.pojo.page.Pagination;
import com.alibaba.nacossync.pojo.request.TaskListQueryRequest;
import com.alibaba.nacossync.pojo.result.TaskListQueryResult;
import com.alibaba.nacossync.pojo.view.TaskModel;
import com.alibaba.nacossync.template.Processor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author NacosSync
 * @version $Id: TaskListQueryProcessor.java, v 0.1 2018-09-30 PM1:01 NacosSync Exp $$
 */
@Service
@Slf4j
public class TaskListQueryProcessor implements Processor<TaskListQueryRequest, TaskListQueryResult> {

    @Autowired
    private TaskAccessService taskAccessService;

    @Override
    public void process(TaskListQueryRequest taskListQueryRequest,
                        TaskListQueryResult taskListQueryResult, Object... others) {

        Integer pageNum = taskListQueryRequest.getPageNum();
        Integer pageSize = taskListQueryRequest.getPageSize();
        String serviceName = taskListQueryRequest.getServiceName();
        Pagination<TaskDO> pagination = taskAccessService.findPage(pageNum, pageSize, serviceName);

        List<TaskModel> taskList = pagination.getResult().stream().map(val -> {
            TaskModel taskModel = new TaskModel();
            taskModel.setTaskId(val.getTaskId());
            taskModel.setDestClusterId(val.getDestClusterId());
            taskModel.setSourceClusterId(val.getSourceClusterId());
            taskModel.setServiceName(val.getServiceName());
            taskModel.setGroupName(val.getGroupName());
            taskModel.setTaskStatus(val.getTaskStatus());
            return taskModel;
        }).collect(Collectors.toList());

        taskListQueryResult.setTaskModels(taskList);
        taskListQueryResult.setTotalPage(pagination.getTotalPage());
        taskListQueryResult.setTotalSize(pagination.getTotalCount());
        taskListQueryResult.setCurrentSize(taskList.size());
    }
}
