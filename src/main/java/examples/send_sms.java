package examples;

import net.lleida.json.sender.Sender;
import net.lleida.json.sender.Status;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonArray;

public class send_sms {

    public static void main(String[] args) {
        try {
            Sender sender = new Sender(Config.USERNAME, Config.APIKEY);
            sender.setLogger(Config.RESOURCES_PATH + "logger.log");

            Random r = new Random();

            String id = Integer.toString(Math.abs(r.nextInt()));
            JsonArray dst = Json.createArrayBuilder().add(Config.RECIPIENT).build();
            String text = "This is an SMS for testing purposes!";
            // options is optional. We don't need it for this example.
            // javax.json.JsonObject options = javax.json.Json.createObjectBuilder().build();

            boolean queued = sender.sms(id, dst, text /* , options */);

            if (sender.errno != 0) {
                System.err.println(sender.errno + ":" + sender.error);
                System.err.println("");
            }

            if (queued) {
                Status status = sender.getStatusSMS(id);
                System.out.println("ID: " + id);
                System.out.println("Status: " + status.getKey());
                System.out.println("Status: " + status.getCode() + " => " + status.getDescription());
            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }

}
