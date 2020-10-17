package com.alibaba.nacossync.extension.ha;

import com.alibaba.nacossync.extension.curator.CuratorProxy;
import com.alibaba.nacossync.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

//@Component
@Slf4j
public class ConsistentHashSyncShardingZkProxy extends AbstractSyncShardingZkProxy {

    @Value("${sync.consistent.hash.replicas}")
    private int numberOfReplicas;

    private static ConsistentHash<String> consistentHash;

    public ConsistentHashSyncShardingZkProxy(CuratorProxy manager, Environment environment) {
        super(manager, environment);
    }

    public String shardingNode(String serviceName) {
        consistentHashWrapper();
        return consistentHash.getNode(serviceName);
    }

    public String headNode() {
        consistentHashWrapper();
        return consistentHash.getHeadNode();
    }

    public boolean isProcessNode(String serviceName) {
        try {
            return IPUtils.getIpAddress().equalsIgnoreCase(shardingNode(serviceName));
        } catch (Exception e) {
            log.error("Judge process node failed,{}", e);
        }
        return false;
    }

    public boolean isLeaderNode() {
        try {
            return IPUtils.getIpAddress().equalsIgnoreCase(headNode());
        } catch (Exception e) {
            log.error("Judge leader node failed,{}", e);
        }
        return false;
    }

    private void consistentHashWrapper() {
        if (CollectionUtils.isEmpty(nodeCaches)) {
            List<String> ips = getWorkerIps();
            consistentHash = new ConsistentHash(numberOfReplicas, ips);
        } else {
            consistentHash = new ConsistentHash(numberOfReplicas, nodeCaches);
        }
    }
}
