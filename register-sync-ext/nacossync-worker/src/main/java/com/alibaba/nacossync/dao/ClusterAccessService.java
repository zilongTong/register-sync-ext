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
package com.alibaba.nacossync.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacossync.constant.EtcdConstants;
import com.alibaba.nacossync.pojo.model.ClusterDO;
import com.alibaba.nacossync.pojo.page.Pagination;
import com.alibaba.nacossync.util.*;
import com.google.common.collect.Lists;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.GetOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author NacosSync
 * @version $Id: ClusterAccessService.java, v 0.1 2018-09-25 PM9:32 NacosSync Exp $$
 */
@Service
@Slf4j
public class ClusterAccessService {

    @Autowired
    private Client client;

    public ClusterDO insert(ClusterDO clusterDO) {
        String clusterId = clusterDO.getClusterId();
        ByteSequence key = ByteSequenceUtil.fromString(generateClusterIdNode(clusterId));
        ByteSequence value = ByteSequenceUtil.fromString(JSON.toJSONString(clusterDO));
        CompletableFuture<PutResponse> future = client.getKVClient().put(key, value);
        return CompletableFutureUtil.handle(future, resp -> EtcdResponseUtil.get(resp, ClusterDO.class));
    }

    public Long deleteByClusterId(String clusterId) {
        ByteSequence key = ByteSequenceUtil.fromString(generateClusterIdNode(clusterId));
        CompletableFuture<DeleteResponse> future = client.getKVClient().delete(key);
        return CompletableFutureUtil.handle(future, EtcdResponseUtil::get);
    }

    public ClusterDO findByClusterId(String clusterId) {
        ByteSequence key = ByteSequenceUtil.fromString(generateClusterIdNode(clusterId));
        CompletableFuture<GetResponse> future = client.getKVClient().get(key);
        return CompletableFutureUtil.handle(future, resp -> EtcdResponseUtil.get(resp, ClusterDO.class));
    }

    public ClusterDO findByClusterType(String type) {
        ClusterDO retClusterDO = null;
        List<ClusterDO> clusterDOList = buildClusterCaffeineCache();
        if (CollectionUtils.isNotEmpty(clusterDOList)) {
            retClusterDO =
                    clusterDOList.stream().filter(val -> type.equals(val.getClusterType())).findFirst().orElse(null);
        }
        if (Objects.nonNull(retClusterDO)) {
            return retClusterDO;
        }
        String rootNode = EtcdConstants.ETCD_CLUSTER_ROOT_NODE;
        ByteSequence key = ByteSequenceUtil.fromString(rootNode);
        CompletableFuture<GetResponse> future = client.getKVClient().get(key,
                GetOption.newBuilder().withPrefix(key).build());
        clusterDOList = CompletableFutureUtil.handle(future, resp -> EtcdResponseUtil.getAll(resp, ClusterDO.class));
        return clusterDOList.stream().filter(val -> type.equals(val.getClusterType())).findFirst().orElse(null);
    }

    public Pagination<ClusterDO> findPage(Integer pageNum, Integer pageSize, String clusterName) {
        List<ClusterDO> clusterDOList = buildClusterCaffeineCache();
        if (!StringUtils.isEmpty(clusterName)) {
            clusterDOList =
                    clusterDOList.stream().filter(Objects::nonNull).filter(val -> val.getClusterName().contains(clusterName)).collect(Collectors.toList());
        }
        return RamPageUtil.page(clusterDOList, pageNum, pageSize);
    }

    private List<ClusterDO> buildClusterCaffeineCache() {
        String cacheKey = EtcdConstants.ETCD_CLUSTER_CAFFEIN;
        return Optional.ofNullable(CaffeineUtil.get(cacheKey, val -> {
            String rootNode = EtcdConstants.ETCD_CLUSTER_ROOT_NODE;
            ByteSequence key = ByteSequenceUtil.fromString(rootNode);
            CompletableFuture<GetResponse> future = client.getKVClient()
                    .get(key,
                            GetOption.newBuilder().withPrefix(key).withSortOrder(GetOption.SortOrder.DESCEND).build());
            return CompletableFutureUtil.handle(future, resp -> EtcdResponseUtil.getAll(resp, ClusterDO.class));
        })).orElse(Lists.newArrayList());
    }

    private String generateClusterIdNode(String clusterId) {
        return EtcdConstants.ETCD_CLUSTER_ROOT_NODE + EtcdConstants.DELIMITER + clusterId;
    }
}
