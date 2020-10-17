package com.alibaba.nacossync.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.MapUtils;


import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * okhttp客户端
 */
@Slf4j
public class OkHttpUtil {

    static ObjectMapper mapper = new ObjectMapper();

    private static OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .connectTimeout(2, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(500, 5, TimeUnit.MINUTES)).build();


    public static OkHttpClient getOkHttpClient() {
        return OK_HTTP_CLIENT;
    }

    /**
     * @param url          请求的url
     * @param queries      请求的参数 没有可以传null
     * @param headerParams 请求header中的参数 没有可以传null
     */
    public static String get(String url, Map<String, String> queries, Map<String, String> headerParams) {
        StringBuilder stringBuilder = new StringBuilder(url);
        if (!MapUtils.isEmpty(queries)) {
            stringBuilder.append("?");
            queries.forEach((k, v) -> stringBuilder.append(k).append("=").append(v).append("&"));
        }
        Request.Builder builder = new Request.Builder().url(stringBuilder.toString());
        if (!MapUtils.isEmpty(headerParams)) {
            Headers headers = Headers.of(headerParams).newBuilder().build();
            builder.headers(headers);
        }
        return client(builder.build());
    }

    /**
     * @param url     请求的url
     * @param queries 请求的参数 没有可以传null
     */
    public static String get(String url, Map<String, String> queries) {
        StringBuilder stringBuilder = new StringBuilder(url);
        if (!MapUtils.isEmpty(queries)) {
            stringBuilder.append("?");
            queries.forEach((k, v) -> stringBuilder.append(k).append("=").append(v).append("&"));
        }
        Request request = new Request.Builder().url(stringBuilder.toString()).build();
        return client(request);
    }

    /**
     * post
     *
     * @param url          请求的url
     * @param params       post form 提交的参数
     * @param headerParams 请求header中的参数 没有可以传null
     * @return
     */
    public static String post(String url, Map<String, String> params, Map<String, String> headerParams) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        // 添加参数
        if (!MapUtils.isEmpty(params)) {
            params.forEach(formBuilder::add);
        }
        Request.Builder builder = new Request.Builder().url(url).post(formBuilder.build());
        if (!MapUtils.isEmpty(headerParams)) {
            Headers headers = Headers.of(headerParams).newBuilder().build();
            builder.headers(headers);
        }
        return client(builder.build());
    }

    /**
     * post
     *
     * @param url    请求的url
     * @param params post form 提交的参数
     * @return
     */
    public static String post(String url, Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        // 添加参数
        if (!MapUtils.isEmpty(params)) {
            params.forEach(builder::add);
        }
        Request request = new Request.Builder().url(url).post(builder.build()).build();
        return client(request);
    }

    /**
     * @param url          请求的url
     * @param jsonParams   post 提交的参数
     * @param headerParams 请求header中的参数 没有可以传null
     */
    public static String postJsonParams(String url, String jsonParams, Map<String, String> headerParams) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams);
        Request.Builder builder = new Request.Builder().url(url).post(requestBody);
        if (!MapUtils.isEmpty(headerParams)) {
            Headers headers = Headers.of(headerParams).newBuilder().build();
            builder.headers(headers);
        }
        return client(builder.build());
    }

    /**
     * @param url        请求的url
     * @param jsonParams post 提交的参数
     */
    public static String postJsonParams(String url, String jsonParams) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams);
        Request request = new Request.Builder().url(url).post(requestBody).build();
        return client(request);
    }

    public static String postJsonParams(String url, Object jsonParams) {

        RequestBody requestBody = null;
        try {
            requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), mapper.writeValueAsString(jsonParams));
        } catch (JsonProcessingException e) {
            log.error("postJsonParams error:{}", e);
        }
        Request request = new Request.Builder().url(url).post(requestBody).build();
        return client(request);
    }

    public static String client(Request request) {
        Response response = null;
        String requestJson = null;
        try {
//            requestJson = mapper.writeValueAsString(request.body());
            response = OK_HTTP_CLIENT.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String result = response.body().string();
                if (result == null) result = "";
                return result;
            } else {
                log.error("response error:{}, requestbody:{}", response, requestJson);
            }
        } catch (Exception e) {
            log.error("okhttp3 error, url:{}, requestbody:{}, e:{}", request.url().toString(), requestJson, e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

}
