import static groovyx.javafx.GroovyFX.start

import com.jdydev.ew.client.EWClient

def ewclient = new EWClient()
start {
    actions {
        fxaction(id: 'cancelAction', name: 'Cancel', onAction: { stage.close() })
        fxaction(id: 'loginAction', name: 'Login', onAction: {
            username.disable = true
            password.disable = true
            loginButton.disable = true
            // I want to dump this call to another thread
            if (ewclient.authenticate(username.text, password.text)) {
                stage.close()
                ewclient.showSettings = false
                ewclient.start()
            } else {
                println 'Authentication Failed'
                username.disable = false
                password.disable = false
                loginButton.disable = false
            }
        })
    }
    stage(title: "Endless Wilderness Login", visible: true, id: 'stage') {
        scene(width: 500, height: 250) {
            vbox(padding: 50, style: '-fx-background-color: black; -fx-alignment: center; -fx-spacing: 20') {
                text(text: 'Endless Wilderness', font: '36pt sansserif') { fill green }
                textField(id: 'username', style: '-fx-alignment: center; -fx-font-size: 18')
                passwordField(id: 'password', style: '-fx-alignment: center; -fx-font-size: 18')
                hbox(style: '-fx-alignment: center; -fx-spacing: 20') {
                    button(id: 'loginButton', loginAction)
                    button(id: 'cancelButton', cancelAction)
                }
            }
        }
    }
}