import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;
import java.time.*;
import java.io.IOException;
import java.util.HashMap;
public class Scrapper {
    /**
     *  The reason that we choose a static hashmap since the website identifies the dorm
     *  name with an associated ID, and what is better is that by inputting the ID the website can
     *  direct you automatically to the appropriate dorm name without actually need to include the dorm
     *  name in the website
     */
    private static HashMap<String, Integer> DormID;
    Document store;
    private Element washer_info;
    private int number;
    private static String baseWebsite = "https://www.laundryview.com/home/12/";
    /**
     * Static HashMap is created because that the dorm and ID is likely to be the same, and will not change
     */
    static {
        DormID = new HashMap<String, Integer>();
        DormID.put("66",2980914);
        DormID.put("2nd_floor_2k",29809029);
        DormID.put("Gabelli",2980917);
    }

    public Document getStore() {
        return store;
    }


    private String generateWebsite(HashMap dorms, String dormname) {
        String address = baseWebsite;
        address += dorms.getOrDefault(dormname,"");
        return address;
    }

    private void connect() {
        try {//
            store = Jsoup.connect(generateWebsite(DormID,"66")).get();
            System.out.println("The connection was successful, will proceed shortly");

        } catch (IOException e) {
            System.out.println("The connection was unsuccessful, please try again soon");
            System.out.println("Exiting Shortly");
            throw new RuntimeException(e);
        }
    }


    /**
     * The actual scraping information of the website
     */
    private Elements getInfo() {
        Elements washer_info =store.select("washers ng-binding");
        if(washer_info.isEmpty()) {
            System.out.println("There's nothing in this tage");
        } else {
            System.out.println("There's some informtation in this tag");
            System.out.println(String.valueOf(washer_info).length());
        }
        return washer_info;
    }
    //<span class="washers ng-binding">Washers: 7 <span class="avail">avail</span></span>
    /**
     * Goal: Set a TTL for the scraped result,in a csv file, such that when the TTL reaches 0, the entire function will
     * run again
     */
    public static void main(String[] args) {
        Scrapper jsoupScrapper = new Scrapper();
        try {
            jsoupScrapper.connect();
        } catch(RuntimeException e) {
            System.exit(0);
        }
        jsoupScrapper.getInfo();
        System.exit(0);
    }



}


