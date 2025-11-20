package examples;

import net.lleida.json.sender.Sender;
import net.lleida.json.sender.Status;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class send_scheduled_sms {

    public static void main(String[] args) {
        try {
            Sender sender = new Sender(Config.USERNAME, Config.APIKEY);
            sender.setLogger(Config.RESOURCES_PATH + "logger.log");

            Random r = new Random();

            String id = Integer.toString(Math.abs(r.nextInt()));
            JsonArray dst = Json.createArrayBuilder().add(Config.RECIPIENT).build();

            // We'll send it in 5 minutes.
            long timestamp = System.currentTimeMillis();
            long schedule = timestamp + 60L * 5L * 1000L;

            String dateNow = new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date(timestamp)),
                    dateSched = new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date(schedule));

            String text = "Scheduled SMS at " + dateNow + " to be sent at " + dateSched;
            JsonObject options = Json.createObjectBuilder()
                    .add("schedule", dateSched)
                    .build();

            boolean queued = sender.scheduledSMS(id, dst, text, options);

            if (sender.errno != 0) {
                System.err.println(sender.errno + ":" + sender.error);
                System.err.println("");
            }

            if (queued) {
                Status status = sender.getStatusScheduled(id);
                System.out.println("ID: " + id);
                System.out.println("Status: " + status.getKey());
                System.out.println("Status: " + status.getCode() + " => " + status.getDescription());
            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }

}
