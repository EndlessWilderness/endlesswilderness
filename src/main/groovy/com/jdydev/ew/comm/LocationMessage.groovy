package com.jdydev.ew.comm

import com.jme3.math.Vector3f
import com.jme3.network.AbstractMessage
import com.jme3.network.serializing.Serializable

@Serializable
class LocationMessage extends AbstractMessage {

    Vector3f currentLocation
    Vector3f viewDirection
    Vector3f walkDirection
    String username

    def LocationMessage() {
        // nuttin
    }

    def LocationMessage(String name, Vector3f locIn) {
        username = name
        currentLocation = locIn
        viewDirection = new Vector3f(0, 0, 1)
        walkDirection = new Vector3f(0, 0, 0)
    }

    def LocationMessage(String name, Vector3f locIn, Vector3f view, Vector3f walk) {
        username = name
        currentLocation = locIn
        viewDirection = view
        walkDirection = walk
    }

    String toString() {
        "Username: $username, Current location: $currentLocation, Look Direction: $viewDirection, Move Direction: $walkDirection"
    }
}
