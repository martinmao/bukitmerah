/**
 * Copyright 2001-2005 The Apache Software Foundation.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scleropages.crud.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * 设计格式兼容 {@link org.springframework.boot.web.servlet.error.DefaultErrorAttributes}
 *
 * @author <a href="mailto:martinmao@icloud.com">Martin Mao</a>
 * @see org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
 */
public class BizExceptionHttpView {

    private static final Logger logger = LoggerFactory.getLogger(BizExceptionHttpView.class);


    private final Date timestamp;
    private final String path;
    private final String code;
    private final String error;
    private final String[] constraintViolations;
    private final int status;
    private final String message;
    private final ServerHttpResponse outputMessage;

    public BizExceptionHttpView(BizException ex, HttpServletRequest request, HttpServletResponse response, String message) {
        this.code = ex.getCode();
        this.constraintViolations = ex.getConstraintViolations();
        this.status = computeStatus(ex);
        this.message = null != message ? message : ex.getMessage();
        this.timestamp = new Date();
        this.path = request.getRequestURI() + request.getQueryString();
        this.error = ex.getClass().getSimpleName();
        this.outputMessage = new ServletServerHttpResponse(response);
    }


    protected int computeStatus(Exception ex) {
        if (ex instanceof BizParamViolationException)
            return 400;
        if (ex instanceof BizStateViolationException)
            return 300;
        else
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String[] getConstraintViolations() {
        return constraintViolations;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    protected ResponseEntity<BizExceptionHttpView> asResponseEntity(HttpHeaders httpHeaders) {
        return new ResponseEntity(this, httpHeaders, HttpStatus.BAD_REQUEST);
    }

    public void render(HttpHeaders httpHeaders, HttpMessageConverter httpMessageConverter) {
        try {
            ResponseEntity<BizExceptionHttpView> responseEntity = asResponseEntity(httpHeaders);
            outputMessage.setStatusCode(responseEntity.getStatusCode());
            outputMessage.getHeaders().putAll(responseEntity.getHeaders());
            httpMessageConverter.write(responseEntity.getBody(), null, outputMessage);
            outputMessage.flush();
        } catch (IOException e) {
            throw new IllegalStateException("failure to render BizException. caused by: " + e.getMessage(), e);
        }
    }
}
