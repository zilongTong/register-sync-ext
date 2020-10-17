package com.alibaba.nacossync.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Created by liaomengge on 2020/6/15.
 */
@Slf4j
@UtilityClass
public class CompletableFutureUtil {

    public <T> void handle(CompletableFuture<T> future) {
        future.whenComplete((resp, throwable) -> {
            if (Objects.nonNull(throwable)) {
                log.error("handle completable future exception", throwable);
            }
        }).join();
    }

    public <T, R> R handle(CompletableFuture<T> future, Function<T, R> function) {
        return future.handle((resp, throwable) -> {
            if (Objects.nonNull(throwable)) {
                log.error("handle completable future exception", throwable);
                return null;
            }
            return function.apply(resp);
        }).join();
    }
}
