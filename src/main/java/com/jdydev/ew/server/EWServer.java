package com.jdydev.ew.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.app.SimpleApplication;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.system.JmeContext;

public class EWServer extends SimpleApplication implements ConnectionListener {

    private static Logger log = LoggerFactory.getLogger(EWServer.class);
    public static final int SERVER_PORT = 7777;
    public static final String SERVER_NAME = "Endless Wilderness";
    public static final int SERVER_VERSION = 1;

    public static void main(String[] args) {
        EWServer app = new EWServer();
        app.start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        try {
            Server myServer = Network.createServer(SERVER_PORT);
            myServer.addConnectionListener(this);
            myServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Error while starting up server.", e);
        }
    }

    @Override
    public void connectionAdded(Server s, HostedConnection conn) {
        log.debug("Connection Added from {} with id {}", conn.getAddress(), conn.getId());
    }

    @Override
    public void connectionRemoved(Server s, HostedConnection conn) {
        log.debug("Connection Removed by {}", conn.getId());
    }

}
