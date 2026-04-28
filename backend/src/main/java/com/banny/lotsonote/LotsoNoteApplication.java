package com.banny.lotsonote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @ClassName LotsoNoteApplication
 * @Description ToDo
 * @Author Tong
 * @LastChangeDate 2024-12-16 11:08
 * @Version v1.0
 */
@SpringBootApplication(exclude = {RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class})
@EnableScheduling
public class LotsoNoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(LotsoNoteApplication.class, args);
    }
}
