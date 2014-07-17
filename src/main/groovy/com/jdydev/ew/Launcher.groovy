package com.jdydev.ew

import static groovyx.javafx.GroovyFX.start

import com.jdydev.ew.client.EWClient

def ewclient = new EWClient()
start {
    actions {
        fxaction(id: 'loginAction', name: 'Login', onAction: {
            if (ewclient.authenticate(username.text, password.text)) {
                stage.close()
                ewclient.showSettings = false
                ewclient.start()
            }
        })
    }
    stage(title: "Endless Wilderness Login", visible: true, id: 'stage') {
        scene(width: 500, height: 250) {
            vbox(padding: 50, style: '-fx-background-color: black; -fx-alignment: center; -fx-spacing: 20') {
                text(text: 'Endless Wilderness', font: '36pt sansserif') { fill green }
                textField(id: 'username', style: '-fx-alignment: center; -fx-font-size: 18')
                passwordField(id: 'password', style: '-fx-alignment: center; -fx-font-size: 18')
                button(id: 'loginButton', loginAction)
            }
        }
    }
}