package com.alibaba.nacossync.extension.ha;

import com.alibaba.nacossync.extension.curator.CuratorProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

//@Component
@Slf4j
public class DynamicHashSyncShardingProxy extends AbstractSyncShardingZkProxy {

    public DynamicHashSyncShardingProxy(CuratorProxy manager, Environment environment) {
        super(manager,environment);
    }

    @Override
    public String shardingNode(String serviceName) {
        return super.shardingNode(serviceName);
    }

    @Override
    public boolean isProcessNode(String serviceName) {
        return super.isProcessNode(serviceName);
    }

    @Override
    public boolean isLeaderNode() {
        return super.isLeaderNode();
    }

    @Override
    public String leaderNode() {
        return super.leaderNode();
    }
}
