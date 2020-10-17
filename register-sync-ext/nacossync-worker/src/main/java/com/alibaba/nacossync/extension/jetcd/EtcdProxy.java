package com.alibaba.nacossync.extension.jetcd;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.ETCD_BEAT_TTL;
import static com.google.common.base.Charsets.UTF_8;


import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;

import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;

import io.etcd.jetcd.watch.WatchResponse;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * etcd v3
 */
@Component
@Slf4j
public class EtcdProxy {

    private final Client client;


    private final ScheduledExecutorService scheduledExecutorService;

    private final Environment environment;

    public EtcdProxy(Client client, @Qualifier("etcdListenerExecutor") ScheduledExecutorService scheduledExecutorService, Environment environment) {
        this.client = client;
        this.scheduledExecutorService = scheduledExecutorService;
        this.environment = environment;
    }

    /**
     * 新增或者修改指定的配置
     *
     * @param key
     * @param value
     * @throws Exception
     */
    public void putEtcdValueByKey(String key, String value) throws Exception {
        client.getKVClient().put(ByteSequence.from(key, UTF_8), ByteSequence.from(value, UTF_8)).get();
    }

    /**
     * 校验key是否存在
     *
     * @param key
     * @throws Exception
     */
    public boolean checkEtcdKeyExist(String key) throws Exception {
        List<KeyValue> keyValues = getEtcdValueByKey(key, false);
        if (CollectionUtils.isEmpty(keyValues)) {
            return false;
        }
        return true;
    }

    /**
     * 新增或者修改指定的配置
     *
     * @param key
     * @param value
     * @throws Exception
     */
    public void registerEtcdValueByKey(String key, String value) throws Exception {
        String ttls = environment.getProperty(ETCD_BEAT_TTL);
        long ttl = NumberUtils.toLong(ttls);
        long leaseId = client.getLeaseClient().grant(ttl).get().getID();
        PutOption option = PutOption.newBuilder().withLeaseId(leaseId).withPrevKV().build();
        client.getKVClient().put(ByteSequence.from(key, UTF_8), ByteSequence.from(value, UTF_8), option).get();
        long delay = ttl / 6;
        scheduledExecutorService.schedule(new BeatTask(leaseId, delay), delay, TimeUnit.SECONDS);
    }


    /**
     * 查询指定的key名称对应的value
     *
     * @param key
     * @return value值
     */
    public List<KeyValue> getEtcdValueByKey(String key, boolean withPrefix) {
        GetResponse getResponse = null;
        GetOption option = GetOption.newBuilder().build();
        if (withPrefix) {
            option = GetOption.newBuilder().withPrefix(ByteSequence.from(key, UTF_8)).build();
        }
        try {
            getResponse = client.getKVClient()
                    .get(ByteSequence.from(key, UTF_8), option).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        // key does not exist
        if (getResponse.getKvs().isEmpty()) {
            return null;
        }
        List<KeyValue> keyValueList = getResponse.getKvs();
        return keyValueList;
    }

    /**
     * 删除指定的配置
     *
     * @param key
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void deleteEtcdValueByKey(String key, boolean withPrefix) throws InterruptedException, ExecutionException {
        DeleteOption option = DeleteOption.newBuilder().build();
        if (withPrefix) {
            option = DeleteOption.newBuilder().withPrefix(ByteSequence.from(key, UTF_8)).build();
        }
        DeleteResponse response = client.getKVClient().delete(ByteSequence.from(key, UTF_8), option).get();
        System.out.println(response);
    }

    /**
     * etcd的watch阻塞模式
     *
     * <p>
     * 异步执行
     *
     * @param key
     * @throws Exception
     */
    public void watchEtcdKeyAsync(String key, Boolean usePrefix, Consumer<WatchResponse> onNext) {
        scheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    watchEtcdKey(key, usePrefix, onNext);
                } catch (Exception e) {
                    log.error("watchEtcdKeyAyc error,{}", e);
                }
            }
        });

    }


    public void watchEtcdKey(String key, Boolean usePrefix, Consumer<WatchResponse> onNext) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Watcher watcher = null;
        try {
            ByteSequence watchKey = ByteSequence.from(key, UTF_8);
            WatchOption watchOpts = WatchOption.newBuilder().build();
            if (usePrefix) {
                watchOpts = WatchOption.newBuilder().withPrefix(watchKey).build();
            }
            watcher = client.getWatchClient().watch(watchKey, watchOpts, onNext);
            latch.await();
        } catch (Exception e) {
            if (watcher != null) {
                watcher.close();
            }
            throw e;
        }
    }

    /**
     * 续约
     */
    private class BeatTask implements Runnable {
        long leaseId;
        long delay;

        public BeatTask(long leaseId, long delay) {
            this.leaseId = leaseId;
            this.delay = delay;
        }

        public void run() {
            log.info("BeatTask lease :{}", leaseId);
            client.getLeaseClient().keepAliveOnce(leaseId);
            scheduledExecutorService.schedule(new BeatTask(this.leaseId, this.delay), delay, TimeUnit.SECONDS);

        }
    }
}