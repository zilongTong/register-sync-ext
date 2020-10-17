package com.alibaba.nacossync.extension.ha;

import com.alibaba.nacossync.util.StringSerializer;
import com.alibaba.nacossync.util.Utils;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class ConsistentHash<T> {
    private final int numberOfReplicas;
    private final SortedMap<Integer, T> circle = new TreeMap<Integer, T>();

    public ConsistentHash(int numberOfReplicas,
                          Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {

            String nodestr = node.toString() + i;
//            int hashcode = getMurmur2Hash(nodestr);
            int hashcode = Math.abs(new Long(new MD5Hash().hash(nodestr.toString())).intValue());
//            int hashcode = getHash(nodestr);
//            int hashcode = nodestr.hashCode();
            circle.put(hashcode, node);

        }
    }

    public static String stringToAscii(String value) {
        StringBuffer sbu = new StringBuffer();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i != chars.length - 1) {
                sbu.append((int) chars[i]).append(",");
            } else {
                sbu.append((int) chars[i]);
            }
        }
        return sbu.toString();
    }


    private static class MD5Hash {
        MessageDigest instance;

        public MD5Hash() {
            try {
                instance = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
            }
        }

        public long hash(String key) {
            instance.reset();
            instance.update(key.getBytes());
            byte[] digest = instance.digest();

            long h = 0;
            for (int i = 0; i < 4; i++) {
                h <<= 8;
                h |= ((int) digest[i]) & 0xFF;
            }
            return h;
        }
    }

    private int getMurmur2Hash(String str) {
        StringSerializer keySerializer = new StringSerializer();
        byte[] serializedKey = keySerializer.serialize("nacos", str);
        int positive = Utils.murmur2(serializedKey) & 0x7fffffff;
        return positive;
    }

    private int getHash(String str) {
        str = stringToAscii(str);
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++)
            hash = (hash ^ str.charAt(i)) * p;
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        // 如果算出来的值为负数则取其绝对值
        if (hash < 0)
            hash = Math.abs(hash);
        return hash;
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++)
            circle.remove((node.toString() + i).hashCode());
    }

    /**
     * 获得一个最近的顺时针节点,根据给定的key 取Hash
     * 然后再取得顺时针方向上最近的一个虚拟节点对应的实际节点
     * 再从实际节点中取得 数据
     */
    public T getNode(Object key) {
        if (circle.isEmpty())
            return null;
//        int hash = getHash(key.toString());
        int hash = Math.abs(new Long(new MD5Hash().hash(key.toString())).intValue());
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public T getHeadNode() {
        if (circle.isEmpty())
            return null;
        return circle.entrySet().iterator().next().getValue();
    }

    public long getSize() {
        return circle.size();
    }


    /*
     * 查看表示整个哈希环中各个虚拟节点位置
     */
    public void testBalance() {
        Set<Integer> sets = circle.keySet();//获得TreeMap中所有的Key
        SortedSet<Integer> sortedSets = new TreeSet<Integer>(sets);//将获得的Key集合排序
        /*
         * 查看相邻两个hashCode的差值
         */
        Iterator<Integer> it = sortedSets.iterator();
        Iterator<Integer> it2 = sortedSets.iterator();
        if (it2.hasNext())
            it2.next();
        long keyPre, keyAfter;
        while (it.hasNext() && it2.hasNext()) {
            keyPre = it.next();
            keyAfter = it2.next();
//            System.out.println("key distance:" + (keyAfter - keyPre));
        }
    }


}
