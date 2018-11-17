package xyz.farhanfarooqui.JRocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * ClientReceiver handles new Socket connections
 * it accepts a connection, and creates a Communicator for that.
 */

class ClientReceiver extends Thread {
    private ServerSocket serverSocket;
    private JRocketServer rocketServer;
    private ExecutorService executorService;

    ClientReceiver(JRocketServer rocketServer, ServerSocket serverSocket, ExecutorService executorService) {
        this.serverSocket = serverSocket;
        this.rocketServer = rocketServer;
        this.executorService = executorService;
    }

    @Override
    public void run() {
        super.run();
        while (!serverSocket.isClosed()) {
            try {
                long t1 = System.currentTimeMillis();
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(rocketServer.getHeartBeatRate());
                Client client = Client.createClient(Utils.createID(), rocketServer, socket, executorService);
                System.out.println(System.currentTimeMillis() - t1);
                rocketServer.onConnect(client);
            } catch (IOException e) {
                e.printStackTrace();
                rocketServer.onServerStop();
            }
        }
    }
}
