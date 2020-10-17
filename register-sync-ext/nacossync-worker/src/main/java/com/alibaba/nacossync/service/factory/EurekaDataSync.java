package com.alibaba.nacossync.service.factory;


import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.ClusterAccessService;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.extension.eureka.EurekaNamingService;
import com.alibaba.nacossync.extension.holder.EurekaServerHolder;
import com.alibaba.nacossync.pojo.model.ClusterDO;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.util.SkyWalkerUtil;
import com.netflix.discovery.shared.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EurekaDataSync implements SyncCluster {

    @Autowired
    private TaskAccessService taskAccessService;

    @Autowired
    private ClusterAccessService clusterAccessService;

    @Autowired
    private EurekaServerHolder eurekaServerHolder;

    @Override
    public void syncClusterData() {

        List<TaskDO> taskDOS = taskAccessService.findAll();
        ClusterDO nacosClusterDO = clusterAccessService.findByClusterType(ClusterTypeEnum.NACOS.name());
        ClusterDO eurekaClusterDO = clusterAccessService.findByClusterType(ClusterTypeEnum.EUREKA.name());
        if (nacosClusterDO == null) {
            return;
        }
        EurekaNamingService eurekaNamingService = eurekaServerHolder.get(eurekaClusterDO.getClusterId(), null);
        List<Application> applications = eurekaNamingService.getAllApplications();
        applications.stream().forEach(a -> {
            String taskId = SkyWalkerUtil.generateTaskId(a.getName().toLowerCase(),
                    "default", eurekaClusterDO.getClusterId(), nacosClusterDO.getClusterId());
            TaskDO taskDO = new TaskDO();
            taskDO.setTaskId(taskId);
            taskDO.setDestClusterId(nacosClusterDO.getClusterId());
            taskDO.setSourceClusterId(eurekaClusterDO.getClusterId());
            taskDO.setServiceName(a.getName().toLowerCase());
            taskDO.setVersion("");
            taskDO.setGroupName("default");
            taskDO.setNameSpace("");
            taskDO.setTaskStatus(TaskStatusEnum.SYNC.getCode());
            try {
                taskDO.setWorkerIp(SkyWalkerUtil.getLocalIp());
            } catch (Exception e) {
                e.printStackTrace();
            }
            taskDO.setOperationId(SkyWalkerUtil.generateOperationId());
            if (!taskDOS.contains(taskDO)) {
                taskAccessService.addTask(taskDO);
            }
        });
    }
}
