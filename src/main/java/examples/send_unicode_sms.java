package examples;

import net.lleida.json.sender.Sender;
import net.lleida.json.sender.Status;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class send_unicode_sms {
    
    public static void main(String[] args) {
        try{
            Sender sender = new Sender(Config.USERNAME, Config.PASSWORD);
            sender.setLogger(Config.RESOURCES_PATH + "logger.log");

            Random r = new Random();

            String id = Integer.toString(Math.abs(r.nextInt()));
            JsonArray dst = Json.createArrayBuilder().add(Config.RECIPIENT).build();
            String dateNow = new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date(System.currentTimeMillis()));
            String text = "Unicode SMS sent at " + dateNow + " àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸ €";
            JsonObject options = Json.createObjectBuilder()
                .add("unicode", "1")
                .build();

            boolean queued = sender.registeredSMS(id, dst, text, options);

            if(sender.errno != 0){
                System.err.println(sender.errno + ":" + sender.error);
                System.err.println("");
            }
            
            if(queued){
                Status status = sender.getStatusSMS(id);
                System.out.println("ID: " + id);
                System.out.println("Status: " + status.getKey());
                System.out.println("Status: " + status.getCode() + " => " + status.getDescription());
            }
        }catch(Exception ex){
            System.err.println(ex.toString());
        }
    }
    
}
