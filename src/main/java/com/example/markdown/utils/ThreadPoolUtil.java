package com.example.markdown.utils;

/**
 * @author songquanheng
 * @Time: 2020/3/3-12:00
 */

import com.example.markdown.common.SimpleThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wycm on 2018/10/22.
 */
public class ThreadPoolUtil {

    private static SimpleThreadPoolExecutor simpleThreadPoolExecutor;

    public static synchronized SimpleThreadPoolExecutor getThreadPoolExecutor() {
        if (simpleThreadPoolExecutor == null) {
            simpleThreadPoolExecutor = new SimpleThreadPoolExecutor(
                    500,
                    1000,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(100),
                    new ThreadPoolExecutor.DiscardPolicy(),
                    "simpleThreadPoolExecutor");
        }
        return simpleThreadPoolExecutor;
    }


}

