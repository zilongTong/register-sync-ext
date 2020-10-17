package com.alibaba.nacossync.extension.ha;

import com.alibaba.nacossync.constant.RegisterTypeEnum;
import com.alibaba.nacossync.extension.jetcd.EtcdProxy;
import com.alibaba.nacossync.util.IPUtils;
import com.google.common.base.Charsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.watch.WatchEvent;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.PER_WORKER_PROCESS_SERVICE;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.REGISTER_WORKER_PATH;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.REGISTER_SWITCH;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SLASH;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SYNC_REGISTER_TYPE;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SYNC_WORKER_ADDRESS;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.UTF_8;

@Component
@Order(1)
@Slf4j
public abstract class AbstractSyncShardingEtcdProxy implements SyncShardingProxy, InitializingBean {

    protected static Set<String> nodeCaches = new TreeSet<>();

    private final Environment environment;

    private final EtcdProxy manager;

    public AbstractSyncShardingEtcdProxy(EtcdProxy manager, Environment environment) {
        this.manager = manager;
        this.environment = environment;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            String type = environment.getProperty(SYNC_REGISTER_TYPE);
            if (!type.equalsIgnoreCase(RegisterTypeEnum.ETCD.name())) {
                return;
            }
            registerWorker();
            nodeCacheWatcher();
        } catch (Exception e) {
            log.error("Initializing sync sharding proxy failed, load workers from props");
        }
    }

    private void nodeCacheWatcher() {
        initData();
        manager.watchEtcdKeyAsync(REGISTER_WORKER_PATH, true, response -> {
            for (WatchEvent event : response.getEvents()) {
                String value = Optional.ofNullable(event.getKeyValue().getValue())
                        .map(bs -> bs.toString(Charsets.UTF_8)).orElse(StringUtils.EMPTY);
                String key = Optional.ofNullable(event.getKeyValue().getKey())
                        .map(bs -> bs.toString(Charsets.UTF_8)).orElse(StringUtils.EMPTY);
                String[] ks = key.split(SLASH);
                if (event.getEventType().equals(WatchEvent.EventType.PUT)) {
                    nodeCaches.add(value);
                }
                if (event.getEventType().equals(WatchEvent.EventType.DELETE)) {
                    log.info("{}  lost heart beat ", ks[3]);
                    if (!IPUtils.getIpAddress().equalsIgnoreCase(ks[3])) {
                        nodeCaches.remove(ks[3]);
                        //心跳丢失，清除etcd上该节点的处理任务
                        try {
                            manager.deleteEtcdValueByKey(PER_WORKER_PROCESS_SERVICE.concat(SLASH).concat(ks[3]), true);
                        } catch (InterruptedException e) {
                            log.error("clear {} process service failed,{}", ks[3], e);
                        } catch (ExecutionException e) {
                            log.error("clear {} process service failed,{}", ks[3], e);
                        }
                    }
                }
                log.info("watch type= \"" + event.getEventType().toString() + "\",  key= \""
                        + Optional.ofNullable(event.getKeyValue().getKey()).map(bs -> bs.toString(Charsets.UTF_8)).orElse("")
                        + "\",  value= \"" + Optional.ofNullable(event.getKeyValue().getValue())
                        .map(bs -> bs.toString(Charsets.UTF_8)).orElse("")
                        + "\"");
            }
        });
    }

    public void initData() {
        clearData();
        List<KeyValue> keyValues = manager.getEtcdValueByKey(REGISTER_WORKER_PATH, true);
        if (!CollectionUtils.isEmpty(keyValues)) {
            List<ByteSequence> strings = keyValues.stream().map(KeyValue::getValue).collect(Collectors.toList());
            strings.stream().forEach(s -> {
                nodeCaches.add(s.toString(UTF_8));
            });
        }
    }

    private void registerWorker() throws Exception {
        if (manager.checkEtcdKeyExist(PER_WORKER_PROCESS_SERVICE.concat(SLASH).concat(IPUtils.getIpAddress())))
            manager.deleteEtcdValueByKey(PER_WORKER_PROCESS_SERVICE.concat(SLASH).concat(IPUtils.getIpAddress()), true);
        if (!manager.checkEtcdKeyExist(REGISTER_SWITCH))
            manager.putEtcdValueByKey(REGISTER_SWITCH, Boolean.FALSE.toString());
        String addressPath = REGISTER_WORKER_PATH.concat(SLASH).concat(IPUtils.getIpAddress());
        log.info("register local ip ,{}", IPUtils.getIpAddress());
        manager.registerEtcdValueByKey(addressPath, IPUtils.getIpAddress());
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
    public String switchState() throws Exception {
        List<KeyValue> keyValues = manager.getEtcdValueByKey(REGISTER_SWITCH, false);
        return keyValues.get(0).getValue().toString(UTF_8);
    }

    @Override
    public boolean setSwitchState(String state) {
        try {
            manager.putEtcdValueByKey(REGISTER_SWITCH, state);
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
