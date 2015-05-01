package org.amazing.hazelcast.cluster;

import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class LoggingMembershipListener implements MembershipListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggingMembershipListener.class);

    @Override
    public void memberAdded(MembershipEvent me) {
        InetSocketAddress address = me.getMember().getSocketAddress();
        logger.info("Member {}:{} joined the cluster", address.getHostName(), address.getPort());
    }

    @Override
    public void memberRemoved(MembershipEvent me) {
        InetSocketAddress address = me.getMember().getSocketAddress();
        logger.info("Member {}:{} was removed from the cluster", address.getHostName(), address.getPort());
    }

    @Override
    public void memberAttributeChanged(MemberAttributeEvent mae) {
        logger.info("Member attribute changed");
    }
}
