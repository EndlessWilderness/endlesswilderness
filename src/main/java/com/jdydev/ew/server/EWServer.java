package com.jdydev.ew.server;

import java.io.IOException;

import com.jme3.app.SimpleApplication;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.system.JmeContext;

public class EWServer extends SimpleApplication {

    public static void main(String[] args) {
        EWServer app = new EWServer();
        app.start(JmeContext.Type.Headless);
    }

    @Override
    public void simpleInitApp() {
        try {
            Server myServer = Network.createServer(6143);
            myServer.start();
        } catch (IOException e) {
            throw new RuntimeException("Error while starting up server.", e);
        }
    }

}
