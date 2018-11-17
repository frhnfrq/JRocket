package xyz.farhanfarooqui.JRocket;

import org.json.JSONObject;
import xyz.farhanfarooqui.JRocket.ClientListeners.OnReceiveListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JRocketClient implements JRocket {
    private static JRocketClient mRocketClient;
    private static Socket mSocket;
    private static HashMap<String, OnReceiveListener> mEventLists;
    private static Communicator mCommunicator;
    private static boolean mKeepAlive = false;
    private static ExecutorService mExecutorService;
    private RocketClientListener mRocketClientListener;
    private int mHeartBeatRate = 0;
    private String mHost;
    private int mPort;

    private boolean disconnected = false;

    {
        mEventLists = new HashMap<>();
    }

    private static Client.ClientListener clientListener = new Client.ClientListener() {
        @Override
        public void onEventReceive(JRocket JRocket, String event, JSONObject data) {
            ((JRocketClient) JRocket).onReceiveEvent(event, data);
        }

        @Override
        public void onClientDisconnect(JRocket JRocket) {
            ((JRocketClient) JRocket).onDisconnect();
        }
    };

    private JRocketClient(String host, int port) {
        this(host, port, null);
    }

    private JRocketClient(String host, int port, RocketClientListener rocketClientListener) {
        this.mHost = host;
        this.mPort = port;
        this.mRocketClientListener = rocketClientListener;
        mExecutorService = Executors.newFixedThreadPool(2);
    }


    /**
     * Connects the client to the server. It does its operations on a separate thread to avoid blocking the
     * main thread. Call {@link #setHeartBeatRate(int)} method before connecting.
     */
    public void connect() {
        Thread thread = new Thread(new ConnectRunnable());
        thread.start();
    }

    private class ConnectRunnable implements Runnable {
        @Override
        public void run() {
            try {
                mSocket = new Socket(mHost, mPort);
                mSocket.setSoTimeout(getHeartBeatRate());
                mCommunicator = new Communicator(mRocketClient, mSocket, mExecutorService);
                mCommunicator.setClientListener(clientListener);
                mCommunicator.start();
                disconnected = false;
                mRocketClient.onConnect();
            } catch (IOException e) {
                e.printStackTrace();
                onConnectFailed();
            }
        }
    }

    /**
     * Prepares the client to connect to the host.
     * Call {@link #connect()} method to connect to the host
     */
    public static JRocketClient prepare(String host, int port) {
        mRocketClient = new JRocketClient(host, port);
        return mRocketClient;
    }

    /**
     * Prepares the client to connect to the host.
     * Call {@link #connect()} method to connect to the host
     */
    public static JRocketClient prepare(String host, int port, RocketClientListener rocketClientListener) {
        mRocketClient = new JRocketClient(host, port, rocketClientListener);
        return mRocketClient;
    }

    /**
     * Called when the connection is successfully established
     */
    private void onConnect() {
        if (mRocketClientListener != null)
            mRocketClientListener.onConnect(mRocketClient);
    }

    /**
     * Called if failed to connect to the server, either because of network error or the server is not listening
     */
    private void onConnectFailed() {
        if (mRocketClientListener != null)
            mRocketClientListener.onConnectFailed(mRocketClient);
    }

    /**
     * Disconnects the client from the server
     */
    public void disconnect() {
        mCommunicator.close();
    }

    /**
     * Called when the client is disconnected from the server
     */
    private void onDisconnect() {
        if (mRocketClientListener != null && !disconnected) {
            disconnected = true;
            mRocketClientListener.onDisconnect(mRocketClient);
        }
    }

    /**
     * Send data to the server
     */
    public boolean send(String event, JSONObject data) {
        if (isConnected()) {
            mCommunicator.send(event, data);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create an event receive listener
     */
    public void onReceive(String event, OnReceiveListener onReceiveListener) {
        mEventLists.put(event, onReceiveListener);
    }

    /**
     * Called when the client receives an event
     */
    private void onReceiveEvent(String event, JSONObject data) {
        if (mEventLists.containsKey(event)) {
            mEventLists.get(event).onReceive(data);
        }
    }


    /**
     * Returns true if the client has ever connected to the server
     */
    public boolean hasConnected() {
        return mSocket.isConnected();
    }

    /**
     * Returns true if the client is currently connected to the server
     */
    public boolean isConnected() {
        return mSocket != null && !mSocket.isClosed();
    }

    /**
     * Set the option for Keep-Alive
     */
    public void setKeepAlive(boolean on) throws SocketException {
        this.mKeepAlive = on;
        mSocket.setKeepAlive(on);
    }

    /**
     * Returns true if Keep-Alive is turned on
     */
    public boolean getKeepAlive() throws SocketException {
        return mSocket.getKeepAlive();
    }

    /**
     * Returns the Local InetAddress of the server
     */
    public InetAddress getLocalAddress() {
        return mSocket.getLocalAddress();
    }

    /**
     * Returns the port number of the server
     */
    public int getPort() {
        return mSocket.getPort();
    }

    /**
     * Returns the InetAddress of the server
     */
    @Override
    public InetAddress getInetAddress() {
        return mSocket.getInetAddress();
    }

    /**
     * Returns true if the socket is currently closed
     */
    @Override
    public boolean hasDisconnected() {
        return mSocket.isClosed();
    }

    /**
     * Get the heartbeat rate in milliseconds
     */
    public int getHeartBeatRate() {
        return mHeartBeatRate;
    }

    /**
     * Set the heartbeat rate in milliseconds. Client will send a heartbeat to the server in n milliseconds. Must be called before {@link #connect()} method is called.
     */
    public void setHeartBeatRate(int milliseconds) {
        this.mHeartBeatRate = milliseconds;
    }

    /**
     * Ignored on client side
     */
    @Override
    public void setCoreThreadPoolSize(int coreThreadPoolSize) {

    }

    /**
     * Ignored on client side
     */
    @Override
    public void setMaxThreadPoolSize(int maxThreadPoolSize) {

    }

    /**
     * Set the timeout period
     */

    @Override
    public int getLocalPort() {
        return mSocket.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return mSocket.getLocalSocketAddress();
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return mSocket.getSoTimeout();
    }

    public interface RocketClientListener {
        void onConnect(JRocketClient rocketClient);

        void onConnectFailed(JRocketClient rocketClient);

        void onDisconnect(JRocketClient rocketClient);
    }


}
