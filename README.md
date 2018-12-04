# JRocket 

`JRocket` is an event-driven Java Socket library. Execute events in Server and Clients and send data payloads with it. You can use it in Android too.

## Features

* **Event-driven. Send events with data payloads between server and client**
* **Asynchronous.**
* **Heartbeat mechanism. Server and client will be able to detect if one of them is disconnected**

## Download

Download JRocket.jar from <a href="https://github.com/frhnfrq/JRocket/releases/download/v1.0/JRocket.jar">here.</a>

## How to use

### Server

Use the `JRocketServer.listen(int port, int coreThreadPoolSize)` method to start listening for connections. It'll return a `JRocketServer` object.
Here, `coreThreadPoolSize` refers to the core size of the underlying thread pool. This library lets you handle the size of the thread pool. You can change the thread pool size in runtime by calling
`setCoreThreadPoolSize(int coreThreadPoolSize)` method.
Note that, you must call `setMaxThreadPoolSize(int maxThreadPoolSize)` after calling `listen(int port, int coreThreadPoolSize)` if you want to change the 
core thread pool size.
Start listening for events by calling `onReceive(String event, OnReceiveListener onReceiveListener)` method, and send events to clients by calling `send(String event, JSONObject data)` on `Client` objects.

See the example section for more.

### Client

Use the `JRocketClient.prepare(String host, int port, RocketClientListener rocketClientListener)` to prepare the client. It'll return a `JRocketClient` object. Then call `connect()` method to connect to the server.
Start listening for events by calling `onReceive(String event, OnReceiveListener onReceiveListener)` method, and send events to server by calling `send(String event, JSONObject data)` on `JRocketClient` object.

See the example section for more.

#### Example (Server)

```java
public static void main(String[] args) {

        try {
            // Start listening to port 1234 and set core thread pool size to 1000. Each client requires 2 threads, so we'll be handling 500 clients at a time.
            JRocketServer rocketServer = JRocketServer.listen(1234, 1000);
            // Client will send and wait for a hearbeat every 3000 milliseconds.
            rocketServer.setHeartBeatRate(3000);


            rocketServer.setOnClientConnectListener(new OnClientConnectListener() {
                @Override
                public void onClientConnect(Client client) {
                    System.out.println("New client connected. ID: " + client.getId());
                }
            });

            rocketServer.setOnClientDisconnectListener(new OnClientDisconnectListener() {
                @Override
                public void onClientDisconnect(Client client) {
                    System.out.println("Client disconnected. ID: " + client.getId());
                }
            });

            // Start listening for the event named client_name.
            rocketServer.onReceive("client_name", new OnReceiveListener() {
                @Override
                public void onReceive(JSONObject data, Client client) {
                    System.out.println("Client " + client.getId() + " send its name. Name : " + data.getString("name"));
                    // Store the name in the client object.
                    client.put("name", data.getString("name"));
                    JSONObject responseData = new JSONObject();
                    responseData.put("response", "Hey, " + data.getString("name") + "!");
                    // Send an event to the client with a data payload
                    client.send("greeting", responseData);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
```

#### Example (Client)

```java
public static void main(String[] args) {
        // Prepare the JRocketClient to connect to the server hosted at 127.0.0.1 on port 1234. 
        JRocketClient rocketClient = JRocketClient.prepare("127.0.0.1", 1234, new JRocketClient.RocketClientListener() {
            @Override
            public void onConnect(JRocketClient rocketClient) {
                System.out.println("Connected to the server");
                JSONObject data = new JSONObject();
                data.put("name", "Farhan");
                // Send an event named client_name to the server with data payload.
                rocketClient.send("client_name", data);
            }

            @Override
            public void onConnectFailed(JRocketClient rocketClient) {
                System.out.println("Failed to connect");
            }

            @Override
            public void onDisconnect(JRocketClient rocketClient) {
                System.out.println("Disconnected");
            }
        });

        // Client will send and wait for a heartbeat every 3000 milliseconds. This must be called before calling connect()
        rocketClient.setHeartBeatRate(3000);
        rocketClient.connect();

        // Start listening for the event named greeting.
        rocketClient.onReceive("greeting", new OnReceiveListener() {
            @Override
            public void onReceive(JSONObject jsonObject) {
                System.out.println(jsonObject.getString("response"));
            }
        });
    }
```


License
=======

     Copyright 2018 Farhan Farooqui
     
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
