package com.alibaba.nacossync.util;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by liaomengge on 2020/6/16.
 */
@UtilityClass
public class EtcdResponseUtil {

    public String get(GetResponse resp) {
        long count = resp.getCount();
        if (count > 0) {
            KeyValue kv = resp.getKvs().get(0);
            if (Objects.nonNull(kv)) {
                return ByteSequenceUtil.fromKv(kv);
            }
        }
        return null;
    }

    public <T> T get(GetResponse resp, Class<T> clazz) {
        long count = resp.getCount();
        if (count > 0) {
            KeyValue kv = resp.getKvs().get(0);
            if (Objects.nonNull(kv)) {
                return JSON.parseObject(ByteSequenceUtil.fromKv(kv), clazz);
            }
        }
        return null;
    }

    public <T> List<T> getAll(GetResponse resp, Class<T> clazz) {
        long count = resp.getCount();
        if (count > 0) {
            return resp.getKvs().stream().map(kv -> JSON.parseObject(ByteSequenceUtil.fromKv(kv), clazz)).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public <T> T get(PutResponse resp, Class<T> clazz) {
        return JSON.parseObject(ByteSequenceUtil.fromKv(resp.getPrevKv()), clazz);
    }

    public long get(DeleteResponse resp) {
        return resp.getDeleted();
    }
}
