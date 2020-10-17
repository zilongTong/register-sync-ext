package com.alibaba.nacossync.util;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import lombok.experimental.UtilityClass;

import java.nio.charset.Charset;

/**
 * Created by liaomengge on 2020/6/16.
 */
@UtilityClass
public class ByteSequenceUtil {

    public ByteSequence fromString(String source) {
        return ByteSequence.from(source, Charset.forName("UTF-8"));
    }

    public String fromKv(KeyValue kv) {
        return kv.getValue().toString(Charset.forName("UTF-8"));
    }
}
