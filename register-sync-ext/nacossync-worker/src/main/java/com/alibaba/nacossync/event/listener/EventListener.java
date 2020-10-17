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
package com.alibaba.nacossync.event.listener;

import javax.annotation.PostConstruct;

import com.alibaba.nacossync.constant.MetricsStatisticsType;
import com.alibaba.nacossync.extension.ha.ConsistentHashSyncShardingEtcdProxy;
import com.alibaba.nacossync.extension.jetcd.EtcdProxy;
import com.alibaba.nacossync.monitor.MetricsManager;
import com.alibaba.nacossync.util.IPUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.event.DeleteTaskEvent;
import com.alibaba.nacossync.event.SyncTaskEvent;
import com.alibaba.nacossync.extension.SyncManagerService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.util.Optional;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.PER_WORKER_PROCESS_SERVICE;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SLASH;

/**
 * @author NacosSync
 * @version $Id: EventListener.java, v 0.1 2018-09-27 AM1:21 NacosSync Exp $$
 */
@Slf4j
@Service
public class EventListener {

    @Autowired
    private MetricsManager metricsManager;

    @Autowired
    private SyncManagerService syncManagerService;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private SkyWalkerCacheServices skyWalkerCacheServices;

    @Autowired
    private EtcdProxy proxy;

    @PostConstruct
    public void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void listenerSyncTaskEvent(SyncTaskEvent syncTaskEvent) {

        try {
            long start = System.currentTimeMillis();
            String serviceName = Optional.ofNullable(syncTaskEvent).map(sy -> sy.getTaskDO()).map(t -> t.getServiceName()).orElse(StringUtils.EMPTY);
            syncManagerService.sync(syncTaskEvent.getTaskDO());
            proxy.putEtcdValueByKey(PER_WORKER_PROCESS_SERVICE.concat(SLASH).concat(IPUtils.getIpAddress()).concat(SLASH).concat(serviceName), serviceName);
            skyWalkerCacheServices.addFinishedTask(syncTaskEvent.getTaskDO());
            metricsManager.record(MetricsStatisticsType.SYNC_TASK_RT, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("listenerSyncTaskEvent process error", e);
        }

    }

    @Subscribe
    public void listenerDeleteTaskEvent(DeleteTaskEvent deleteTaskEvent) {

        try {
            long start = System.currentTimeMillis();
            log.info("listenerDeleteTaskEvent" + deleteTaskEvent.getTaskDO().toString());
            String serviceName = Optional.ofNullable(deleteTaskEvent).map(sy -> sy.getTaskDO()).map(t -> t.getServiceName()).orElse(StringUtils.EMPTY);
            syncManagerService.delete(deleteTaskEvent.getTaskDO());
            proxy.deleteEtcdValueByKey(PER_WORKER_PROCESS_SERVICE.concat(SLASH).concat(IPUtils.getIpAddress()).concat(SLASH).concat(serviceName), false);
            skyWalkerCacheServices.addFinishedTask(deleteTaskEvent.getTaskDO());
            metricsManager.record(MetricsStatisticsType.DELETE_TASK_RT, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("listenerDeleteTaskEvent process error", e);
        }

    }

}
