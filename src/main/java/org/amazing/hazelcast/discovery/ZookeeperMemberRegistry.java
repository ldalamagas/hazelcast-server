package org.amazing.hazelcast.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "registry.enabled", havingValue = "true")
public class ZookeeperMemberRegistry implements MemberRegistry, ConnectionStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperMemberRegistry.class);
    private static final RetryPolicy RETRY_POLICY = new ExponentialBackoffRetry(3000, 3);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final CountDownLatch initializationLatch = new CountDownLatch(1);

    @Value("${zookeeper.host}")
    private String host;

    @Value("${zookeeper.port}")
    private int port;

    @Value("${zookeeper.path}")
    private String path;

    @Value("${zookeeper.connection.timeout}")
    private int timeout;

    private CuratorFramework client;
    private PathChildrenCache registryCache;

    @PostConstruct
    private void initialize() {

        logger.info("Initialising {}", this.getClass().getSimpleName());

        String connectionString = host + ":" + String.valueOf(port);
        client = CuratorFrameworkFactory.newClient(connectionString, RETRY_POLICY);
        client.getConnectionStateListenable().addListener(this);
        client.start();
        try {

            logger.debug("Awaiting zookeeper connection, timeout set to {} seconds", timeout);

            if (!client.blockUntilConnected(timeout, TimeUnit.SECONDS)) {
                throw new RuntimeException("Zookeeper connection timed out");
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to establish zookeeper connection", e);
        }

        try {
            initializationLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while initializing member registry", e);
        }

        logger.info("{} initialization complete", this.getClass().getSimpleName());
    }

    @PreDestroy
    private void destroy() {
        logger.info("Destroying {} ", this.getClass().getSimpleName());
        CloseableUtils.closeQuietly(registryCache);
        CloseableUtils.closeQuietly(client);
    }

    @Override
    public void register(ServerInstance s) {
        logger.debug("Adding {} to member registry", s);
        try {
            String json = objectMapper.writeValueAsString(s);
            client.create().withMode(CreateMode.EPHEMERAL).forPath(ZKPaths.makePath(path, s.getUrl()), json.getBytes());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise server instance " + s, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register server instance " + s, e);
        }
    }

    @Override
    public void unregister(ServerInstance s) {
        logger.debug("Removing {} from member registry", s);
        try {
            client.delete().forPath(ZKPaths.makePath(path, s.getUrl()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to unregister server instance " + s, e);
        }
    }

    @Override
    public Set<ServerInstance> list() {
        logger.debug("Member list requested");
        List<ChildData> childrenData = registryCache.getCurrentData();
        Set<ServerInstance> instances = new HashSet<ServerInstance>();

        try {
            for (ChildData cd : childrenData) {
                String json = new String(cd.getData());
                instances.add(objectMapper.readValue(json, ServerInstance.class));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve server instances", e);
        }
        logger.debug("Registered members {}", instances);
        return Collections.unmodifiableSet(instances);
    }

    private void onZookeeperConnected() {
        logger.debug("Zookeeper connection established");
        try {
            logger.debug("Initializing registry cache");
            registryCache = new PathChildrenCache(client, path, true);
            registryCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            initializationLatch.countDown();
            logger.debug("Cache initialization complete");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize registry cache", e);
        }

    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTED:
                onZookeeperConnected();
                break;
            case SUSPENDED:
                logger.warn("Zookeeper connection suspended");
                break;
            case RECONNECTED:
                logger.info("Zookeeper connection restored");
                break;
            case LOST:
                logger.warn("Zookeeper connection lost");
                break;
            case READ_ONLY:
                logger.warn("Zookeeper connection established");
                break;
        }
    }
}
