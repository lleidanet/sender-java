package examples;

import net.lleida.json.sender.Sender;
import net.lleida.json.sender.Status;
import java.io.IOException;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class send_mms {
    
    public static void main(String[] args) {
        try{
            Sender sender = new Sender(Config.USERNAME, Config.PASSWORD);
            sender.setLogger(Config.RESOURCES_PATH + "logger.log");
            
            Random r = new Random();

            String id = Integer.toString(Math.abs(r.nextInt()));
            JsonArray dst = Json.createArrayBuilder().add(Config.RECIPIENT).build();
            String text = "Texto de ejemplo!!";
            String subject = "Subject MMS through Java API";
            JsonObject attachment = Json.createObjectBuilder()
                .add("mime", "image/png")
                .add("content", sender.getFileContentBase64(Config.RESOURCES_PATH + "logo.png"))
                .build();
            // options is optional. We don't need it for this example.
            // JsonObject options = Json.createObjectBuilder().build();

            boolean queued = sender.mms(id, dst, text, subject, attachment /*, options */);

            if(sender.errno != 0){
                System.out.println(sender.errno + ":" + sender.error);
                System.out.println("");
            }
            
            if(queued){
                Status status = sender.getStatusMMS(id);
                System.out.println("ID: " + id);
                System.out.println("Status: " + status.getKey());
                System.out.println("Status: " + status.getCode() + " => " + status.getDescription());
            }
        }catch(IllegalArgumentException | IOException ex){
            System.err.println(ex.toString());
        }
    }
    
}
