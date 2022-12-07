import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class APICaller {
    public static void main(String[] args) throws IOException {
        // Split two blocks of code into two functions
        // Gets the data from the api for one laundry room, you probably want to replace that with a string
        URL url = new URL("https://www.laundryview.com/api/currentRoomData?school_desc_key=12&location=298095&rdm=1670386001696");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String jsonData = reader.readLine();
        conn.disconnect();
        // Converting json string to list of hashmaps, now data is list of hashmaps, each hashmap is data for one laundry machine so you can data.get(0).get("time_left_lite") to get the availability
        // used the following sources - https://stackoverflow.com/questions/44698437/map-json-to-listmapstring-object
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        List<Map<String, Object>> data = mapper.readValue(jsonData, new TypeReference<>(){});



    }


    }

