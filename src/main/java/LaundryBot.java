import com.twilio.Twilio;

import java.time.ZoneId;
import java.time.ZonedDateTime;
// added with the chatbot

import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;


public class LaundryBot {
    public static final String ACCOUNT_SID = "ACb58307342cb8bc69695700b6f9c97bd0";
    public static final String AUTH_TOKEN = "aa944a0427841259e70e6463bc3a1237";
    private static final String TWILIO_PHONE_NUMBER = "+14782495460";
    public static HashMap<String, String> usersResidentialHall = new HashMap<>();
    public static final String INTRODUCTION_MESSAGE = "Hello. Welcome to LaundryBot. What residential hall do you live in? The LaundryBot currently works for the following residential halls. Walsh Voute";
    public static final String OPTIONS_MESSAGE = "You can ask the laundry bot 1)If the washing machines are available / when they will become an available 2) To set a reminder for when a washing machine becomes available 3) Set a reminder when your laundry is done  ";

    public static void main(String[] args) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        // Create a Date object to store the time of the last message check
        ZonedDateTime lastMessageCheck = ZonedDateTime.now();
        ZonedDateTime mostRecentDate = null;

        // Run the chat bot indefinitely
        while (true) {

            // Check for new messages
            ResourceSet<Message> messages =  Message.reader()
                    .setDateSentAfter(lastMessageCheck)
                    .read();

            // Iterate over the list of messages
            for (Message message : messages) {
                String fromPhoneNumber = String.valueOf(message.getFrom());
                if(!fromPhoneNumber.equals("+14782495460"))
                handleMessage(message);
                String replyMessage = handleMessage(message);
                sendScheduledMessage(replyMessage,fromPhoneNumber);
//                sendMessage(replyMessage, phoneNumber);


                // Update the time of the last message check
                if(message.getDateSent() != null) {
                    mostRecentDate = message.getDateSent().plus(3, ChronoUnit.SECONDS);
                }
            }
            // Update the date for the next message reader so that only new messages will be read
            if(mostRecentDate != null)
                lastMessageCheck = mostRecentDate;

           //  Sleep for a short period of time before checking for new messages again
//            try {
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                // Do nothing
//            }

        }


    }

    public static String handleMessage(Message message) {
        String responseMessage = "";
        String phoneNumber = String.valueOf(message.getFrom());
        String messageBody = message.getBody().toLowerCase();
        // If it is a new user ask them their hall
        if(!usersResidentialHall.containsKey(phoneNumber) && !messageBody.contains("hall")){
            responseMessage = INTRODUCTION_MESSAGE;
        }
        else {
            int requestType = determineMessageType(messageBody);
            // Send introduction message if it is new user
            if(requestType == 1 && !usersResidentialHall.containsKey(phoneNumber)) {
                responseMessage = INTRODUCTION_MESSAGE;
                // Store the phone number and residential hall in hashmap if the response contains the word hall
            } else if(requestType == 2) {
                responseMessage = OPTIONS_MESSAGE;
                usersResidentialHall.put(phoneNumber,messageBody);
                // This is where we have to add logic to check the availability of the washing machine
            } else if(requestType == 3){
                responseMessage = "The washing machine is available right now.";
            } else if(requestType == 4) {
                // This is where we have to add the logic to check the availability of the washing machine
                responseMessage = "A timer has been set for.";
            } else {
                // Send functions of the bot in response
                responseMessage = OPTIONS_MESSAGE;
            }
        }
        return responseMessage;

    }
    public static int determineMessageType(String message) {
        //
        message = message.toLowerCase();
        if(message.contains("hall")) {
            return 2;
        }else if (message.toLowerCase().contains("when") || message.contains("washing+machine") || message.contains("available")) {
            return 3;
        } else if (message.contains("timer") || message.contains("started")) {
            return 4;
        }
        return 1;
    }

    public static void sendMessage(String message, String phoneNumber) {
        System.out.println("the number we are sending to " + phoneNumber);
        System.out.println("the number we are sending from " + TWILIO_PHONE_NUMBER);
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message responseMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(TWILIO_PHONE_NUMBER),
                message
        ).create();
    }


    public static void sendScheduledMessage(String message, String phoneNumber) {
        ZonedDateTime time = ZonedDateTime.now().plus(15,ChronoUnit.MINUTES);
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        System.out.println("phone number " + phoneNumber);
        Message responseMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                "MG257f14c5e835f375d38a6f4e25347bbf",
                message
        ).setSendAt(time).setScheduleType(Message.ScheduleType.FIXED).create();


    }


}


