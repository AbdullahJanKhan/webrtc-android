# How the App Works?
A detailed description is addressed in the following article [Maximizing WebRTC Potential](https://medium.com/@abdullah-jan-khan/maximizing-webrtc-potential-unleashing-power-in-android-apps-with-stun-turn-servers-ice-79c17b010bd8)

# Calling App using Web RTC and Java

This project covers an implementation of Web RTC in Java to establish an app to app calling Mechanism.

![App to App Calling](https://i.ibb.co/xM2qsyW/apptoappcalling.png)
You will get a UI like attached above.  

## How to run the app?
To start the application you will need to start the Signalling server. A signalling server helps in sharing the ICE candidates between the peers [it must be over  secure medium, i.e. wss], it is build using WebSockets or anyother related library like Socket.io.
You can use the signalling server from [this repo](https://github.com/AbdullahJanKhan/webrtc-signalling-server). 
Here I implemened a simple websocket based Node Js server that helps in establishing connection between peers.

### Starting the Signalling Server
You need to run the following commands for first time execution to install the required packages.
`yarn or npm install`
Then you need to start the server using
`yarn start or npm start`

Now you have the signalling server up and running on port `:3000` you can edit this port from the index.js file in the repository.

# How to add it to the App?

When you open the app code base you will see a SocketRepository class here we have implemented a connection to the **Signalling Server** 
```java
// Here in the place of new URI("") => need to add 
// your connection string in place of the ""
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
```

* * *
## How can you get a Secure Connection link to your local Signalling server?

You can use tools like [ngrok](https://ngrok.com/) to get a secure like to your local server.
To run the server with ngrok, start the signalling server and run the following commands:
`ngrok http 3000`
This command will give you a secure link to you local server.

```
ngrok                                                                                                                                                                                        (Ctrl+C to quit)
                                                                                                                                                                                                             
Send your ngrok traffic logs to Datadog: https://ngrok.com/blog-post/datadog-logs                                                                                                                            
                                                                                                                                                                                                             
Session Status                online                                                                                                                                                                         
Account                       username@mail.com (Plan: Free)                                                                                                                                   
Version                       3.3.1                                                                                                                                                                          
Region                        India (in)                                                                                                                                                                     
Latency                       -                                                                                                                                                                              
Web Interface                 http://127.0.0.1:4040                                                                                                                                                          
Forwarding                    https://c76d-58-65-176-42.ngrok-free.app -> http://localhost:3000                                                                                                              
                                                                                                                                                                                                             
Connections                   ttl     opn     rt1     rt5     p50     p90                                                                                                                                    
                              0       0       0.00    0.00    0.00    0.00  
```

The link `https://c76d-58-65-176-42.ngrok-free.app` should be added as a URI in the SocketRepository class, as discussed earlier.
* * *

