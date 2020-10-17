package com.alibaba.nacossync.extension.jetcd;

import io.etcd.jetcd.Client;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class EtcdConfig {

    @Bean(value = "etcdClient", destroyMethod = "close")
    public Client getClient(@Value(value = "${sync.etcd.register.address}") String etcdAddress) {
        String[] adds = etcdAddress.split(",");
        return Client.builder().endpoints(adds).build();
    }

    @Bean(value = "etcdListenerExecutor")
    public ScheduledExecutorService etcdListenerExecutor() {
        return new ScheduledThreadPoolExecutor(5, new BasicThreadFactory.Builder()
                .namingPattern("etcd-listener-schedule-pool-%d").daemon(true).build());
    }
}
