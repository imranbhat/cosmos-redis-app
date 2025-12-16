package com.example.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Redis connection configuration with Lettuce client.
 * Supports both standalone (local) and cluster (production) modes.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration commandTimeout;

    @Value("${spring.data.redis.connect-timeout:2000ms}")
    private Duration connectTimeout;

    /**
     * Standalone Redis connection for local development.
     */
    @Bean
    @Profile("local")
    public LettuceConnectionFactory redisConnectionFactoryLocal() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(commandTimeout)
                .poolConfig(poolConfig())
                .clientOptions(clientOptions())
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * Redis Cluster connection for production environment.
     * Configured with TLS, connection pooling, and topology refresh.
     */
    @Bean
    @Profile("production")
    public LettuceConnectionFactory redisConnectionFactoryProduction(
            @Value("${spring.data.redis.cluster.nodes}") List<String> clusterNodes,
            @Value("${spring.data.redis.ssl.enabled:true}") boolean sslEnabled) {

        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
        clusterConfig.setMaxRedirects(3);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            clusterConfig.setPassword(redisPassword);
        }

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration
                .builder()
                .commandTimeout(commandTimeout)
                .poolConfig(poolConfig())
                .clientOptions(clusterClientOptions());

        if (sslEnabled) {
            builder.useSsl();
        }

        return new LettuceConnectionFactory(clusterConfig, builder.build());
    }

    /**
     * Default connection factory for non-profiled environments.
     */
    @Bean
    @Profile("!local & !production")
    public LettuceConnectionFactory redisConnectionFactoryDefault() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(commandTimeout)
                .poolConfig(poolConfig())
                .clientOptions(clientOptions())
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * RedisTemplate with JSON serialization for values.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Connection pool configuration.
     */
    private GenericObjectPoolConfig<?> poolConfig() {
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(4);
        poolConfig.setMaxWait(Duration.ofMillis(1000));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        return poolConfig;
    }

    /**
     * Lettuce client options for standalone mode.
     */
    private ClientOptions clientOptions() {
        return ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(connectTimeout)
                        .keepAlive(true)
                        .build())
                .timeoutOptions(TimeoutOptions.enabled())
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build();
    }

    /**
     * Cluster client options with topology refresh for production.
     */
    private ClusterClientOptions clusterClientOptions() {
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30))
                .enableAllAdaptiveRefreshTriggers()
                .build();

        return ClusterClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(connectTimeout)
                        .keepAlive(true)
                        .build())
                .timeoutOptions(TimeoutOptions.enabled())
                .topologyRefreshOptions(topologyRefreshOptions)
                .autoReconnect(true)
                .validateClusterNodeMembership(false)
                .build();
    }
}
