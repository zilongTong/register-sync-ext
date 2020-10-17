/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.alibaba.nacossync.pojo.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author NacosSync
 * @version $Id: TaskDo.java, v 0.1 2018-09-24 PM11:53 NacosSync Exp $$
 */
@Data

public class TaskDO implements Serializable {


    private Integer id;
    /**
     * custom task id(unique)
     */
    private String taskId;
    /**
     * source cluster id
     */
    private String sourceClusterId;
    /**
     * destination cluster id
     */
    private String destClusterId;
    /**
     * service name
     */
    private String serviceName;
    /**
     * version
     */
    private String version;
    /**
     * group name
     */
    private String groupName;
    /**
     * name space
     */
    private String nameSpace;

    /**
     * the current task status
     */
    private String taskStatus;
    /**
     * The IP address that performs the current task
     */
    private String workerIp;
    /**
     * operation id,The operation id follow when the task status changes
     */
    private String operationId;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDO taskDO = (TaskDO) o;
        return Objects.equals(sourceClusterId, taskDO.sourceClusterId) &&
                Objects.equals(destClusterId, taskDO.destClusterId) &&
                Objects.equals(serviceName, taskDO.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceClusterId, destClusterId, serviceName);
    }

    public TaskDO() {
    }
}
