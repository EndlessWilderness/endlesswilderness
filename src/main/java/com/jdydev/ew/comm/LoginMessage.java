package com.jdydev.ew.comm;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

@Serializable
public class LoginMessage extends AbstractMessage {

    private String username;
    private String password;
    private boolean accepted;

    public LoginMessage() {
        // Empty Constructor
    }

    public LoginMessage(String u, String p) {
        this.username = u;
        this.password = p;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Username: ");
        sb.append(this.getUsername());
        sb.append(", Password: ");
        // Mask Password with flag?
        sb.append(this.getPassword());
        sb.append(", Accepted: ");
        sb.append(this.isAccepted());
        return sb.toString();
    }
}
