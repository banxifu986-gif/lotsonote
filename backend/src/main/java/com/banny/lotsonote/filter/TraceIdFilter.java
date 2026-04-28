package com.banny.lotsonote.filter;

import org.slf4j.MDC;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 生成并存储 traceId
            String traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID_KEY, traceId);
            chain.doFilter(request, response);
        } finally {
            // 清理 MDC 中的 traceId
            MDC.remove(TRACE_ID_KEY);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}
