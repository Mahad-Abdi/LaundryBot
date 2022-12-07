import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class APICaller {
    /**
     *  The website has information contains the dorm name and an associated ID on the API that we are calling to\
     *  And the ID is constant, so we only need to employ the name correctly;
     */
    private static HashMap<String, Integer> DormID;
    static {
        DormID = new HashMap<String, Integer>();
        DormID.put("66",2980914);
        DormID.put("2nd_floor_2k",29809029);
        DormID.put("Gabelli",2980917);
    }

    private URL combine_URL(URL url, Map dorms, String dormname) throws MalformedURLException {
        String newURL = url.toString() + dorms.getOrDefault(dormname,"");
        return new URL(newURL);
    }

    public static void main(String[] args) throws IOException {
        // Split two blocks of code into two functions
        // Gets the data from the api for one laundry room, you probably want to replace that with a string
        APICaller useAPI = new APICaller() ;
        URL url = new URL("https://www.laundryview.com/api/currentRoomData?school_desc_key=12&location=");
        url = useAPI.combine_URL(url,DormID,"66");

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
        System.out.println("The data itself has a type of " + data.getClass());
        System.out.println("The data[0] has the size of " + data.get(0).size());
        System.out.println("The data[0] has the " + data.get(0).entrySet());
        System.out.println("The data[0] get objects has a type of" + data.get(0).get("objects").size());
        System.out.println(data);
        System.out.println(data.get(0).get("objects"));
        String saver = data.get(0).get("objects").toString();
        System.out.println(saver.length());
        String[] value = saver.split("[ | { | } | ]");
        System.out.println(value.length);
        for(int i = 3; i < 18; i++) {
            System.out.println(value[i]);
        }



    }


    }

