package org.amazing.hazelcast.cluster;

import com.hazelcast.cluster.impl.TcpIpJoiner;
import com.hazelcast.instance.Node;

public class ZookeeperJoiner extends TcpIpJoiner {
    public ZookeeperJoiner(Node node) {
        super(node);
    }
}
