package xyz.farhanfarooqui.JRocket.ServerListeners;

import org.json.JSONObject;
import xyz.farhanfarooqui.JRocket.Client;

public interface OnReceiveListener {
    void onReceive(JSONObject data, Client client);
}
