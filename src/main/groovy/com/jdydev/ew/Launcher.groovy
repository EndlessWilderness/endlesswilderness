package com.jdydev.ew

import static groovyx.javafx.GroovyFX.start
import javafx.application.Platform

import com.jdydev.ew.client.EWClient
import com.jdydev.ew.comm.LoginMessage

public class Launcher {

    def ewclient = new EWClient()
    def myStage
    def username
    def password
    def login

    public static void main(String[] args) {
        new Launcher()
    }

    public Launcher() {
        start {
            actions {
                fxaction(id: 'loginAction', name: 'Login', onAction: {
                    username.disable = true
                    password.disable = true
                    loginButton.disable = true
                    ewclient.setLauncher(this)
                    ewclient.authenticate(new LoginMessage(username: username.text, password: password.text))
                })
            }
            myStage = stage(title: "Endless Wilderness Login", visible: true, id: 'stage') {
                scene(width: 500, height: 300) {
                    vbox(padding: 50, style: '-fx-background-color: black; -fx-alignment: center; -fx-spacing: 20') {
                        text(text: 'Endless Wilderness', font: '36pt sansserif') { fill green }
                        username = textField(id: 'username', style: '-fx-alignment: center; -fx-font-size: 18')
                        password = passwordField(id: 'password', style: '-fx-alignment: center; -fx-font-size: 18')
                        login = button(id: 'loginButton', loginAction)
                        text(text: 'Warning: This Login is Insecure') { fill yellow }
                    }
                }
            }
        }
    }

    def loginFailure() {
        Platform.runLater(new Runnable() {
                    public void run() {
                        username.disable = false
                        password.disable = false
                        password.text = ''
                        login.disable = false
                    }
                })
    }

    def loginSuccess() {
        Platform.runLater(new Runnable() {
                    public void run() {
                        myStage.close()
                        ewclient.showSettings = false
                        ewclient.start()
                    }
                })
    }
}