import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
    private static HttpURLConnection conn;

    private URL combine_URL(URL url, Map dorms, String dormname) throws MalformedURLException {
        String newURL = url.toString() + dorms.getOrDefault(dormname,"");
        return new URL(newURL);
    }

    /**
     *  Start creating other additional functions, 1: to make the main file shorter
     *  2: csv creation and reading csv files, i.e to convert to lists of lists and extract info from lists of lists
     * @param
     * @throws IOException
     */

    private String connection(URL url) throws IOException {
        this.conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String jsonData = reader.readLine();
        conn.disconnect();
        return jsonData;
    }

    private List<Map<String,Object>> getValue(ObjectMapper mapper, String jsonData) throws JsonProcessingException {
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        List<Map<String, Object>> data = mapper.readValue(jsonData, new TypeReference<>(){});
        return data;
    }

    private ArrayList<String> parseData(List<Map<String,Object>> data) {
        String object = data.get(0).get("objects").toString();
        object = object.substring(1, object.length()-1);
        ArrayList<String> dataParsed = new ArrayList<>();
        getContent(object);
        return dataParsed;
    }

    /**
     * The goal of this function is that we are trying to extract the information that is within the '{ '} block
     *  Using substring method, and then add them to our
     * @param object
     * @return
     */
    private ArrayList<String> getContent(String object) {
        ArrayList<String> dataParsed = new ArrayList<String>();
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
        return dataParsed;
    }

    private ArrayList<Map<String, String>> generateFinalData(ArrayList<String> parsed) {
        ArrayList<Map<String, String>> finalData = new ArrayList<Map<String, String>>();
        for(String str: parsed) {
            Map<String, String> hmap = Splitter.on(", ")
                    .withKeyValueSeparator("=")
                    .split(str);
            finalData.add(hmap);
        }
        return finalData;
    }

    private ArrayList<String> getAvailability(ArrayList<Map<String, String>> finalData) {
        ArrayList<String> availabilityinfo = new ArrayList<String>();
        int currentNumWasher = 0;
        int closetMinute = 0;
        int numNotWorking = 0;
        var current = Instant.now();
        for(Map<String, String> map: finalData) {
            if(map.get("time_left_lite").equals("Available")) {
                currentNumWasher ++;
            } else if(map.get("time_left_lite").equals("Out of Service")){
                numNotWorking++;
            } else {
                String[] characters = map.get("time_left_lite").split("\\s+"); //We get the actual characters by splitting
                if(Integer.parseInt(characters[0]) >= closetMinute && closetMinute == 0) {
                    closetMinute = Integer.parseInt(characters[0]);
                } else if(Integer.parseInt(characters[0]) <= closetMinute) {
                    closetMinute = Integer.parseInt(characters[0]);
                }
            }
            current = Instant.now();
            if(currentNumWasher == 0) { //This means that the currentNum Washer is unavailable, so must loop through
                current  = current.plus(closetMinute,ChronoUnit.MINUTES);
            }
        } //Now we have looped through the availability, and we should be able to put them into a single list
        //The first part is to deal with the formatting of the time, we only care about the hour and the minute,
        availabilityinfo.add(formatting(current));
        return availabilityinfo;
    }

    private String formatting(Instant time) {
        ZoneId EST = ZoneId.of("US/Eastern");
        ZonedDateTime BCTime = ZonedDateTime.ofInstant(time,EST);
        LocalDateTime ldt = LocalDateTime.ofInstant(time, EST);//We do want to get the time in EST
        return ldt.getHour() + ":" + ldt.getMinute();
    }

    public static void main(String[] args) throws IOException {
        // Split two blocks of code into two functions
        // Gets the data from the api for one laundry room, you probably want to replace that with a string
        APICaller useAPI = new APICaller() ;
        // This is the link for voute, 66 says its unavailable so I commented out the line below
        URL url = new URL("https://www.laundryview.com/api/currentRoomData?school_desc_key=12&location=");
        url = useAPI.combine_URL(url,DormID,"Gabelli");
        // used the following sources - https://stackoverflow.com/questions/44698437/map-json-to-listmapstring-object
        String jsonData = useAPI.connection(url);
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> data = useAPI.getValue(mapper,jsonData);

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






    }


    }

