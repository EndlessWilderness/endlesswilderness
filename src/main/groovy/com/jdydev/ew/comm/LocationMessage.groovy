package com.jdydev.ew.comm

import com.jme3.math.Vector3f
import com.jme3.network.AbstractMessage
import com.jme3.network.serializing.Serializable

@Serializable
class LocationMessage extends AbstractMessage {

    Vector3f currentLocation
    String username;
    
    def LocationMessage() {
        // nuttin
    }
    
    def LocationMessage(String name, Vector3f locIn) {
        username = name
        currentLocation = locIn
    }
    
    String toString() {
        "Username: $username, Current location: $currentLocation"
    }
}
