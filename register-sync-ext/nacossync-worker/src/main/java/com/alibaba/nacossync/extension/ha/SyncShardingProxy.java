package com.alibaba.nacossync.extension.ha;


import java.util.List;

public interface SyncShardingProxy {

    String shardingNode(String serviceName);

    List<String> getWorkerIps();

    String switchState() throws Exception;


    boolean setSwitchState(String state);


    boolean isProcessNode(String serviceName);

    boolean isLeaderNode();

    String leaderNode();

}
