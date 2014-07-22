package com.jdydev.ew.comm

import com.jme3.math.Vector3f
import com.jme3.network.AbstractMessage
import com.jme3.network.serializing.Serializable

@Serializable
class LocationMessage extends AbstractMessage {

    Vector3f currentLocation
    
    def LocationMessage() {
        // nuttin
    }
    
    def LocationMessage(Vector3f locIn) {
        currentLocation = locIn
    }
    
    String toString() {
        "Current location: $currentLocation"
    }
}
