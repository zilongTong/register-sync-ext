package com.alibaba.nacossync.extension.curator;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class CuratorConfig {


    @Value(value = "${sync.zk.register.address}")
    private String zkAddress;

    @Bean()
    public CuratorClient client() {
        CuratorClient client = new CuratorClient(zkAddress);
        return client;
    }


    @Bean()
    public CuratorProxy manager() {
        CuratorProxy manager = new CuratorProxy(client());
        return manager;
    }
}
