package JsonUtility;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonHandler {

    public JSONArray getAttributeAsJsonArray(Object o) {
        JSONArray saver = (JSONArray) o;
        return saver;
    }

    public JSONObject getAttributeAsJsonObject(Object o) {
        JSONObject saver = (JSONObject) o;
        return saver;
    }
}
