package xyz.farhanfarooqui.JRocket;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;

public class Converter {

    public static JSONObject convertObjectToJSON(Object object) {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T convertJSONToObject(JSONObject jsonObject, Class<T> type) {
        Gson gson = new Gson();
        String json = jsonObject.toString();
        if (json.isEmpty()) {
            return null;
        } else {
            return gson.fromJson(json, type);
        }
    }
}
