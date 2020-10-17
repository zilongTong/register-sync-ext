package com.alibaba.nacossync.timer;

import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.extension.ha.ConsistentHashSyncShardingEtcdProxy;
import com.alibaba.nacossync.extension.ha.ConsistentHashSyncShardingZkProxy;
import com.alibaba.nacossync.service.factory.SyncCluster;
import com.alibaba.nacossync.service.factory.SyncFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//@Component
@Slf4j
public class MergeClusterDataTimer implements CommandLineRunner {

    @Qualifier("executorService")
    @Autowired
    private ScheduledExecutorService scheduledExecutorService;
    @Autowired
    private ConsistentHashSyncShardingEtcdProxy syncShardingProxy;
    @Autowired
    private SyncFactory syncFactory;

    @Override
    public void run(String... args) throws Exception {
        /** Fetch the task list from the register every 5 seconds */
        scheduledExecutorService.scheduleWithFixedDelay(new SyncClusterData(), 5000, 5000,
                TimeUnit.MILLISECONDS);
    }

    private class SyncClusterData implements Runnable {
        @Override
        public void run() {
            /**
             * process leader node only
             */
            if (syncShardingProxy.isLeaderNode()) {
                SyncCluster eureka = syncFactory.getSync(ClusterTypeEnum.EUREKA);
                eureka.syncClusterData();
                SyncCluster nacos = syncFactory.getSync(ClusterTypeEnum.NACOS);
                nacos.syncClusterData();
            }
        }
    }
}

