import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;
import java.time.*;
import java.io.IOException;
import java.util.HashMap;
public class Info {
    private Instant currentTime;
    private int numWasher;
    private int numDryer;
    private Instant available;

    public void setWasher(int numWasher) {
        this.numWasher = numWasher;
    }

    public int getNumWasher() {
        return numWasher;
    }

    public void setNumDryer(int numDryer) {
        this.numDryer = numDryer;
    }

    public int getNumDryer() {
        return numDryer;
    }
}

