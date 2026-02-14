package com.rootlink.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RootLink 后端应用启动类
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.rootlink.backend.mapper")
public class RootLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(RootLinkApplication.class, args);
        System.out.println("\n" +
                "  ____              _   _     _       _    \n" +
                " |  _ \\ ___   ___ | |_| |   (_)_ __ | | __\n" +
                " | |_) / _ \\ / _ \\| __| |   | | '_ \\| |/ /\n" +
                " |  _ < (_) | (_) | |_| |___| | | | |   < \n" +
                " |_| \\_\\___/ \\___/ \\__|_____|_|_| |_|_|\\_\\\n" +
                "                                           \n" +
                " :: RootLink Backend :: Running on port 8080 ::\n");
    }
}
