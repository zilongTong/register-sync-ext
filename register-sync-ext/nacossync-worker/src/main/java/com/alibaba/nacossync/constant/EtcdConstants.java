package com.alibaba.nacossync.constant;

/**
 * Created by liaomengge on 2020/6/15.
 */
public class EtcdConstants {

    public static final char DELIMITER = '/';

    public static final String ETCD_TASK_ROOT_NODE = "/solar-nacos-task";
    public static final String ETCD_CLUSTER_ROOT_NODE = "/solar-nacos-cluster";

    public static final String ETCD_TASK_CAFFEINE = "cache:sloar-nacos-task";
    public static final String ETCD_CLUSTER_CAFFEIN = "cache:sloar-nacos-cluster";
}
