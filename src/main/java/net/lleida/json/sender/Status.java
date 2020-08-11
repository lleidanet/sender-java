package net.lleida.json.sender;

import java.util.HashMap;
import java.util.Map;

public class Status {
    private final String key;
    private final int code;
    private final String description;
    
    private static final Map<String, Status> statuses;
    static {
        statuses = new HashMap();
        statuses.put("N", new Status("N", 1, "New. The message has not been processed yet."));
        statuses.put("P", new Status("P", 2, "Pending. The message has not been sent yet."));
        statuses.put("S", new Status("S", 3, "Sent. The message has been sent."));
        statuses.put("D", new Status("D", 4, "Delivered. The message has been delivered to the addressee (mobile phone confirmation has been received). This status is only available if delivery receipt was activated in the sending."));
        statuses.put("B", new Status("B", 5, "Buffered. Message has been sent to operator yet not to addresse. Mobile switched off or out of range. Operator will retry sending until recipient receives sms. This status is only available if delivery receipt was activated in the sending."));
        statuses.put("F", new Status("F", 6, "Failed. The message has not been sent."));
        statuses.put("I", new Status("I", 7, "Invalid. The message is invalid."));
        statuses.put("C", new Status("C", 8, "Cancelled. The message has been canceled."));
        statuses.put("X", new Status("X", 9, "Scheduled. The message is scheduled and undelivered."));
        statuses.put("E", new Status("E", 10, "Expired. The message has been expired."));
        statuses.put("L", new Status("L", 11, "Deleted. The message has been deleted by Operator."));
        statuses.put("V", new Status("V", 12, "Undeliverable. The message has not been delivered."));
        statuses.put("U", new Status("U", 13, "Unknown."));
        statuses.put("R", new Status("R", 14, "Received. The MO has been received."));
        statuses.put("A", new Status("A", 15, "Notified. The MO has been notified."));
        statuses.put("W", new Status("W", 16, "Waiting. The MO is waiting proccess."));
        statuses.put("Z", new Status("Z", 17, "Processed. The MO has been processed."));
    }
    
    private Status(String key, int code, String description){
        this.key = key;
        this.code = code;
        this.description = description;
    }
    
    /**
     * Gets the Status instance associated with the given key
     * @param key status key. It is returned by the HTTP APIs.
     * @return Status instance
     */
    public static Status getStatus(String key){
        Status st = Status.statuses.get(key);
        if(st == null)
            throw new IllegalArgumentException("Undefined status '"+ key +"'");
        return st;
    }
    
    /**
     * Gets the Key associated to the current Status instance
     * @return String key
     */
    public String getKey(){
        return this.key;
    }
    
    /**
     * Gets the code of the Status
     * @return code
     */
    public int getCode(){
        return this.code;
    }
    
    /**
     * Gets the description of the Status
     * @return
     */
    public String getDescription(){
        return this.description;
    }
    
}
