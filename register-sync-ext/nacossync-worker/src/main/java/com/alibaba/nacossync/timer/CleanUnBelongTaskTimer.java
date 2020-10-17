package com.alibaba.nacossync.timer;

import com.alibaba.nacossync.cache.SkyWalkerCacheServices;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.ClusterAccessService;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.extension.SyncManagerService;
import com.alibaba.nacossync.extension.event.SpecialSyncEventBus;
import com.alibaba.nacossync.extension.ha.ConsistentHashSyncShardingEtcdProxy;
import com.alibaba.nacossync.extension.impl.EurekaSyncToNacosServiceImpl;
import com.alibaba.nacossync.extension.impl.NacosSyncToEurekaServiceImpl;
import com.alibaba.nacossync.extension.jetcd.EtcdProxy;
import com.alibaba.nacossync.pojo.FinishedTask;
import com.alibaba.nacossync.pojo.model.ClusterDO;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.util.IPUtils;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.PER_WORKER_PROCESS_SERVICE;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SLASH;

@Component
@Slf4j
public class CleanUnBelongTaskTimer implements CommandLineRunner {

    @Qualifier("executorService")
    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    private TaskAccessService taskAccessService;

    @Autowired
    private SkyWalkerCacheServices skyWalkerCacheServices;

    @Autowired
    private ConsistentHashSyncShardingEtcdProxy shardingEtcdProxy;

    @Autowired
    private ClusterAccessService clusterAccessService;

    @Autowired
    private EtcdProxy proxy;

    @Autowired
    private SpecialSyncEventBus specialSyncEventBus;

    @Autowired
    private SyncManagerService syncManagerService;

    @Autowired
    private ConsistentHashSyncShardingEtcdProxy syncShardingProxy;

    @Override
    public void run(String... args) throws Exception {
        scheduledExecutorService.scheduleWithFixedDelay(new CleanUnBelongTask(), 5000, 5000,
                TimeUnit.MILLISECONDS);
    }

    private class CleanUnBelongTask implements Runnable {
        @Override
        public void run() {
            try {
                log.info("register cluster state,{}", syncShardingProxy.switchState());
                if (syncShardingProxy.switchState() == null || syncShardingProxy.switchState().equalsIgnoreCase(Boolean.FALSE.toString())) {
                    log.info("register state is Not activated , please active the register first");
                    return;
                }
                Map<String, FinishedTask> finishedTaskMap = skyWalkerCacheServices.getFinishedTaskMap();
                Map<String, FinishedTask> tmpTaskMap = Maps.newHashMap();
                for (Map.Entry<String, FinishedTask> entry : finishedTaskMap.entrySet()) {
                    String operationId = entry.getKey();
                    TaskDO taskDO = taskAccessService.findByOperationId(operationId);
                    if (Objects.isNull(taskDO)) {
                        continue;
                    }
                    if (!shardingEtcdProxy.isProcessNode(taskDO.getServiceName()) && TaskStatusEnum.SYNC.getCode().equals(taskDO.getTaskStatus())) {
                        tmpTaskMap.put(operationId, entry.getValue());
                    }
                }
                log.info("need remove taskId count ===> [{}]", tmpTaskMap.size());

                tmpTaskMap.entrySet().stream().forEach(val -> {
                    String opId = val.getValue().getOperationId();
                    TaskDO taskDO = taskAccessService.findByOperationId(opId);
                    log.info("Clean UnBelong  task {}", taskDO.toString());
                    try {
                        //1.删除多余的节点同步
                        specialSyncEventBus.unsubscribe(taskDO);

                        //2.删除多余的节点处理任务数
                        proxy.deleteEtcdValueByKey(PER_WORKER_PROCESS_SERVICE.concat(SLASH).concat(IPUtils.getIpAddress()).concat(SLASH).concat(taskDO.getServiceName()), false);

                        //3.删除多余的节点心跳(替代发布delete event)
                        ClusterDO clusterDO = clusterAccessService.findByClusterId(taskDO.getSourceClusterId());
                        if (Objects.nonNull(clusterDO)) {
                            if (ClusterTypeEnum.EUREKA.getCode().equalsIgnoreCase(clusterDO.getClusterType())) {
                                EurekaSyncToNacosServiceImpl syncToNacosService =
                                        (EurekaSyncToNacosServiceImpl) syncManagerService.getSyncService(taskDO.getSourceClusterId(), taskDO.getDestClusterId());
                                syncToNacosService.deleteBeat(taskDO);
                            } else if (ClusterTypeEnum.NACOS.getCode().equalsIgnoreCase(clusterDO.getClusterType())) {
                                NacosSyncToEurekaServiceImpl syncToEurekaService =
                                        (NacosSyncToEurekaServiceImpl) syncManagerService.getSyncService(taskDO.getSourceClusterId(), taskDO.getDestClusterId());
                                syncToEurekaService.deleteBeat(taskDO);
                            }
                        }

                        //4.删除多余的finish任务
                        finishedTaskMap.remove(val.getKey());
                    } catch (Exception e) {
                        log.error("the node[{}], remove unnecessary task[{}] fail", IPUtils.getIpAddress(),
                                taskDO.getTaskId(), e);
                    }
                });
            } catch (Exception e) {
                log.warn("CleanUnBelongTaskTimer Exception", e);
            }
        }
    }
}
