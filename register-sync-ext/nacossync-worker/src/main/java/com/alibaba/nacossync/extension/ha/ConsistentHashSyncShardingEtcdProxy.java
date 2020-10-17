package com.alibaba.nacossync.extension.ha;


import com.alibaba.nacossync.extension.jetcd.EtcdProxy;
import com.alibaba.nacossync.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ConsistentHashSyncShardingEtcdProxy extends AbstractSyncShardingEtcdProxy {


    @Value("${sync.consistent.hash.replicas}")
    private int numberOfReplicas;

    public ConsistentHashSyncShardingEtcdProxy(EtcdProxy manager, Environment environment) {
        super(manager, environment);
    }

    public String shardingNode(String serviceName) {
        ConsistentHash<String> consistentHash = consistentHashWrapper();
        return consistentHash.getNode(serviceName);
    }

    public String headNode() {
        ConsistentHash<String> consistentHash = consistentHashWrapper();
        return consistentHash.getHeadNode();
    }

    public boolean isProcessNode(String serviceName) {
        try {
            return IPUtils.getIpAddress().equalsIgnoreCase(shardingNode(serviceName));
        } catch (Exception e) {
            log.error("Judge process node failed {}", e);
        }
        return false;
    }

    public boolean isLeaderNode() {
        try {
            return IPUtils.getIpAddress().equalsIgnoreCase(headNode());
        } catch (Exception e) {
            log.error("Judge leader node failed {}", e);
        }
        return false;
    }

    private ConsistentHash<String> consistentHashWrapper() {
        List<String> ips = getWorkerIps();
        ConsistentHash<String> consistentHash = new ConsistentHash(numberOfReplicas, ips);
        if (CollectionUtils.isNotEmpty(nodeCaches)) {
            consistentHash = new ConsistentHash(numberOfReplicas, nodeCaches);
        }
        return consistentHash;
    }
}
