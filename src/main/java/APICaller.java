import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;

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
        // This is the link for voute, 66 says its unavailable so I commented out the line below
        URL url = new URL("https://www.laundryview.com/api/currentRoomData?school_desc_key=12&location=2980923");
//        url = useAPI.combine_URL(url,DormID,"66");

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
        // Gets the objects from the hashmap
        String object = data.get(0).get("objects").toString();
        object = object.substring(1, object.length()-1);
        // Parses the objects into a list of strings most of which contain information about the washers and dryers by using { } to delineate, some of the information has to do with displaying the web page
        ArrayList<String> dataParsed = new ArrayList<>();
        for(int i = 0; i < object.length(); i ++) {
            int startIndex = 0;
            int endIndex = 0;
            if(object.charAt(i) == '{') {
                startIndex = i+1;
                while(i < object.length()) {
                    if(object.charAt(i) == '}') {
                        endIndex = i;
                        dataParsed.add(object.substring(startIndex,endIndex));
                        break;
                    }
                    i++;
                }
                i -=1;
            }
        }

        // Converts the arraylist to a list of hashmaps
        ArrayList<Map<String, String>> finalData = new ArrayList<Map<String, String>>();
        for(String str: dataParsed) {
            Map<String, String> hmap = Splitter.on(", ")
                    .withKeyValueSeparator("=")
                    .split(str);
            finalData.add(hmap);
        }
        int count = 1;
        /* Now all you have to do is use this hashmap to determine the number of washer's available and the availability of the next washer (using a time representation but in string format)
        if washer's are available then the availability would be zero, add this to a list of lists, and then convert that list of lists to a file, and then convert that file back to a list of lists (which I will use in my code)
        * */
        for(Map<String, String> map: finalData) {
            System.out.println("hashmap number: " + count + " appliance type: " + map.get("appliance_type") + " availability/time left " + map.get("time_left_lite"));
            count++;
        }
        System.out.println(" Now all you have to do is use this hashmap to determine the number of washer's available and the availability of the next washer (using a time representation but in string format)\n" +
                "if washer's are available then the availability would be zero, add this to a list of lists, and then convert that list of lists to a file, \n" +
                " and then convert that file back to a list of lists (which I will use in my code) ");




//        List<String> matchList = new ArrayList<String>();
//        Pattern regex = Pattern.compile("");
//        Matcher regexMatcher = regex.matcher(object);
//
//        while (regexMatcher.find()) {
//            System.out.println("got here 1");
//            matchList.add(regexMatcher.group(1));
//        }
//
//        for(String str:matchList) {
//            System.out.println("got here 2");
//
//            System.out.println(str);
//        }


//        System.out.println("The data[0] has the size of " + data.get(0).size());
//        System.out.println("The data[1] has the size of " + data.get(0).entrySet());
//        System.out.println("The data[0] has the " + data.get(0).entrySet());
//        System.out.println("The data[0] get objects has a type of" + data.get(0).get("objects"));
//        System.out.println(data);
//        System.out.println(data.get(0).get("objects"));
//        String saver = data.get(0).get("objects").toString();
//        System.out.println(saver.length());
//        String[] value = saver.split("[ | { | } | ]");
//        System.out.println(value.length);
//        for(int i = 3; i < 18; i++) {
//            System.out.println(value[i]);
//        }



    }


    }

