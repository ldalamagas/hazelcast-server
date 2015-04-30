package org.amazing.hazelcast.discovery;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ServerInstance {

    private String host;
    private int port;

    public ServerInstance() {

    }

    public ServerInstance(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @JsonIgnore
    public String getUrl() {
        return host + ":" + String.valueOf(port);
    }

    @Override
    public String toString() {
        return getUrl();
    }
}
