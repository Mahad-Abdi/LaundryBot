import com.twilio.Twilio;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.time.ZonedDateTime;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class LaundryBot {
    public static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String TWILIO_PHONE_NUMBER = "+14782495460";
    public static HashMap<String, String> usersResidentialHall = new HashMap<>();
    public static ArrayList<ArrayList<String>> csvData = new ArrayList<>();
    public static HashMap<String, List<String>> machineData = new HashMap<>();
    public static String minutes = "";
    public static boolean reminderOffered = false;
    public static Boolean reminderRequested = false;


    static HashSet<String> availableHalls = new HashSet<>();
    public static final String INTRODUCTION_MESSAGE = "Hello. Welcome to the LaundryBot. What residential hall do you live in? The LaundryBot currently works for the following residential halls." + "\n" + "GreyCliff, Mods, Gabelli, Voute";
    public static final String OPTIONS_MESSAGE = "You can ask the LaundryBot " + "\n" + "1)If washing machines are available / when they will become an available "  + "\n" + "2) To set a reminder for when a washing machine becomes available "  + "\n" + "3) Set a reminder when your laundry is done  ";

    public static void main(String[] args) throws IOException, InterruptedException {
        availableHalls.add("greycliff"); availableHalls.add("voute"); availableHalls.add("mods"); availableHalls.add("gabelli");
        availableHalls.add("fitzpatrick"); availableHalls.add("williams"); availableHalls.add("stayer"); availableHalls.add("walsh");
        availableHalls.add("2150");
        APICaller caller = new APICaller();
        csvData = extractCSV();
        machineData = convertToMap(csvData);
//        caller.extractData();
        combine();

    }

    // Combines all of the necessary functions to receive, handle, and send messages
    public static void combine() throws IOException, InterruptedException {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        // Create a Date object to store the time of the last message check
        ZonedDateTime lastMessageCheck = ZonedDateTime.now();
        ZonedDateTime mostRecentDate = null;
        long updateTimeCheck = System.currentTimeMillis();

        // Run the chat bot indefinitely
        APICaller caller = new APICaller();
        while (true) {
            //  At a regular interval update the dorm room date
            if(updateTimeCheck >= (updateTimeCheck + (10 * 60 * 1000))) {
                caller.extractData();
                csvData = extractCSV();
                machineData = convertToMap(csvData);
                updateTimeCheck = System.currentTimeMillis();
            }

            // Check for new messages
            ResourceSet<Message> messages =  Message.reader()
                    .setDateSentAfter(lastMessageCheck)
                    .read();


            // Check if 11 minutes has gone by

            // Iterate over the list of messages
            for (Message message : messages) {
                String fromPhoneNumber = String.valueOf(message.getFrom());
                String messageBody = message.getBody().toLowerCase();
                int requestType = determineMessageType(messageBody);
                if(!fromPhoneNumber.equals("+14782495460"))
                    handleMessage(messageBody, fromPhoneNumber, requestType);
                if(requestType == 4 || requestType == 5) {
                    String replyMessage = handleMessage(messageBody, fromPhoneNumber, requestType);
                    sendMessage(replyMessage,fromPhoneNumber);
                    String delayedMessage = generateReminderMessage(requestType);
                    if(requestType == 4) {
                        sendScheduledMessage(delayedMessage, fromPhoneNumber, Integer.valueOf(minutes));
                    } else  {
                        sendScheduledMessage(delayedMessage, fromPhoneNumber, 33);
                    }
                }else {
                    String replyMessage = handleMessage(messageBody, fromPhoneNumber, requestType);
                    sendMessage(replyMessage,fromPhoneNumber);
                }

//                sendScheduledMessage(replyMessage,fromPhoneNumber);
//                sendMessage(replyMessage, phoneNumber);


                // Update the time of the last message check
                if(message.getDateSent() != null) {
                    mostRecentDate = message.getDateSent().plus(3, ChronoUnit.SECONDS);
                }
            }
            // Update the date for the next message reader so that only new messages will be read
            if(mostRecentDate != null)
                lastMessageCheck = mostRecentDate;


        }
    }

    // Converts csv file into a list
    private static ArrayList<ArrayList<String>> extractCSV() throws IOException {
        ArrayList<ArrayList<String>> data = new ArrayList<>();
        String testRow;
        BufferedReader reader = new BufferedReader(new FileReader("DormInfo.csv"));
        /**Read data as long as it's not empty
         Parse the data by comma using .split() method
         Place into a temporary array, then add to List**/
        while ((testRow = reader.readLine()) != null) {
            String[] line = testRow.split(",");
            data.add(new ArrayList<>(Arrays.asList(line)));
        }
        return data;
    }
    // Determines message type using keywords
    public static int determineMessageType(String message) {
        //
        message = message.toLowerCase();
        if(message.contains("hall") || availableHalls.contains(message)) {
            return 2;
        }else if (message.contains("when") || message.contains("washing+machine") || message.contains("available?") || message.contains("is")) {
            return 3;
        }else if(message.contains("yes") && reminderOffered) {
            reminderRequested = true;
            reminderOffered = false;
            return 4;
        } else if (message.contains("timer") || message.contains("started")) {
            return 5;
        }

        return 1;
    }

    // Generated a response message based on the message type
    public static String handleMessage(String messageBody, String phoneNumber, int requestType) {
        String responseMessage = "";
        // If it is a new user ask them their hall
        if(!usersResidentialHall.containsKey(phoneNumber) && !messageBody.contains("hall") && !availableHalls.contains(messageBody.toLowerCase())){
            responseMessage = INTRODUCTION_MESSAGE;
        }
        else {
            // Send introduction message if it is new user
            if(requestType == 1 && !usersResidentialHall.containsKey(phoneNumber)) {
                responseMessage = INTRODUCTION_MESSAGE;
                // Store the phone number and residential hall in hashmap if the response contains the word hall
            } else if(requestType == 2) {
                responseMessage = OPTIONS_MESSAGE;
                if(messageBody.contains(" ")) {
                    String[] strarray = messageBody.split(" ");
                    String hall = strarray[0];
                    usersResidentialHall.put(phoneNumber,hall.toLowerCase());
                } else {
                    usersResidentialHall.put(phoneNumber,messageBody);

                }

                // This is where we have to add logic to check the availability of the washing machine
            } else if(requestType == 3){
                responseMessage = checkAvailablity(usersResidentialHall.get(phoneNumber));
            } else if(requestType == 4) {
                // This is where we have to add the logic to check the availability of the washing machine
                responseMessage = "You will receive a reminder when a washing machine is available.";
            } else if(requestType == 5) {
                responseMessage = "You will receive a reminder when your clothes are done.";
            }
                else {
                // Send functions of the bot in response
                responseMessage = OPTIONS_MESSAGE;
            }
        }
        return responseMessage;

    }
    // Generates the type of delayed message
    public static String generateReminderMessage(int requestType) {
        if(requestType == 4){
            return "A washing machine is now available!";
        }
        System.out.println("request type 5 is returned");
        return "Your laundry is done";
    }
// Sends message using Twilio Message creator
    public static void sendMessage(String message, String phoneNumber) {
        if(!phoneNumber.equals(TWILIO_PHONE_NUMBER)) {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            Message responseMessage = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(TWILIO_PHONE_NUMBER),
                    message
            ).create();
        }
    }

// SChedules a message to be sent (minutes) into the future
    public static void sendScheduledMessage(String message, String phoneNumber, int minutes) {
        final ZonedDateTime time = ZonedDateTime.now().plus(minutes,ChronoUnit.MINUTES);
        System.out.println("the scheduled time " + time);
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message responseMessage = Message.creator(

                new PhoneNumber(phoneNumber),
                "MG257f14c5e835f375d38a6f4e25347bbf",
                message
        ).setSendAt(time).setScheduleType(Message.ScheduleType.FIXED).create();

    }
// Converts the list representation of the csv into a hashmap
    public static HashMap<String, List<String>> convertToMap(List<ArrayList<String>> csvData) {
        HashMap<String, List<String>> hmap = new HashMap<>();
        for(ArrayList<String> ls: csvData) {
            String key = ls.get(1);
            key = key.substring(1,key.length()-1).toLowerCase();
            ArrayList<String> values = new ArrayList<String>(ls.subList(2, ls.size()));
            for(int i = 0; i < values.size(); i++) {
                String element = values.get(i);
                element = element.substring(1,element.length()-1);
                values.set(i, element);
            }
            hmap.put(key,values);
        }
        for (String key : hmap.keySet()) {
            List<String> value = hmap.get(key);
            System.out.println(key + ": " + value);
        }
        return hmap;
    }
// Using the hashmap checks the availability of washing machines in a residentiall hall
    public static String checkAvailablity(String hall) {
        String returnMessage = "";
            // Checks if the hall from the list is the same as input hall
            if(machineData.containsKey(hall)) {
                ArrayList<String> values = (ArrayList<String>) machineData.get(hall);
                String numberOfMachines = values.get(0);
                int countMachines = Integer.valueOf(numberOfMachines);
                if(countMachines > 0){
                    returnMessage = "There are " + countMachines + " washers available.";
                }
                else {
                    String minutesLeft = values.get(1);
                    reminderOffered = true;
                     returnMessage = "A washing machine will be available in " + minutes + " minutes " + "\n" + "Would you like to set a reminder?";
                }
            } else {
                returnMessage = "Your residential hall is not currently supported";
            }

        return returnMessage;
    }


}


