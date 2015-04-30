package org.amazing.hazelcast.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "registry.enabled", havingValue = "false")
public class StandAloneMemberRegistry implements MemberRegistry {

    private static final Logger logger = LoggerFactory.getLogger(StandAloneMemberRegistry.class);

    @PostConstruct
    private void initialize() {
        logger.info("Initialising {}", this.getClass().getSimpleName());
    }

    @Override
    public void register(ServerInstance s) {
        logger.debug("Adding {} to member registry", s);
    }

    @Override
    public void unregister(ServerInstance s) {
        logger.debug("Removing {} from member registry", s);
    }

    @Override
    public Set<ServerInstance> list() {
        logger.debug("Member list requested");
        return Collections.emptySet();
    }
}
