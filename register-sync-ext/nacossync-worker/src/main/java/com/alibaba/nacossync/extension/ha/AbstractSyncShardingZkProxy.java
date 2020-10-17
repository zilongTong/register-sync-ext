package com.alibaba.nacossync.extension.ha;

import com.alibaba.nacossync.constant.RegisterTypeEnum;
import com.alibaba.nacossync.extension.curator.CuratorProxy;
import com.alibaba.nacossync.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.REGISTER_WORKER_PATH;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.REGISTER_SWITCH;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SYNC_REGISTER_TYPE;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SYNC_WORKER_ADDRESS;

@Component
@Order(1)
@Slf4j
public abstract class AbstractSyncShardingZkProxy implements SyncShardingProxy, InitializingBean {

    protected static Set<String> nodeCaches = new TreeSet<>();


    private final Environment environment;

    private final CuratorProxy manager;

    public AbstractSyncShardingZkProxy(CuratorProxy manager, Environment environment) {
        this.manager = manager;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            String type = environment.getProperty(SYNC_REGISTER_TYPE);
            if (!type.equalsIgnoreCase(RegisterTypeEnum.ZOOKEEPER.name())) {
                return;
            }
            registerWorker();
            nodeCacheWatcher();
        } catch (Exception e) {
            log.error("Initializing sync sharding proxy failed, load workers from props");
        }
    }

    public void nodeCacheWatcher() {
        this.initData();
        PathChildrenCache pathChildrenCache = manager.registerPathChildListener(REGISTER_WORKER_PATH, (client, event) -> {
            ChildData childData = event.getData();
            if (childData == null) {
                return;
            }
            String path = childData.getPath();
            if (StringUtils.isEmpty(path)) {
                return;
            }
            String[] paths = path.split("/");
            if (paths == null || path.length() < 4) {
                return;
            }
            String ip = paths[3];
            ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
            switch (event.getType()) {
                case CHILD_ADDED:
                    log.info("正在新增子节点：" + childData.getPath());
                    nodeCaches.add(ip);
                    break;
                case CHILD_UPDATED:
                    log.info("正在更新子节点：" + childData.getPath());
                    nodeCaches.add(ip);
                    break;
                case CHILD_REMOVED:
                    log.info("子节点被删除");
                    nodeCaches.remove(ip);
                    break;
                case CONNECTION_LOST:
                    log.info("连接丢失");
                    clearData();
                    break;
                case CONNECTION_SUSPENDED:
                    log.info("连接被挂起");
                    break;
                case CONNECTION_RECONNECTED:
                    log.info("恢复连接");
                    initData();
                    break;
            }
        });
    }

    public void initData() {
        if (CollectionUtils.isNotEmpty(nodeCaches)) {
            nodeCaches.clear();
        }
        List<String> strings = manager.getChildren(REGISTER_WORKER_PATH);
        if (!CollectionUtils.isEmpty(strings)) {
            nodeCaches.addAll(strings);
        }
    }

    private void registerWorker() {
        if (!manager.checkExists(REGISTER_WORKER_PATH))
            manager.createPersistentNode(REGISTER_WORKER_PATH, Strings.EMPTY);
        if (!manager.checkExists(REGISTER_SWITCH))
            manager.createPersistentNode(REGISTER_SWITCH, Boolean.FALSE.toString());
        String addressPath = null;
        try {
            addressPath = REGISTER_WORKER_PATH + "/" + IPUtils.getIpAddress();
            log.info("register local ip ,{}", IPUtils.getIpAddress());
        } catch (Exception e) {
            log.error("register local ip failed");
        }
        if (!manager.checkExists(addressPath)) {
            manager.createEphemeralNode(addressPath, Strings.EMPTY);
        }
    }

    public void clearData() {
        if (CollectionUtils.isNotEmpty(nodeCaches)) {
            nodeCaches.clear();
        }
    }

    @Override
    public List<String> getWorkerIps() {
        String workerIps = environment.getProperty(SYNC_WORKER_ADDRESS);
        String[] strings = workerIps.split(",");
        return Arrays.asList(strings);
    }

    public Set<String> getNodeCaches() {
        return nodeCaches;
    }

    @Override
    public String switchState() {
        return manager.getData(REGISTER_SWITCH);
    }

    @Override
    public boolean setSwitchState(String state) {
        try {
            manager.setData(REGISTER_SWITCH, state);
            return true;
        } catch (Exception e) {
            log.error("set switchState failed :{},error:{}", state, e);
        }
        return false;
    }

    @Override
    public boolean isProcessNode(String serviceName) {
        return false;
    }

    @Override
    public boolean isLeaderNode() {
        return false;
    }

    @Override
    public String shardingNode(String serviceName) {
        return null;
    }

    @Override
    public String leaderNode() {
        return null;
    }
}
