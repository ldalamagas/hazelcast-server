package org.amazing.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.amazing.hazelcast.cluster.LoggingMembershipListener;
import org.amazing.hazelcast.discovery.MemberRegistry;
import org.amazing.hazelcast.discovery.ServerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final String INTERFACE_NOT_DEFINED = "";

    @Autowired
    private MemberRegistry registry;


    @Value("${hazelcast.port:5701}")
    private int port;

    @Value("${hazelcast.group:local}")
    private String group;

    @Value("${hazelcast.interface:}")
    private String networkInterface;

    private HazelcastInstance hazelcast;
    private ServerInstance serverInstance;

    @PostConstruct
    public void initialize() {
        try {

            logger.info("Initializing {}", this.getClass().getSimpleName());
            Set<ServerInstance> instances = registry.list();
            hazelcast = Hazelcast.newHazelcastInstance(createConfiguration(instances));
            hazelcast.getCluster().addMembershipListener(new LoggingMembershipListener());
            introduceSelfToRegistry();
        } catch (Exception e) {
            logger.error("Failed to initialize server", e);
        }
    }

    private void introduceSelfToRegistry() {
        InetSocketAddress address = hazelcast.getCluster().getLocalMember().getSocketAddress();
        String host = address.getHostName();
        int port = address.getPort();
        serverInstance = new ServerInstance(host, port);
        registry.register(serverInstance);
    }

    @PreDestroy
    public void destroy() {
        logger.info("Destroying {}", this.getClass().getSimpleName());
        registry.unregister(serverInstance);
        hazelcast.shutdown();
    }

    private Config createConfiguration(Set<ServerInstance> instances) {
        List<String> instanceUrls = new ArrayList<String>();

        for(ServerInstance si : instances) {
            instanceUrls.add(si.getUrl());
        }

        Config config = new Config();
        config.getGroupConfig().setName(group);
        config.getNetworkConfig().setPort(port);
        if (!INTERFACE_NOT_DEFINED.equals(networkInterface)) {
            config.getNetworkConfig().getInterfaces().setEnabled(true);
            config.getNetworkConfig().getInterfaces().setInterfaces(Collections.singletonList(networkInterface));
        }
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setMembers(instanceUrls);

        return config;
    }
}
