package com.alibaba.nacossync.extension.watcher;

import com.alibaba.nacossync.constant.EtcdConstants;
import com.alibaba.nacossync.util.ByteSequenceUtil;
import com.alibaba.nacossync.util.CaffeineUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by liaomengge on 2020/6/16.
 */
@Slf4j
@Service
public class EtcdWatchService implements CommandLineRunner {

    @Autowired
    private Client client;

    @Override
    public void run(String... args) throws Exception {
        log.info("start etcd cluster/task listener...");

        try {
            watchClusterKey();
        } catch (Exception e) {
            CaffeineUtil.invalidate(EtcdConstants.ETCD_CLUSTER_CAFFEIN);
            log.error("watch cluster key failed", e);
        }

        try {
            watchTaskKey();
        } catch (Exception e) {
            CaffeineUtil.invalidate(EtcdConstants.ETCD_TASK_CAFFEINE);
            log.error("watch task key failed", e);
        }
    }

    private void watchClusterKey() {
        ByteSequence clusterKey = ByteSequenceUtil.fromString(EtcdConstants.ETCD_CLUSTER_ROOT_NODE);
        client.getWatchClient().watch(clusterKey, WatchOption.newBuilder().withPrefix(clusterKey).build(), resp -> {
            List<WatchEvent> watchEventList = resp.getEvents();
            watchEventList.stream().filter(val ->
                    WatchEvent.EventType.PUT == val.getEventType() || WatchEvent.EventType.DELETE == val.getEventType())
                    .findFirst().ifPresent(val -> {
                log.info("the first change event[{}], key[{}]", val.getEventType().name(),
                        val.getKeyValue().getKey().toString(Charset.forName("UTF-8")));
                CaffeineUtil.invalidate(EtcdConstants.ETCD_CLUSTER_CAFFEIN);
            });
        });
    }

    private void watchTaskKey() {
        ByteSequence clusterKey = ByteSequenceUtil.fromString(EtcdConstants.ETCD_TASK_ROOT_NODE);
        client.getWatchClient().watch(clusterKey, WatchOption.newBuilder().withPrefix(clusterKey).build(), resp -> {
            List<WatchEvent> watchEventList = resp.getEvents();
            watchEventList.stream().filter(val ->
                    WatchEvent.EventType.PUT == val.getEventType() || WatchEvent.EventType.DELETE == val.getEventType())
                    .findFirst().ifPresent(val -> {
                log.info("the first change event[{}], key[{}]", val.getEventType().name(),
                        val.getKeyValue().getKey().toString(Charset.forName("UTF-8")));
                CaffeineUtil.invalidate(EtcdConstants.ETCD_TASK_CAFFEINE);
            });
        });
    }
}
