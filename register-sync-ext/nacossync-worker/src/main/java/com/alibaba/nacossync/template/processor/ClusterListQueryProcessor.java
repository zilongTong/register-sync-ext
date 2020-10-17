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
package com.alibaba.nacossync.template.processor;

import com.alibaba.nacossync.dao.ClusterAccessService;
import com.alibaba.nacossync.pojo.model.ClusterDO;
import com.alibaba.nacossync.pojo.page.Pagination;
import com.alibaba.nacossync.pojo.request.ClusterListQueryRequest;
import com.alibaba.nacossync.pojo.result.ClusterListQueryResult;
import com.alibaba.nacossync.pojo.view.ClusterModel;
import com.alibaba.nacossync.template.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author NacosSync
 * @version $Id: ClusterListQueryProcessor.java, v 0.1 2018-09-30 PM2:33 NacosSync Exp $$
 */
@Service
public class ClusterListQueryProcessor implements
        Processor<ClusterListQueryRequest, ClusterListQueryResult> {

    @Autowired
    private ClusterAccessService clusterAccessService;

    @Override
    public void process(ClusterListQueryRequest clusterListQueryRequest,
                        ClusterListQueryResult clusterListQueryResult, Object... others) {
        Integer pageNum = clusterListQueryRequest.getPageNum();
        Integer pageSize = clusterListQueryRequest.getPageSize();
        String clusterName = clusterListQueryRequest.getClusterName();
        Pagination<ClusterDO> pagination = clusterAccessService.findPage(pageNum, pageSize, clusterName);

        List<ClusterModel> clusterModelList = pagination.getResult().stream().map(val -> {
            ClusterModel clusterModel = new ClusterModel();
            clusterModel.setClusterId(val.getClusterId());
            clusterModel.setClusterName(val.getClusterName());
            clusterModel.setClusterType(val.getClusterType());
            clusterModel.setConnectKeyList(val.getConnectKeyList());
            return clusterModel;
        }).collect(Collectors.toList());

        clusterListQueryResult.setClusterModels(clusterModelList);
        clusterListQueryResult.setTotalPage(pagination.getTotalPage());
        clusterListQueryResult.setTotalSize(pagination.getTotalCount());
        clusterListQueryResult.setCurrentSize(clusterModelList.size());

    }
}
