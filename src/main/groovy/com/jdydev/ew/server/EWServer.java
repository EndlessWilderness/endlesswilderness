package com.jdydev.ew.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jdydev.ew.comm.CommUtil;
import com.jdydev.ew.comm.LocationMessage;
import com.jdydev.ew.comm.LoginMessage;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filter;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.system.JmeContext;

public class EWServer extends SimpleApplication implements ConnectionListener {

    private static Logger log = LoggerFactory.getLogger(EWServer.class);
    public static final int SERVER_PORT = 7777;
    public static final String SERVER_HOST = "ewserver.jdydev.com";
    // Toggle the above and below for local testing
    // public static final String SERVER_HOST = "localhost";
    public static final String SERVER_NAME = "Endless Wilderness";
    public static final int SERVER_VERSION = 1;

    private Map<String, String> logins = new HashMap<String, String>();
    // Might not need this, keeping for now
    private Map<Integer, String> connLogin = new HashMap<Integer, String>();
    private Map<String, Vector3f> userLoc = new HashMap<String, Vector3f>();

    public static void main(String[] args) {
        EWServer app = new EWServer();
        app.start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        CommUtil.registerMessages();
        try {
            Server myServer = Network.createServer(SERVER_NAME, SERVER_VERSION, SERVER_PORT,
                    SERVER_PORT);
            myServer.addConnectionListener(this);
            myServer.addMessageListener(new MessageListener<HostedConnection>() {
                @Override
                public void messageReceived(HostedConnection hc, Message m) {
                    LoginMessage lm = (LoginMessage) m;
                    log.debug("Message Received: {}", lm);
                    validatePassword(lm);
                    if (lm.isAccepted()) {
                        connLogin.put(hc.getId(), lm.getUsername());
                    }
                    log.debug("Sending Message: {}", lm);
                    Filter<HostedConnection> f = Filters.equalTo(hc);
                    for (Entry<String, Vector3f> e : userLoc.entrySet()) {
                        myServer.broadcast(f, new LocationMessage(e.getKey(), e.getValue()));
                    }
                    myServer.broadcast(f, lm);
                }
            }, LoginMessage.class);
            myServer.addMessageListener(new MessageListener<HostedConnection>() {
                @Override
                public void messageReceived(HostedConnection hc, Message m) {
                    LocationMessage lm = (LocationMessage) m;
                    log.debug("LocationMessage received: {}", lm);
                    userLoc.put(lm.getUsername(), lm.getCurrentLocation());
                    myServer.broadcast(Filters.notEqualTo(hc), lm);
                }

            }, LocationMessage.class);
            myServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Error while starting up server.", e);
        }
    }

    public void validatePassword(LoginMessage lm) {
        if (logins.get(lm.getUsername()) == null) {
            logins.put(lm.getUsername(), lm.getPassword());
            log.debug("Storing credentials for username: {}", lm.getUsername());
            lm.setAccepted(true);
        } else {
            if (lm.getPassword().equals(logins.get(lm.getUsername()))) {
                lm.setAccepted(true);
            } else {
                lm.setAccepted(false);
            }
        }
    }

    @Override
    public void connectionAdded(Server s, HostedConnection conn) {
        log.debug("Connection Added from {} with id {}", conn.getAddress(), conn.getId());
    }

    @Override
    public void connectionRemoved(Server s, HostedConnection conn) {
        log.debug("Connection Removed by {}", conn.getId());
        connLogin.remove(conn.getId());
    }

}
