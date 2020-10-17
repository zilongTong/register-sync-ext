package com.alibaba.nacossync.service.factory;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacossync.constant.ClusterTypeEnum;
import com.alibaba.nacossync.constant.TaskStatusEnum;
import com.alibaba.nacossync.dao.ClusterAccessService;
import com.alibaba.nacossync.dao.TaskAccessService;
import com.alibaba.nacossync.extension.holder.EurekaServerHolder;
import com.alibaba.nacossync.pojo.model.ClusterDO;
import com.alibaba.nacossync.pojo.model.TaskDO;
import com.alibaba.nacossync.util.OkHttpUtil;
import com.alibaba.nacossync.util.SkyWalkerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NacosDataSync implements SyncCluster {

    @Autowired
    private TaskAccessService taskAccessService;

    @Autowired
    private ClusterAccessService clusterAccessService;

    @Autowired
    private EurekaServerHolder eurekaServerHolder;


    public static void main(String[] args) {
        String urlKey = "http://nacos.zmaxis.com";
        Map<String, String> queries = new HashMap<>();
        queries.put("pageNo", "1");
        queries.put("pageSize", "100000");
        queries.put("hasIpCount", "true");

        String response = OkHttpUtil.get(urlKey.concat("/nacos/v1/ns/syncHelper/services"), queries);
        System.out.println(response);


        Map<String, List<Service>> map = JSONObject.parseObject(response, Map.class);
        List<Service> services = map.get("serviceList");
        System.out.println(map);

    }

    @Override
    public void syncClusterData() {

        List<TaskDO> taskDOS = taskAccessService.findAll();
        ClusterDO nacosClusterDO = clusterAccessService.findByClusterType(ClusterTypeEnum.NACOS.name());
        ClusterDO eurekaClusterDO = clusterAccessService.findByClusterType(ClusterTypeEnum.EUREKA.name());
        List<String> connectKeyList = JSONObject.parseObject(nacosClusterDO.getConnectKeyList(),
                new TypeReference<List<String>>() {
                });
        //http://nacos.zmaxis.com/nacos/v1/ns/syncHelper/services?pageNo=1&pageSize=100000&withInstances=true
        if (CollectionUtils.isEmpty(connectKeyList)) {
            return;
        }
        String urlKey = connectKeyList.get(0);
        Map<String, String> queries = new HashMap<>();
        queries.put("pageNo", "1");
        queries.put("pageSize", "100000");
        queries.put("hasIpCount", "true");

        String response = OkHttpUtil.get(urlKey.concat("/nacos/v1/ns/syncHelper/services"), queries);
        Map<String, List> map = JSONObject.parseObject(response, Map.class);
        List services = map.get("serviceList");
        for (Object o : services) {
            JSONObject jo = (JSONObject) o;
            Service a = jo.toJavaObject(Service.class);
            String taskId = SkyWalkerUtil.generateTaskId(a.getName().toLowerCase(),
                    "public", nacosClusterDO.getClusterId(), eurekaClusterDO.getClusterId());
            TaskDO taskDO = new TaskDO();
            taskDO.setTaskId(taskId);
            taskDO.setSourceClusterId(nacosClusterDO.getClusterId());
            taskDO.setDestClusterId(eurekaClusterDO.getClusterId());
            taskDO.setServiceName(a.getName().toLowerCase());
            taskDO.setVersion("");
            taskDO.setGroupName("public");
            taskDO.setNameSpace("");
            taskDO.setTaskStatus(TaskStatusEnum.SYNC.getCode());
            try {
                taskDO.setWorkerIp(SkyWalkerUtil.getLocalIp());
            } catch (Exception e) {
                e.printStackTrace();
            }
            taskDO.setOperationId(SkyWalkerUtil.generateOperationId());
            if (!taskDOS.contains(taskDO)) {
                taskAccessService.addTask(taskDO);
            }
        }
    }
}
