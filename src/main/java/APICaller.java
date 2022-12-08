import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.*;
import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.lang.*;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;


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
    private File all_washer_info;

    private File set_info(String name) {
        this.all_washer_info = new File(name);
        return all_washer_info;
    }

    private URL combine_URL(URL url, Map dorms, String dormname) throws MalformedURLException {
        String newURL = url.toString() + dorms.getOrDefault(dormname,"");
        return new URL(newURL);
    }

    /**
     *  Start creating other additional functions, 1: to make the main file shorter
     *  2: csv creation and reading csv files, i.e to convert to lists of lists and extract info from lists of lists
     *  This is actually used to establish the connection to the scrapper website and find all the relevant information for the data, that is stored the json format
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

    /**
     * This is the method that is used to actually read whatever that is presented on the website we are scraping from, and it is formatted as a String
     * @param mapper
     * @param jsonData
     * @return
     * @throws JsonProcessingException
     */
    private List<Map<String,Object>> getValue(ObjectMapper mapper, String jsonData) throws JsonProcessingException {
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        List<Map<String, Object>> data = mapper.readValue(jsonData, new TypeReference<>(){});
        return data;
    }

    /**
     * The data that contains all the information that we need, but it is not very helpful, as it is not formatted properly
     * The goal of this function, is simply to seperate a data in a nicer way, as per machine infromation is contained in the {} block, which is
     * what this function is doing
     * @param data
     * @return
     */
    private ArrayList<String> parseData(List<Map<String,Object>> data) {
        String object = data.get(0).get("objects").toString();
        object = object.substring(1, object.length()-1);
        ArrayList<String> dataParsed = new ArrayList<>();
        dataParsed = getContent(object);
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

    /**
     * This is the method that is used to get an ArrayList of HashMap by splitting the input string
     * @param parsed
     * @return
     */
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

    /**
     * This is the method that uses too get the availability for both washer and the drier, the second dynamic string type is used to determine either washer or dryer
     * @param finalData
     * @param type
     * @return
     */
    private ArrayList<String> getAvailability(ArrayList<Map<String, String>> finalData, String type, String info, String dormname) throws IOException{
        ArrayList<String> availabilityinfo = new ArrayList<String>();
       int currentMachine = 0;
        int closetMinute = 0;
        int numNotWorking = 0;
        var current = Instant.now();   //&& (map.get("appliance type:").equals(type))
        for(Map<String, String> map: finalData) {
            if (map.get(info) != null) { //Because some of the scraper code will return null, so need to check to get this waived
                if (map.get(info).equals("Available")) {
                    currentMachine++;
                } else if (map.get(info).equals("Out of Service")) {
                    numNotWorking++;
                } else {//Then the rest of the machine, is occupied, with a time limit
                    String[] characters = map.get("time_left_lite").split("\\s+"); //We get the actual characters by splitting
                    if (characters[1].equals("min")) { //This is guranteed to be in format of xx min remaining
                        if (Integer.parseInt(characters[0]) >= closetMinute && closetMinute == 0) {
                            closetMinute = Integer.parseInt(characters[0]);
                        } else if (Integer.parseInt(characters[0]) <= closetMinute) {
                            closetMinute = Integer.parseInt(characters[0]);
                        }
                    }
                }
            }
            current = Instant.now();
            if (currentMachine == 0) { //This means that the currentNum Washer is unavailable, so must loop through
                current = current.plus(closetMinute, ChronoUnit.MINUTES);
            }
        }
        availabilityinfo.add(formatting(current));
        availabilityinfo.add(dormname);
        availabilityinfo.add(String.valueOf(currentMachine));
        if(currentMachine != 0) {
            availabilityinfo.add("");
        } else {
            availabilityinfo.add(closetMinute + " minute to the first washing machine");
        }
        return availabilityinfo;
    }

    /**
     * To standardize the time format, as the standrdize the time, not the actual
     * @param  time parameter is an instant object,
     * @return The hour: minute format, in EST time zone
     */
    private String formatting(Instant time) {
        ZoneId EST = ZoneId.of("US/Eastern");
        ZonedDateTime BCTime = ZonedDateTime.ofInstant(time,EST);
        LocalDateTime ldt = LocalDateTime.ofInstant(time, EST);//We do want to get the time in EST
        return ldt.getHour() + ":" + ldt.getMinute();
    }

    /**
     * We are intended to store and make updates to the csv file, but this is an initial version, then the rest will be called repetetively
     * They key difference here is that instead of writing an array into the csv file that is being stored in the array, we are actually,
     * trying to write every entry into the csv file, as an array that contains the dorm information from previous operations
     * @param name, this is used for setting the name purpose
     * @param dorm_laundry_info, this is the list that contains all the informtaion, structured in the way we wanted in our project;
     * @throws IOException, technically should never happen
     */
    private void initialwriteCSV(File name, ArrayList<String> dorm_laundry_info) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(name));
        writer.writeNext(dorm_laundry_info.toArray(new String[0]));
        writer.close();
    }

    /**
     * We are making changes to the csv file by adding additional dorm laundry info to the csv file
     * @param name
     * @param dorm_laundry_info
     * @throws IOException
     */
    private void updateCSV(File name, ArrayList<String> dorm_laundry_info) throws IOException {
        CSVReader reader = new CSVReader(new FileReader("DormInfo.csv"));
        List<String[]> r = reader.readAll(); //Now we have read all previous data.
        CSVWriter writer = new CSVWriter(new FileWriter(name));
        for(String[] column : r) {
            writer.writeNext(column);
        }
        writer.writeNext(dorm_laundry_info.toArray(new String[0]));
        writer.close();
    }

    /**
     * This method is supposed to extract the information of the csv file, which is stored as a list of lists
     * @param dorminfo
     * @return
     * @throws IOException
     */
    private ArrayList<ArrayList<String>> getInfo(File dorminfo) throws IOException {
        CSVReader reader = new CSVReader(new FileReader("DormInfo.csv"));
        List<String[]> values = reader.readAll();
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>;
        for(String[] element : values) {
            result.add((ArrayList<String>) Arrays.asList(element));
        }
        return null;
    }

    private void extractCSV() {

    }

    public static void main(String[] args) throws IOException {
        APICaller useAPI = new APICaller() ;
        // This is the link for voute, 66 says its unavailable so I commented out the line below
        URL url = new URL("https://www.laundryview.com/api/currentRoomData?school_desc_key=12&location=");
        url = useAPI.combine_URL(url,DormID,"Gabelli");
        // used the following sources - https://stackoverflow.com/questions/44698437/map-json-to-listmapstring-object
        String jsonData = useAPI.connection(url);
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> data = useAPI.getValue(mapper,jsonData);
        ArrayList<String> dataParsed = useAPI.parseData(data);
        // Converts the arraylist to a list of hashmaps
        ArrayList<Map<String, String>> finalData =useAPI.generateFinalData(dataParsed);
        String info = "time_left_lite";
        ArrayList<String> AvailableInfo = useAPI.getAvailability(finalData,"W",info,"Gabelli");
        System.out.println(AvailableInfo);
        int count = 1;
        for(Map<String, String> map: finalData) {
            System.out.println("hashmap number: " + count + " appliance type: " + map.get("appliance_type") + " availability/time left " + map.get("time_left_lite"));
            count++;
        }

        File all_washer_info = useAPI.set_info("DormInfo.csv");
        useAPI.initialwriteCSV(all_washer_info,AvailableInfo);







    }


    }

