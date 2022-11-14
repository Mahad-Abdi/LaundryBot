import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.*;
import static spark.Spark.*;
import java.util.HashMap;


public class LaundryBot {
    public static final String ACCOUNT_SID = "ACb58307342cb8bc69695700b6f9c97bd0";
    public static final String AUTH_TOKEN = "aa944a0427841259e70e6463bc3a1237";
    public static HashMap<String, String> usersResidentialHall = new HashMap<>();

    public static void main(String[] args) {
        String bodyMessage = replyToSMS("Hello. Welcome to LaundryBot. What residential hall do you live in?");
        }

    /*public static void sendSMS() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        for(int i = 0; i < 10; i++) {
            Message message = Message.creator(
                            new com.twilio.type.PhoneNumber("+16176526846"),
                            new com.twilio.type.PhoneNumber("+14782495460"),
                            "This is the ship that made the Kessel Run in fourteen parsecs?")
                    .create();
            System.out.println(message.getSid());
        }
    } */

    public static String replyToSMS(String message)  {
        final String[] requestBody = {""};
            post("/receive-sms", (req, res) -> {
                requestBody[0] = String.valueOf(req.body());
                System.out.println("Body " + requestBody[0]);
                res.type("application/xml");
                storeResidentialHall(req.body());
                Body body = new Body
                        .Builder(message)
                        .build();
                Message sms = new Message
                        .Builder()
                        .body(body)
                        .build();
                MessagingResponse twiml = new MessagingResponse
                        .Builder()
                        .message(sms)
                        .build();
                return twiml.toXml();
            });
            return requestBody[0];
    }
    public static String[] parseRequestBody(String body) {
        return body.split("&");
    }

    public static String[] parseNumber(String number) {
        return number.split("B");
    }

    public static String[] parseResidentialHall(String hall) {
        return hall.split("B");
    }

    public static void storeResidentialHall(String bodyMessage) {
        String[] parsedRequest = parseRequestBody(bodyMessage);
        String phoneNumber = parseNumber(parsedRequest[18])[1];
        String parsedPhoneNumber = phoneNumber.substring(0,phoneNumber.length()-1);
        String residentialHall = parsedRequest[10];
        String parsedResidentialHall = residentialHall.substring(0,residentialHall.length()-1);
        usersResidentialHall.put(parsedPhoneNumber,parsedResidentialHall);
    }
}
