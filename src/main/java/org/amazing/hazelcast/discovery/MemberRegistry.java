package org.amazing.hazelcast.discovery;

import java.util.Set;

public interface MemberRegistry {

    void register(ServerInstance s);

    void unregister(ServerInstance s);

    Set<ServerInstance> list();

}
