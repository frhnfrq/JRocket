package xyz.farhanfarooqui.JRocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;

interface JRocket {
    InetAddress getInetAddress();

    boolean hasDisconnected();

    int getLocalPort();

    SocketAddress getLocalSocketAddress();

    int getSoTimeout() throws IOException;

    void setHeartBeatRate(int heartBeatRate);

    int getHeartBeatRate();

    void setCoreThreadPoolSize(int coreThreadPoolSize);

    void setMaxThreadPoolSize(int maxThreadPoolSize);
}
