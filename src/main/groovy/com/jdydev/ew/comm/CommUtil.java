package com.jdydev.ew.comm;

import com.jme3.network.serializing.Serializer;

public class CommUtil {

    public static void registerMessages() {
        Serializer.registerClass(LoginMessage.class);
    }
}
