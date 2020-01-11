/**
 * Copyright (c) 2005-2009 springside.org.cn
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * <p>
 * $Id: Struts2Utils.java 763 2009-12-27 18:36:21Z calvinxiu $
 */
package org.scleropages.crud.web;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author calvin
 */
public class Views {

    // -- header 常量定义 --//
    private static final String HEADER_ENCODING = "encoding";
    private static final String HEADER_NOCACHE = "no-cache";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final boolean DEFAULT_NOCACHE = true;

    // -- content-type 常量定义 --//
    private static final String TEXT_TYPE = "text/plain";
    private static final String JSON_TYPE = "application/json";
    private static final String XML_TYPE = "text/xml";
    private static final String HTML_TYPE = "text/html";
    private static final String JS_TYPE = "text/javascript";

    /**
     * 直接输出内容的简便函数.
     * <p>
     * eg. render("text/plain", "hello", "encoding:GBK"); render("text/plain",
     * "hello", "no-cache:false"); render("text/plain", "hello", "encoding:GBK",
     * "no-cache:false");
     *
     * @param headers 可变的header数组，目前接受的值为"encoding:"或"no-cache:",默认值分别为UTF-8和true.
     */
    public static void render(HttpServletResponse response, final String contentType, final String content,
                              final String... headers) {
        prepareResponse(response, contentType, headers);
        try {
            response.getWriter().write(content);
            response.getWriter().flush();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 直接输出文本.
     *
     * @param response
     * @param text
     * @param headers
     */
    public static void renderText(HttpServletResponse response, final String text, final String... headers) {
        render(response, TEXT_TYPE, text, headers);
    }


    /**
     * 直接输出HTML.
     *
     * @param response
     * @param html
     * @param headers
     */
    public static void renderHtml(HttpServletResponse response, final String html, final String... headers) {
        render(response, HTML_TYPE, html, headers);
    }


    /**
     * 直接输出XML.
     *
     * @param response
     * @param xml
     * @param headers
     */
    public static void renderXml(HttpServletResponse response, final String xml, final String... headers) {
        render(response, XML_TYPE, xml, headers);
    }

    /**
     * 直接输出JSON.
     *
     * @param response
     * @param jsonString
     * @param headers
     */
    public static void renderJson(HttpServletResponse response, final String jsonString, final String... headers) {
        render(response, JSON_TYPE, jsonString, headers);
    }

    /**
     * 直接输出支持跨域Mashup的JSONP.
     *
     * @param callbackName callback函数名.
     * @param object       Java对象,可以是List<POJO>, POJO[], POJO ,也可以Map名值对, 将被转化为json字符串.
     */
    public static void renderJsonp(HttpServletResponse response, String jsonString, final String callbackName,
                                   final Object object, final String... headers) {

        String result = new StringBuilder().append(callbackName).append("(").append(jsonString).append(");").toString();

        // 渲染Content-Type为javascript的返回内容,输出结果为javascript语句,
        // 如callback197("{html:'Hello World!!!'}");
        render(response, JS_TYPE, result, headers);
    }

    private static HttpServletResponse prepareResponse(HttpServletResponse response, final String contentType,
                                                       final String... headers) {
        // 分析headers参数
        String encoding = DEFAULT_ENCODING;
        boolean noCache = DEFAULT_NOCACHE;
        for (String header : headers) {
            String headerName = StringUtils.substringBefore(header, ":");
            String headerValue = StringUtils.substringAfter(header, ":");

            if (StringUtils.equalsIgnoreCase(headerName, HEADER_ENCODING)) {
                encoding = headerValue;
            } else if (StringUtils.equalsIgnoreCase(headerName, HEADER_NOCACHE)) {
                noCache = Boolean.parseBoolean(headerValue);
            } else {
                throw new IllegalArgumentException(headerName + "不是一个合法的header类型");
            }
        }

        // 设置headers参数
        String fullContentType = contentType + ";charset=" + encoding;
        response.setContentType(fullContentType);
        if (noCache) {
            Servlets.setNoCacheHeader(response);
        }

        return response;
    }

    public static String[] encodeHeaders(Map<String, String> headers) {
        String encodedHeaders[] = new String[headers.size()];
        int i = 0;
        for (Entry<String, String> entry : headers.entrySet()) {
            encodedHeaders[i] = entry.getKey() + ":" + entry.getValue();
            i++;
        }
        return encodedHeaders;
    }

}
