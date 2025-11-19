package examples;

import net.lleida.json.sender.Sender;
import net.lleida.json.sender.Status;
import java.io.IOException;

public class get_status_sms {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
            Sender sender = new Sender(Config.USERNAME, Config.APIKEY);
            sender.setLogger(Config.RESOURCES_PATH + "logger.log");

            String id = ""; /* <- Your SMS identifier here */
            Status status = sender.getStatusSMS(id);
            System.out.println("ID: " + id);
            System.out.println("Status: " + status.getKey());
            System.out.println("Status: " + status.getCode() + " => " + status.getDescription());
        }catch(IllegalArgumentException | IOException ex){
            System.err.println(ex.toString());
        }
    }

}
