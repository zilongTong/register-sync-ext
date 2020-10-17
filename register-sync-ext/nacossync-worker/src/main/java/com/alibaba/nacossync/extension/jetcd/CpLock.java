//package com.alibaba.nacossync.extension.jetcd;
//
//import com.google.common.base.Preconditions;
//import io.netty.util.concurrent.FastThreadLocal;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.Objects;
//import java.util.function.Supplier;
//
//@Slf4j
//@Data
//public class CpLock {
//
//    private String lockName;
//
//    private LockEtcdClient lockEtcdClient;
//
//    /**
//     * 分布式锁的锁持有数
//     */
//    private volatile int state;
//
//    private volatile transient Thread lockOwnerThread;
//
//    /**
//     * 当前线程拥有的lease对象
//     */
//    private FastThreadLocal<LockLeaseData> lockLeaseDataFastThreadLocal = new FastThreadLocal<>();
//    /**
//     * 锁自动释放时间，单位s，默认为30
//     */
//    private static Long LOCK_TIME = 30L;
//
//    /**
//     * 获取锁失败单次等待时间，单位ms，默认为300
//     */
//    private static Integer SLEEP_TIME_ONCE = 300;
//
//    CpLock(String lockName, LockEtcdClient lockEtcdClient) {
//        this.lockName = lockName;
//        this.lockEtcdClient = lockEtcdClient;
//    }
//
//    private LockLeaseData getLockLeaseData(String lockName, long lockTime) {
//        if (lockLeaseDataFastThreadLocal.get() != null) {
//            return lockLeaseDataFastThreadLocal.get();
//        } else {
//            LockLeaseData lockLeaseData = lockEtcdClient.getLeaseData(lockName, lockTime);
//            lockLeaseDataFastThreadLocal.set(lockLeaseData);
//            return lockLeaseData;
//        }
//    }
//
//    final Boolean tryLock(long waitTime) {
//        final long startTime = System.currentTimeMillis();
//        final long endTime = startTime + waitTime * 1000;
//        final long lockTime = LOCK_TIME;
//        final Thread current = Thread.currentThread();
//        try {
//            do {
//                int c = this.getState();
//                if (c == 0) {
//                    LockLeaseData lockLeaseData = this.getLockLeaseData(lockName, lockTime);
//                    if (Objects.isNull(lockLeaseData)) {
//                        return Boolean.FALSE;
//                    }
//                    Long leaseId = lockLeaseData.getLeaseId();
//                    if (lockEtcdClient.tryLock(leaseId, lockName, endTime - System.currentTimeMillis())) {
//                        log.info("线程获取重入锁成功,cp锁的名称为{}", lockName);
//                        this.setLockOwnerThread(current);
//                        this.setState(c + 1);
//                        return Boolean.TRUE;
//                    }
//                } else if (lockOwnerThread == Thread.currentThread()) {
//                    if (c + 1 <= 0) {
//                        throw new Error("Maximum lock count exceeded");
//                    }
//                    this.setState(c + 1);
//                    log.info("线程重入锁成功,cp锁的名称为{},当前LockCount为{}", lockName, state);
//                    return Boolean.TRUE;
//                }
//                int sleepTime = SLEEP_TIME_ONCE;
//                if (waitTime > 0) {
//                    log.info("线程暂时无法获得cp锁,当前已等待{}ms,本次将再等待{}ms,cp锁的名称为{}", System.currentTimeMillis() - startTime, sleepTime, lockName);
//                    try {
//                        Thread.sleep(sleepTime);
//                    } catch (InterruptedException e) {
//                        log.info("线程等待过程中被中断,cp锁的名称为{}", lockName, e);
//                    }
//                }
//            } while (System.currentTimeMillis() <= endTime);
//            if (waitTime == 0) {
//                log.info("线程获得cp锁失败,将放弃获取,cp锁的名称为{}", lockName);
//            } else {
//                log.info("线程获得cp锁失败,之前共等待{}ms,将放弃等待获取,cp锁的名称为{}", System.currentTimeMillis() - startTime, lockName);
//            }
//            this.stopKeepAlive();
//            return Boolean.FALSE;
//        } catch (Exception e) {
//            log.error("execute error", e);
//            this.stopKeepAlive();
//            return Boolean.FALSE;
//        }
//    }
//
//    /**
//     * 停止续约，并将租约对象从线程中移除
//     */
//    private void stopKeepAlive() {
//        LockLeaseData lockLeaseData = lockLeaseDataFastThreadLocal.get();
//        if (Objects.nonNull(lockLeaseData)) {
//            lockLeaseData.getCpSurvivalClam().stop();
//            lockLeaseData.setCpSurvivalClam(null);
//            lockLeaseData.getSurvivalThread().interrupt();
//            lockLeaseData.setSurvivalThread(null);
//        }
//        lockLeaseDataFastThreadLocal.remove();
//    }
//
//    final void unLock() {
//        if (lockOwnerThread == Thread.currentThread()) {
//            int c = this.getState() - 1;
//            if (c == 0) {
//                this.setLockOwnerThread(null);
//                this.setState(c);
//                LockLeaseData lockLeaseData = lockLeaseDataFastThreadLocal.get();
//                this.stopKeepAlive();
//                //unLock操作必须在最后执行，避免其他线程获取到锁时的state等数据不正确
//                lockEtcdClient.unLock(lockLeaseData.getLeaseId());
//                log.info("重入锁LockCount-1,线程已成功释放锁,cp锁的名称为{}", lockName);
//            } else {
//                this.setState(c);
//                log.info("重入锁LockCount-1,cp锁的名称为{}，剩余LockCount为{}", lockName, c);
//            }
//        }
//    }
//
//    public <T> T execute(Supplier<T> supplier, int waitTime) {
//        Boolean holdLock = Boolean.FALSE;
//        Preconditions.checkArgument(waitTime >= 0, "waitTime必须为自然数");
//        try {
//            if (holdLock = this.tryLock(waitTime)) {
//                return supplier.get();
//            }
//            return null;
//        } catch (Exception e) {
//            log.error("cpLock execute error", e);
//            return null;
//        } finally {
//            if (holdLock) {
//                this.unLock();
//            }
//        }
//    }
//
//    public <T> T execute(Supplier<T> supplier) {
//        return this.execute(supplier, 0);
//    }
//}
//
//
