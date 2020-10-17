package com.alibaba.nacossync.extension.ha;

import com.alibaba.nacossync.extension.curator.CuratorProxy;
import com.alibaba.nacossync.util.IPUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.CommandLineRunner;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.REGISTER_WORKER_PATH;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.REGISTER_SWITCH;

//@Service
@Slf4j
public class PartitionRegister implements CommandLineRunner {

    private final CuratorProxy manager;

    public PartitionRegister(CuratorProxy manager) {
        this.manager = manager;
    }


    @Override
    public void run(String... args) throws Exception {
        log.info("PartitionRegister ...........");
        if (!manager.checkExists(REGISTER_WORKER_PATH))
            manager.createPersistentNode(REGISTER_WORKER_PATH, Strings.EMPTY);
        if (!manager.checkExists(REGISTER_SWITCH))
            manager.createPersistentNode(REGISTER_SWITCH, Boolean.FALSE.toString());
        String addressPath = REGISTER_WORKER_PATH + "/" + IPUtils.getIpAddress();
        log.info("register local ip ,{}", IPUtils.getIpAddress());
        if (!manager.checkExists(addressPath)) {
            manager.createEphemeralNode(addressPath, Strings.EMPTY);
        }

    }
}
