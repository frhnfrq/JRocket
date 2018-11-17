package xyz.farhanfarooqui.JRocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

import static xyz.farhanfarooqui.JRocket.Constants.*;

/**
 * Communicator class handles all the I/O between the client
 * and the Server.
 */

class Communicator {
    private JRocket mJRocket;
    private Socket mSocket;
    private Receiver mReceiver;
    private Sender mSender;
    private boolean hasRun = false;
    private Client.ClientListener mClientListener;
    private ExecutorService mExecutorService;
    private volatile boolean running;
    private LinkedBlockingQueue<JSONObject> mQueue;

    void setClientListener(Client.ClientListener clientListener) {
        this.mClientListener = clientListener;
    }

    Communicator(JRocket JRocket, Socket socket, ExecutorService executorService) throws IOException {
        mJRocket = JRocket;
        mSocket = socket;
        mExecutorService = executorService;
        mReceiver = new Receiver(socket);
        mSender = new Sender(socket);
        mQueue = new LinkedBlockingQueue<>();
    }

    private JRocket getJRocket() {
        return mJRocket;
    }

    void start() {
        if (!hasRun) {
            try {
                mExecutorService.execute(mSender);
                mExecutorService.execute(mReceiver);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
                disconnect();
            }
            this.running = true;
            hasRun = true;
        }
    }

    void close() {
        disconnect();
    }

    /**
     * Broadcasts to other clients
     * <br>
     * PS. broadcasts to <b>all of the clients except the client who's broadcasting</b>.
     */

    void broadCast(String event, JSONObject data, Client client) {
        ((JRocketServer) getJRocket()).broadCast(event, data, client);
    }

    /**
     * Sends data
     */
    void send(String event, JSONObject data) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(EVENT, event);
            jsonObject.put(DATA, data);
            mQueue.put(jsonObject);
        } catch (JSONException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class Sender implements Runnable {

        private Socket socket;
        private OutputStreamWriter outputStreamWriter;

        Sender(Socket socket) throws IOException {
            this.socket = socket;
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        }

        @Override
        public void run() {
            while (!socket.isClosed()) {
                try {
                    JSONObject jsonObject = mQueue.poll(mJRocket.getHeartBeatRate(), TimeUnit.MILLISECONDS);

                    if (jsonObject == null) {
                        send("heartbeat", new JSONObject());
                        continue;
                    }

                    outputStreamWriter.write(jsonObject.toString().length());
                    outputStreamWriter.write(jsonObject.toString());
                    outputStreamWriter.flush();
                } catch (IOException e) {
                    try {
                        outputStreamWriter.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            disconnect();
        }
    }

    private class Receiver implements Runnable {
        private Socket socket;
        private BufferedReader bufferedReader;

        Receiver(Socket socket) throws IOException {
            this.socket = socket;
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            StringBuilder stringBuilder = new StringBuilder();
            int retry = 0;

            while (!socket.isClosed()) {
                try {

                    if (retry > 2) {
                        throw new IOException("Maximum retries reached");
                    }

                    int c;
                    char ch;
                    int length = bufferedReader.read();
                    for (int i = 0; i < length; i++) {
                        c = bufferedReader.read();
                        ch = (char) c;
                        stringBuilder.append(ch);
                    }

                    JSONObject jsonObject = new JSONObject(stringBuilder.toString());
                    String event = jsonObject.getString(EVENT);
                    JSONObject data = jsonObject.getJSONObject(DATA);

                    mClientListener.onEventReceive(getJRocket(), event, data);
                    stringBuilder.setLength(0);
                    retry = 0;
                } catch (SocketTimeoutException s) {
                    retry++;
                    System.out.println("Time out on read. Trying again  " + retry);
                } catch (IOException e) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                    break;
                } catch (JSONException e) {
                    e.printStackTrace();
                    break;
                }
            }
            disconnect();
        }
    }

    private void disconnect() {
        if (running) {
            if (!mSocket.isClosed()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mClientListener.onClientDisconnect(getJRocket());
            running = false;
        }
    }
}
