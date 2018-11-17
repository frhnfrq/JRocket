package xyz.farhanfarooqui.JRocket;

import org.json.JSONObject;
import xyz.farhanfarooqui.JRocket.ServerListeners.OnClientConnectListener;
import xyz.farhanfarooqui.JRocket.ServerListeners.OnClientDisconnectListener;
import xyz.farhanfarooqui.JRocket.ServerListeners.OnReceiveListener;
import xyz.farhanfarooqui.JRocket.ServerListeners.OnServerStopListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class JRocketServer implements JRocket {

    private static JRocketServer mRocketServer;
    private static ServerSocket mServerSocket;
    private static volatile ArrayList<Client> mClients;
    private static HashMap<String, OnReceiveListener> mEventLists;
    private static ExecutorService mExecutorService;
    private int heartBeatRate = 0;

    // Listeners

    private OnServerStopListener mOnServerStopListener;
    private OnClientConnectListener mOnClientConnectListener;
    private OnClientDisconnectListener mOnClientDisconnectListener;

    private JRocketServer(int coreThreadPoolSize) {
        mClients = new ArrayList<>();
        mEventLists = new HashMap<>();
        mExecutorService = Executors.newFixedThreadPool(coreThreadPoolSize);
    }

    public static JRocketServer listen(int port, int coreThreadPoolSize) throws IOException {
        if (mRocketServer == null) {
            mRocketServer = new JRocketServer(coreThreadPoolSize);
            mServerSocket = new ServerSocket(port);
            ClientReceiver clientReceiver = new ClientReceiver(mRocketServer, mServerSocket, mExecutorService);
            clientReceiver.start();
        }
        return mRocketServer;
    }

    /**
     * Sets the size of core thread pool. One client will require two threads, so set this accordingly.
     *
     * @param coreThreadPoolSize Should always be an even number. Odd numbers will be incremented and number less than 2 will be ignored and the core thread pool
     *                           size will be set to 2.
     */
    @Override
    public void setCoreThreadPoolSize(int coreThreadPoolSize) {
        if (coreThreadPoolSize < 2) {
            coreThreadPoolSize = 2;
        }
        if (coreThreadPoolSize % 2 != 0) {
            coreThreadPoolSize++;
        }
        ((ThreadPoolExecutor) mExecutorService).setCorePoolSize(coreThreadPoolSize);
    }

    /**
     * Sets the max size of thread pool. One client will require two threads, so set this accordingly.
     *
     * @param maxThreadPoolSize Should always be an even number. Odd numbers will be incremented and number less than 2 will be ignored and the max size of
     *                          thread pool will be set to 2.
     */
    @Override
    public void setMaxThreadPoolSize(int maxThreadPoolSize) {
        if (maxThreadPoolSize < 2) {
            maxThreadPoolSize = 2;
        }
        if (maxThreadPoolSize % 2 != 0) {
            maxThreadPoolSize++;
        }
        ((ThreadPoolExecutor) mExecutorService).setMaximumPoolSize(maxThreadPoolSize);
    }


    /**
     * Stops the server and disconnects all of the clients
     */
    public void stop() throws IOException {
        mServerSocket.close();
        disconnectClients();
    }

    /**
     * Disconnects a client
     */
    public void disconnect(Client client) {
        client.disconnect();
    }

    private void disconnectClients() {
        for (Client client : mClients) {
            client.disconnect();
        }
    }

    /**
     * Sets an onReceive listener
     */
    public void onReceive(String event, OnReceiveListener onReceiveListener) {
        mEventLists.put(event, onReceiveListener);
    }

    /**
     * Sends an event with data
     */
    public void send(String event, JSONObject data) {
        for (Client client : mClients) {
            client.send(event, data);
        }
    }

    /**
     * Returns an ArrayList containing current clients
     */
    public ArrayList<Client> clients() {
        return mClients;
    }


    /**
     * This method broadcasts to all client except the client from whom the event was fired
     */

    void broadCast(String event, JSONObject data, Client client) {
        for (Client c : mClients) {
            if (!c.equals(client)) {
                c.send(event, data);
            }
        }
    }

    /**
     * This method calls the specific event which was fired from the client
     */

    void onReceiveEvent(String event, JSONObject data, Client client) {
        if (mEventLists.containsKey(event)) {
            mEventLists.get(event).onReceive(data, client);
        }
    }

    /**
     * This method is called when a client connects.
     */

    void onConnect(Client client) {
        if (mOnClientConnectListener != null)
            mOnClientConnectListener.onClientConnect(client);
        addClient(client);
    }

    /**
     * This method is called when a client disconnects.
     */

    void onDisconnect(Client client) {
        if (mOnClientDisconnectListener != null && removeClient(client))
            mOnClientDisconnectListener.onClientDisconnect(client);
    }


    /**
     * This method is called when server stops
     */

    void onServerStop() {
        if (mOnServerStopListener != null) {
            mOnServerStopListener.onServerStop();
        }
    }


    public void setOnServerStopListener(OnServerStopListener onServerStopListener) {
        this.mOnServerStopListener = onServerStopListener;
    }

    public void setOnClientConnectListener(OnClientConnectListener onClientConnectListener) {
        this.mOnClientConnectListener = onClientConnectListener;
    }

    public void setOnClientDisconnectListener(OnClientDisconnectListener onClientDisconnectListener) {
        this.mOnClientDisconnectListener = onClientDisconnectListener;
    }

    /**
     * Adds the client to the list of all clients
     */

    private void addClient(Client client) {
        mClients.add(client);
    }

    /**
     * Removed the client from the list of all clients when the client disconnects
     */
    private boolean removeClient(Client client) {
        return mClients.remove(client);
    }

    /**
     * Get the heartbeat rate in milliseconds
     */
    public int getHeartBeatRate() {
        return heartBeatRate;
    }

    /**
     * Set the heartbeat rate in milliseconds. Server will send a heartbeat to the client in n milliseconds
     */
    public void setHeartBeatRate(int milliseconds) {
        this.heartBeatRate = milliseconds;
    }

    @Override
    public InetAddress getInetAddress() {
        return mServerSocket.getInetAddress();
    }

    @Override
    public boolean hasDisconnected() {
        return mServerSocket.isClosed();
    }

    /**
     * If server doesn't receive any client request within t milliseconds it'll throw a SocketTimeoutException.
     * t must be > 0. A timeout of zero is interpreted as an infinite timeout.
     */
    public void setClientReceiveTimeout(int t) throws SocketException {
        mServerSocket.setSoTimeout(t);
    }

    @Override
    public int getLocalPort() {
        return mServerSocket.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return mServerSocket.getLocalSocketAddress();
    }

    @Override
    public int getSoTimeout() throws IOException {
        return mServerSocket.getSoTimeout();
    }
}
