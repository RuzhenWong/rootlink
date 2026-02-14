package com.rootlink.backend.config;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NebulaGraph 连接池配置
 *
 * 连接失败时不抛异常，返回 null，由 NebulaUtil 中的
 * @Autowired(required = false) 接收，EulogyService 自动降级到 MySQL 查询。
 */
@Slf4j
@Configuration
public class NebulaConfig {

    @Value("${nebula.graph.hosts:127.0.0.1:9669}")
    private String hosts;

    @Value("${nebula.pool.max-conn-size:10}")
    private int maxConnSize;

    @Value("${nebula.pool.min-conn-size:1}")
    private int minConnSize;

    @Value("${nebula.pool.idle-time:0}")
    private int idleTime;

    @Value("${nebula.enabled:false}")
    private boolean enabled;

    @Bean(destroyMethod = "")   // destroyMethod="" 防止 null 时调用 close() 报 NPE
    public NebulaPool nebulaPool() {
        if (!enabled) {
            log.info("NebulaGraph 已禁用（nebula.enabled=false），将使用 MySQL 降级查询");
            return null;
        }

        try {
            NebulaPoolConfig poolConfig = new NebulaPoolConfig();
            poolConfig.setMaxConnSize(maxConnSize);
            poolConfig.setMinConnSize(minConnSize);
            poolConfig.setIdleTime(idleTime);

            List<com.vesoft.nebula.client.graph.data.HostAddress> addressList =
                Arrays.stream(hosts.split(","))
                    .map(String::trim)
                    .map(h -> {
                        String[] parts = h.split(":");
                        return new com.vesoft.nebula.client.graph.data.HostAddress(
                            parts[0], Integer.parseInt(parts[1])
                        );
                    })
                    .collect(Collectors.toList());

            NebulaPool pool = new NebulaPool();
            boolean initOk = pool.init(addressList, poolConfig);
            if (!initOk) {
                log.warn("NebulaGraph 连接池初始化失败（服务未启动？），已降级到 MySQL 查询");
                return null;
            }

            log.info("NebulaGraph 连接池初始化成功，hosts={}", hosts);
            return pool;

        } catch (Exception e) {
            log.warn("NebulaGraph 初始化异常，已降级到 MySQL 查询：{}", e.getMessage());
            return null;
        }
    }
}
