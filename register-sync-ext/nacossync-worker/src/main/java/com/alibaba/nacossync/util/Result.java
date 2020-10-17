package com.alibaba.nacossync.util;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable, Cloneable {

    public static String EXCEPTION_ERROR = "500";
    public static String ERROR = "1";
    public static String SUCCESS = "0";

    private final String code;

    private final T data;

    private final String message;

    public Result(String code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static Result.Builder newBuilder() {
        return new Builder();
    }


    public static class Builder<T> {

        private String code;

        private T data;

        private String message;


        public Builder withCode(String code) {
            this.code = code;
            return this;
        }

        public Builder withData(T data) {
            this.data = data;
            return this;
        }

        public Builder withMessage(String msg) {
            this.message = msg;
            return this;
        }

        public Result build() {
            return new Result(code, data, message);
        }

    }


}
