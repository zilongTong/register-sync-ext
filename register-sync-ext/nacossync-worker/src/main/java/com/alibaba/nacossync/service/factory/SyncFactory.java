package com.alibaba.nacossync.service.factory;

import com.alibaba.nacossync.constant.ClusterTypeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class SyncFactory {

    @Resource(name = "nacosDataSync")
    private SyncCluster nacosDataSync;

    @Resource(name = "eurekaDataSync")
    private SyncCluster eurekaDataSync;

    public SyncCluster getSync(ClusterTypeEnum clusterTypeEnum) {
        if (clusterTypeEnum.getCode().equalsIgnoreCase(ClusterTypeEnum.NACOS.getCode())) {
            return nacosDataSync;
        }
        if (clusterTypeEnum.getCode().equalsIgnoreCase(ClusterTypeEnum.EUREKA.getCode())) {
            return eurekaDataSync;
        }
        return null;
    }


}
