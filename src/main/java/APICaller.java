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
        DormID.put("GreyCliff",2980918);
        DormID.put("Mods",2980920);
        DormID.put("Voute",2980923);
    }
    private static HttpURLConnection conn;
    private File all_washer_info;

    private File set_info(String name) {
        this.all_washer_info = new File(name);
        return all_washer_info;
    }

    /**
     *
     * @param url, a fixed url, but included here for concatenation purposes
     * @param dorms, this is a dorm that contains the ID and the name, which is used to determine the ID to attach to the website
     * @param dormname This is the actual dorm that we want to scrape on
     * @return
     * @throws MalformedURLException
     */
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
     * @param mapper This is collected in the main method that is designed to help make the transform of the data
     * @param jsonData The string that contains all the information from the page, source,
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
        /**
         * The rest of the code is dedicated to get the relevant information, such as the scraping time, the dorm name
         * how many washers are available, and if there are available washing machines, then we will ignore the last field
         * If not, we will put an extra field, telling the user when is the closet washing machine be ready
         */
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
        return String.format("%02d:%02d",ldt.getHour(),ldt.getMinute());
    }

    /**
     *
     * @param name This is the name of the file that we are trying to write on
     * @param all_dorm_laundry_info, this is the arraylist that conatins all the dorm information, which every single
     *                               dorm is stored as an array list, so this is a list of list
     * @throws IOException   Probably need to deal with this, but for simplicity, we will ignore this writing
     */
    private void writeCSV(File name, ArrayList<ArrayList<String>> all_dorm_laundry_info) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter(name));
        for(ArrayList<String> single_dorm : all_dorm_laundry_info) {
            writer.writeNext(single_dorm.toArray((new String[0])));
        }
        writer.close();
    }

    private ArrayList<ArrayList<String>> extractCSV(File filename) throws IOException {
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        String testRow;
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        /**Read data as long as it's not empty
        Parse the data by comma using .split() method
         Place into a temporary array, then add to List**/
        while ((testRow = reader.readLine()) != null) {
            String[] line = testRow.split(",");
            data.add(new ArrayList<>(Arrays.asList(line)));
        }
        return data;
    }

    /**
     * This is a big method that combines everything we have done in the helper function, that returns a list of strings
     * that contains the dorm name information, per building, which is what will be called over and over again,
     *
     */
    private ArrayList<String> get_laundry_info_building(URL url, Map dorms, String dormname) throws IOException {
        url = combine_URL(url,dorms,dormname);
        String jsonData = connection(url);
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> data = getValue(mapper,jsonData);
        ArrayList<String> dataParsed = parseData(data);
        // Converts the arraylist to a list of hashmaps
        ArrayList<Map<String, String>> finalData = generateFinalData(dataParsed);
        String info = "time_left_lite";
        ArrayList<String> AvailableInfo = getAvailability(finalData,"W",info,dormname);
        return AvailableInfo;
    }

    /**
     * The laundry information is dynamic, so it will be necessary to scrape at a frequent time interval
     * which is currently set to 10 minutes
     * @param ttl refers to time to live, meaning how valid the result is before we have to rescrape
     */
    private void rescrape(int ttl) {

    }
    public static void main(String[] args) throws IOException {
        APICaller useAPI = new APICaller() ;
        // This is the link for voute, 66 says its unavailable so I commented out the line below
        URL url = new URL("https://www.laundryview.com/api/currentRoomData?school_desc_key=12&location=");

        ArrayList<ArrayList<String>> master_info = new ArrayList<>();


        String[] dormInfo = {"GreyCliff","Mods","Gabelli", "Voute"};
        for(int i = 0; i < dormInfo.length; i++) {
            ArrayList<String> AvailableInfo = new ArrayList<>();
            AvailableInfo = useAPI.get_laundry_info_building(url,DormID,dormInfo[i]);
            System.out.println(AvailableInfo);
            master_info.add(AvailableInfo);
        }

        File all_washer_info = useAPI.set_info("DormInfo.csv");
        useAPI.writeCSV(all_washer_info,master_info);

        ArrayList<ArrayList<String>> dormInfos = useAPI.extractCSV(all_washer_info);
        System.out.println(dormInfos.get(0));







    }


    }

