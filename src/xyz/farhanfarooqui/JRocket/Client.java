package xyz.farhanfarooqui.JRocket;

import com.sun.istack.internal.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

/**
 * Represents each client connected to the server. Communicate with the client through an instance of this class. A new client object will be created if the client reconnects
 * after disconnecting.
 */
public class Client {
    private String mId;
    private Communicator mCommunicator;
    private HashMap<String, Object> mDatas;

    private Client(String id, Communicator communicator) {
        mId = id;
        mCommunicator = communicator;
        mDatas = new HashMap<>();
    }

    /**
     * Creates a client instance for the client which just connected to the server.
     *
     * @param id              Unique Id for each client.
     * @param rocketServer    Instance of {@link JRocketServer} with which the client is connected to the server.
     * @param socket          Instance of {@link Socket} over which the client is connected to the server.
     * @param executorService All thread operations are performed on this executor service.
     */
    static Client createClient(String id, JRocketServer rocketServer, Socket socket, ExecutorService executorService) throws IOException {
        Communicator communicator = new Communicator(rocketServer, socket, executorService);
        Client client = new Client(id, communicator);

        client.mCommunicator.setClientListener(new ClientListener() {
            @Override
            public void onEventReceive(JRocket JRocket, String event, JSONObject data) {
                ((JRocketServer) JRocket).onReceiveEvent(event, data, client);
            }

            @Override
            public void onClientDisconnect(JRocket JRocket) {
                ((JRocketServer) JRocket).onDisconnect(client);
            }
        });

        client.mCommunicator.start();

        return client;
    }

    /**
     * @return Unique Id of the client.
     */
    public String getId() {
        return mId;
    }

    /**
     * Store client datas with unique keys.
     *
     * @param key  key with which the specified data is to be associated.
     * @param data data to be associated with the specified key.
     */
    public void put(String key, Object data) {
        mDatas.put(key, data);
    }

    /**
     * Returns data associated with the key.
     *
     * @param key The Key with which the data is stored.
     * @return The data with which the specified key is stored.
     */
    public Object get(String key) {
        return mDatas.get(key);
    }

    /**
     * Sends an event to the client with payload.
     *
     * @param event The event which will be sent to the client.
     * @param data  The data payload which will be sent to the client. Payloads must be stored in JSON format.
     */
    public void send(@NotNull String event, @NotNull JSONObject data) {
        mCommunicator.send(event, data);
    }

    /**
     * Event is broadcasted to every client except the calling client
     *
     * @param event The event which will be sent to the clients.
     * @param data  The data payload which will be sent to the clients. Payloads must be stored in JSON format.
     */
    public void broadCast(@NotNull String event, @NotNull JSONObject data) {
        mCommunicator.broadCast(event, data, this);
    }

    /**
     * Disconnects the client from the server.
     */
    void disconnect() {
        mCommunicator.close();
    }

    interface ClientListener {
        void onEventReceive(JRocket JRocket, String event, JSONObject data);

        void onClientDisconnect(JRocket JRocket);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Client && ((Client) obj).getId().equals(getId());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 37 * hashCode + mId.hashCode();
        return hashCode;
    }
}
