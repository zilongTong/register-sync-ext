/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacossync.api;

import com.alibaba.nacossync.extension.ha.ConsistentHashSyncShardingEtcdProxy;
import com.alibaba.nacossync.extension.ha.ConsistentHashSyncShardingZkProxy;
import com.alibaba.nacossync.extension.jetcd.EtcdProxy;
import com.alibaba.nacossync.util.IPUtils;
import com.alibaba.nacossync.util.Result;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.nacossync.pojo.result.ConfigAddResult;
import com.alibaba.nacossync.pojo.result.ConfigDeleteResult;
import com.alibaba.nacossync.pojo.result.ConfigQueryResult;
import com.alibaba.nacossync.pojo.request.ConfigAddRequest;
import com.alibaba.nacossync.pojo.request.ConfigDeleteRequest;
import com.alibaba.nacossync.pojo.request.ConfigQueryRequest;
import com.alibaba.nacossync.template.SkyWalkerTemplate;
import com.alibaba.nacossync.template.processor.ConfigAddProcessor;
import com.alibaba.nacossync.template.processor.ConfigDeleteProcessor;
import com.alibaba.nacossync.template.processor.ConfigQueryProcessor;

import java.util.Set;

import static com.alibaba.nacossync.constant.SkyWalkerConstants.PER_WORKER_PROCESS_SERVICE;
import static com.alibaba.nacossync.constant.SkyWalkerConstants.SLASH;
import static com.alibaba.nacossync.util.Result.ERROR;
import static com.alibaba.nacossync.util.Result.SUCCESS;

/**
 * @author NacosSync
 * @version $Id: SystemConfigApi.java, v 0.1 2018-09-26 AM2:06 NacosSync Exp $$
 */
@Slf4j
@RestController
public class SystemConfigApi {

    @Autowired
    private ConfigQueryProcessor configQueryProcessor;

    @Autowired
    private ConfigDeleteProcessor configDeleteProcessor;

    @Autowired
    private ConfigAddProcessor configAddProcessor;

    @Autowired
    private ConsistentHashSyncShardingEtcdProxy proxy;

    @Autowired
    private EtcdProxy etcdProxy;

    @Value(value = "${sync.etcd.init.token}")
    private String token;

    @RequestMapping(path = "/v1/systemconfig/list", method = RequestMethod.GET)

    public ConfigQueryResult tasks(ConfigQueryRequest configQueryRequest) {

        return SkyWalkerTemplate.run(configQueryProcessor, configQueryRequest,
                new ConfigQueryResult());
    }

    @RequestMapping(path = "/v1/systemconfig/delete", method = RequestMethod.DELETE)
    public ConfigDeleteResult deleteTask(@RequestBody ConfigDeleteRequest configDeleteRequest) {

        return SkyWalkerTemplate.run(configDeleteProcessor, configDeleteRequest,
                new ConfigDeleteResult());
    }

    @RequestMapping(path = "/v1/systemconfig/add", method = RequestMethod.POST)
    public ConfigAddResult taskAdd(@RequestBody ConfigAddRequest configAddRequest) {

        return SkyWalkerTemplate.run(configAddProcessor, configAddRequest, new ConfigAddResult());
    }

    @RequestMapping(path = "/v1/systemconfig/leaderNode", method = RequestMethod.GET)
    public String leaderNode() {
        return proxy.headNode();
    }

    @RequestMapping(path = "/v1/systemconfig/listNode", method = RequestMethod.GET)
    public Set<String> listNode() {
        return proxy.getNodeCaches();
    }

    @RequestMapping(path = "/v1/systemconfig/shardingNode", method = RequestMethod.GET)
    public String shardingNode(@RequestParam(value = "serviceName") String serviceName) {
        return proxy.shardingNode(serviceName);
    }

    @RequestMapping(path = "/v1/systemconfig/switchState", method = RequestMethod.GET)
    public String registerSwitch() {
        try {
            return proxy.switchState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @RequestMapping(path = "/v1/systemconfig/clearMonitor", method = RequestMethod.GET)
    public boolean clearMonitor(@RequestParam(name = "ip") String ip) {
        try {
            etcdProxy.deleteEtcdValueByKey(PER_WORKER_PROCESS_SERVICE.concat(SLASH).concat(ip), true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @ResponseBody
    @RequestMapping(path = "/v1/systemconfig/setSwitchState", method = RequestMethod.POST)
    public Result setSwitch(@RequestParam(name = "state") String state, @RequestParam(name = "token") String requestToken) {
        if (!token.equalsIgnoreCase(requestToken)) {
            return Result.newBuilder().withCode(ERROR).withMessage("invalid token").build();
        }
        proxy.setSwitchState(state);
        return Result.newBuilder().withCode(SUCCESS).withData(Boolean.TRUE.toString()).build();
    }


}
