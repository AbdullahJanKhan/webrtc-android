package com.faras.callingapp;

import android.util.Log;

import com.faras.callingapp.models.MessageModels;
import com.faras.callingapp.utils.NewMessageInterface;
import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class SocketRepository implements NewMessageInterface {

    private WebSocketClient webSocketClient;
    private String userName;
    private final String logTag = "SocketRepository";

    public SocketRepository(String username) {
        this.userName = username;

        // if you are using android emulator your local websocket address is going to be "ws://10.0.2.2:3000"
        // if you are using your phone as emulator your local address, use cmd and then write ipconfig
        // and get your ethernet ipv4 , mine is : "ws://192.168.1.25:3000"
        // but if your websocket is deployed you add your websocket address here
        try {
            this.webSocketClient = new WebSocketClient(new URI("")) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    sendMessageToSocket(
                            new MessageModels(
                                    "store_user", username, null, null
                            )
                    );
                }

                @Override
                public void onMessage(String message) {
                    try {
                        onNewMessage(new Gson().fromJson(message, MessageModels.class));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(logTag, "onClose: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    Log.d(logTag, "onError: " + ex.toString());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect(){
        webSocketClient.close();
    }

    public void sendMessageToSocket(MessageModels message) {
        try {
            if (webSocketClient.getConnection() != null) {
                Log.d(logTag, "sendMessageToSocket: " + message);
                webSocketClient.send(new Gson().toJson(message));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewMessage(MessageModels message) {

    }
}
