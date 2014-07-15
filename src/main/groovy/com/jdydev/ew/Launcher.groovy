package com.jdydev.ew

import groovy.swing.SwingBuilder

import java.awt.Color
import java.awt.Font

import javax.swing.JFrame
import javax.swing.JPasswordField
import javax.swing.JTextField

import com.jdydev.ew.client.EWClient

JTextField username
JPasswordField password
def ewclient = new EWClient()

new SwingBuilder().edt {
    frame(title: "Endless Wilderness Login", size: [300, 250], defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, show: true) {
        tableLayout(cellpadding: 10, background: Color.BLACK) {
            tr {
                td(align: 'center', colspan: 2) {
                    label(text: 'Endless Wilderness', foreground: Color.WHITE, font: new Font(Font.SANS_SERIF, Font.PLAIN, 24)) 
                }
            }
            tr {
                td(align: 'center') {
                    label(text: 'Username:', foreground: Color.WHITE)
                }
                td(align: 'center') {
                    username = textField(preferredSize: [100, 20])
                }
            }
            tr {
                td(align: 'center') {
                    label(text: 'Password:', foreground: Color.WHITE)
                }
                td(align: 'center') {
                    password = passwordField(preferredSize: [100, 20])
                }
            }
            tr {
                td(align: 'center') {
                    button(text: "Login", actionPerformed: { 
                        if (ewclient.authenticate(username.text, password.text)) {
                            dispose()
                        }
                        })
                }
                td(align: 'center') {
                    button(text: "Cancel", actionPerformed: { dispose() })
                }
            }
        }
    }
}
