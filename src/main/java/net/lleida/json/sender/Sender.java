package net.lleida.json.sender;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.json.Json;
import javax.json.JsonValue;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

public class Sender {
    private static final String HOST = "https://api.lleida.net/";
    private static final String SERVICE_SMS = HOST + "sms/v2";
    private static final String SERVICE_MSG = HOST + "messages/v3";

    private static final int MAX_LENGTH_PREMIUM_NUMBERS = 5;

    private static final String[] MIMETYPES = {
        "image/gif", "image/png", "image/jpeg",
        "audio/amr", "audio/x-wav", "audio/mpeg",
        "audio/midi", "video/3gpp", "video/mpeg"
    };

    private static final String[] LANGUAGES = {
        "ES", "CA", "EN", "FR", "DE",
        "IT", "NL", "PT", "PL", "SE"
    };

    private final String user;
    private final String apikey;
    private String lang;

    /**
     * Error code of the last HTTP request. 0 if no error.
     */
    public int errno;

    /**
     * Error description of the last HTTP Request. Empty if no error.
     */
    public String error;

    private Logger logger;

    /**
     * Constructs a Sender instance.
     * @param user your api username
     * @param apikey your api key
     * @param lang language
     * @throws IllegalArgumentException
     */
    public Sender(String user, String apikey, String lang) throws IllegalArgumentException{
        if(user == null || user.isEmpty()) {
            throw new IllegalArgumentException("Empty user!");
        }
        if(apikey == null || apikey.isEmpty()) {
            throw new IllegalArgumentException("Empty apikey!");
        }

        this.user = user;
        this.apikey = apikey;
        this.setLang(lang);

        this.errno = 0;
        this.error = "";
    }

    /**
     * Constructs a Sender instance.
     * @param user your api username
     * @param apikey your api key
     * @throws IllegalArgumentException
     */
    public Sender(String user, String apikey) throws IllegalArgumentException{
        this(user, apikey, "EN");
    }

    /**
     * Sends an SMS.
     * @param id identifier of your message
     * @param dst recipient(s)
     * @param text content
     * @param options aditional options (delivery_receipt, data_coding, schedule). See https://api.lleida.net/dtd/sms/v2/en/#sms
     * @return true or false depending on the result of the HTTP request
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public boolean sms(String id, JsonArray dst, String text, JsonObject options) throws UnsupportedEncodingException, Exception{
        return this.mt(id, dst, text, options);
    }

    /**
     * Sends an SMS.
     * @param id identifier of your message
     * @param dst recipient(s)
     * @param text content
     * @return true or false depending on the result of the HTTP request
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public boolean sms(String id, JsonArray dst, String text) throws UnsupportedEncodingException, Exception{
        return this.mt(id, dst, text, Json.createObjectBuilder().build());
    }

    /**
     * Sends a Registered SMS.
     * @param id identifier of your message
     * @param dst recipient(s)
     * @param text content
     * @param options aditional options. Use the 'delivery_receipt' index to configure it. See https://api.lleida.net/dtd/sms/v2/en/#delivery_receipt
     * @return true or false depending on the result of the HTTP request
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public boolean registeredSMS(String id, JsonArray dst, String text, JsonObject options) throws UnsupportedEncodingException, Exception{
        if(options.isEmpty() || !options.containsKey("delivery_receipt")){
            JsonObject tmp = Json.createObjectBuilder()
                .add("lang", this.lang)
                .add("cert_type", "D")
                .add("email", "INTERNALID")
                .build();
            options = Json.createObjectBuilder(options).add("delivery_receipt", tmp).build();
        }
        return this.mt(id, dst, text, options);
    }

    /**
     * Sends a Registered SMS.
     * @param id identifier of your message
     * @param dst recipient(s)
     * @param text content
     * @return true or false depending on the result of the HTTP request
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public boolean registeredSMS(String id, JsonArray dst, String text) throws UnsupportedEncodingException, Exception{
        return registeredSMS(id, dst, text, Json.createObjectBuilder().build());
    }

    /**
     * Sends a Scheduled SMS.
     * @param id identifier of your message
     * @param dst recipient(s)
     * @param text content
     * @param options aditional options. Use the 'schedule' index to configure it. See https://api.lleida.net/dtd/sms/v2/en/#schedule
     * @return true or false depending on the result of the HTTP request
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public boolean scheduledSMS(String id, JsonArray dst, String text, JsonObject options) throws UnsupportedEncodingException, Exception{
        if(options.isEmpty() || !options.containsKey("schedule")){
            throw new IllegalArgumentException("Empty schedule option!");
        }
        return this.mt(id, dst, text, options);
    }

    /**
     * Sends a Scheduled SMS.
     * @param id identifier of your message
     * @param dst recipient(s)
     * @param text content
     * @return true or false depending on the result of the HTTP request
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public boolean scheduledSMS(String id, JsonArray dst, String text) throws UnsupportedEncodingException, Exception{
        return scheduledSMS(id, dst, text, Json.createObjectBuilder().build());
    }

    /* where the magic becomes real */

    /**
     * Sends an SMS.
     * @param id identifier of your message
     * @param dst recipient(s)
     * @param text content
     * @param options aditional options. See https://api.lleida.net/dtd/sms/v2/en/#sms
     * @return true or false depending on the result of the HTTP request
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public boolean mt(String id, JsonArray dst, String text, JsonObject options) throws UnsupportedEncodingException, Exception{
        String json = this.make_json_mt(id, dst, text, options);
        this.debug("json: " + this.protect_json(json));
        return this.response_parser(this.do_request(Sender.SERVICE_SMS, URLEncoder.encode(json, "UTF-8")));
    }

    /* status */

    /**
     * Get status of an SMS.
     * @param id identifier of your message
     * @return a Status Object
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public Status getStatusSMS(String id) throws UnsupportedEncodingException, IOException{
        String json = this.make_json_status("mt", id);
        this.debug("json" + this.protect_json(json));
        return this.response_parser_status("mt", id, this.do_request(Sender.SERVICE_MSG, URLEncoder.encode(json, "UTF-8")));
    }

    /**
     * Get Status of a Scheduled SMS.
     * @param id identifier of your message
     * @return a Status Object
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public Status getStatusScheduled(String id) throws UnsupportedEncodingException, IOException{
        String json = this.make_json_status("sched", id);
        this.debug("json" + this.protect_json(json));
        return this.response_parser_status("mt", id, this.do_request(Sender.SERVICE_MSG, URLEncoder.encode(json, "UTF-8")));
    }

    /**
     * Get the description of a Status object.
     * @param status Status object
     * @return description of the Status
     */
    public String getStatusDescription(Status status){
        if (status != null) {
           return status.getDescription();
        }
        throw new IllegalArgumentException("Empty status!");
    }

    /**
     * Get the code of a Status object.
     * @param status Status object
     * @return code of the Status
     */
    public int getStatusCode(Status status){
        if (status != null) {
           return status.getCode();
        }
        throw new IllegalArgumentException("Empty status!");
    }

    /* setters */

    /**
     * Sets instance language.
     * @param lang Alpha-2 language code
     */
    public void setLang(String lang){
        this.lang = this.check_lang(lang);
    }

    /**
     * Sets where the logs will be saved.
     * @param filename path to the file
     * @throws IOException
     */
    public void setLogger(String filename) throws IOException{
        this.logger = Logger.getLogger("Sender");

        //removing default handlers
        this.logger.setUseParentHandlers(false);

        FileHandler fh = new FileHandler(filename, true);
        this.logger.addHandler(fh);
        fh.setFormatter(new SimpleFormatter());
    }

    /**
     * Encodes in Base64 the content of the file.
     * @param filename path to the file
     * @return base64 encoded string
     * @throws IOException
     */
    public String getFileContentBase64(String filename) throws IOException{
        File file = new File(filename);
        return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
    }

    /* deeper magic */
    private String make_json_mt(String id, JsonArray dst, String text, JsonObject options) throws Exception{
        if(id == null || id.isEmpty())
            throw new IllegalArgumentException("Empty user_id!");
        if(dst == null || dst.isEmpty())
            throw new IllegalArgumentException("Empty recipient!");
        if(text == null || text.isEmpty())
            throw new IllegalArgumentException("Empty text!");

        //perque no peti
        if (options == null) options = JsonObject.EMPTY_JSON_OBJECT;

        JsonObjectBuilder options_job = Json.createObjectBuilder(options);

        if(!options.isEmpty()){
            options = this.check_options(options);
            options_job = Json.createObjectBuilder(options);

            if(options.containsKey("src")){
                options_job.add("src", this.check_src(options.getString("src")));
            }else{
                // enabling allow_answer if no src defined
                options_job.add("allow_answer", "1");
            }

            if(options.containsKey("schedule")){
                options_job.add("schedule", this.check_schedule(options.getString("schedule")));
            }
        }

        options_job.add("user",     this.user);
        options_job.add("apikey", this.apikey);
        options_job.add("user_id",  id);
        options_job.add("dst",      this.make_dst(dst));

        options = options_job.build();
        options = this.make_text(text, options);

        return Json.createObjectBuilder()
            .add("sms", options)
            .build()
            .toString();
    }

    private String make_json_status(String request, String id) {
        return Json.createObjectBuilder()
            .add("user",     this.user)
            .add("apikey", this.apikey)
            .add("user_id",  id)
            .add("request",  request)
            .build()
            .toString();
    }

    private JsonObject make_dst(JsonArray dst) {
        JsonArrayBuilder list_builder = Json.createArrayBuilder();

        for(JsonValue d : dst){
            String num = this.check_number(d.toString());

            if(!num.isEmpty())
                list_builder.add(num);
        }

        JsonArray recipients = list_builder.build();

        if(recipients.isEmpty())
            throw new IllegalArgumentException("Empty or wrong formatted recipients");

        return Json.createObjectBuilder()
            .add("num", recipients)
            .build();
    }

    private JsonObject make_text(String text, JsonObject options) {
        JsonObjectBuilder options_job = Json.createObjectBuilder(options);

        Charset charset;
        if(options.containsKey("unicode") && this.toBool(options.getString("unicode"))){
            charset = Charset.forName("UTF-16");
            options_job.add("data_coding", "unicode");
        }else{
            charset = Charset.forName("ISO-8859-1");
        }
        options_job.add("encoding", "base64");

        text = Base64.getEncoder().encodeToString(
            text.getBytes(charset)
        );
        options_job.add("txt", text);
        options_job.add("charset", charset.name());

        return options_job.build();
    }

    /* validations */
    private JsonObject check_options(JsonObject options){
        JsonObject delivery_receipt;
        JsonObjectBuilder options_job = Json.createObjectBuilder(options);
        JsonObjectBuilder delivery_receipt_job;

        if(options.containsKey("delivery_receipt")){
            delivery_receipt = options.getJsonObject("delivery_receipt");
            delivery_receipt_job = Json.createObjectBuilder(delivery_receipt);

            if(delivery_receipt.containsKey("lang")){
                delivery_receipt_job.add("lang", this.check_lang(delivery_receipt.getString("lang")));
            }else{
                delivery_receipt_job.add("lang", this.lang);
            }

            if(delivery_receipt.containsKey("email")){
                delivery_receipt_job.add("email", this.check_email(delivery_receipt.getString("email")));
            }else{
                delivery_receipt_job.add("email", "INTERNALID");
            }

            if(delivery_receipt.containsKey("cert_type")){
                delivery_receipt_job.add("cert_type", this.check_registered_type(delivery_receipt.getString("cert_type")));
            }
            options_job.add("delivery_receipt", delivery_receipt_job.build());
        }
        return options_job.build();
    }

    private void check_attachment(JsonObject attachment){
        if(!attachment.containsKey("mime")){
            throw new IllegalArgumentException("Invalid attachment format, unknown mimetype!");
        }

        String mime = attachment.getString("mime");
        if(!this.check_mimetype(mime)){
            throw new IllegalArgumentException("Invalid mimetype!");
        }

        if(!attachment.containsKey("content")){
            throw new IllegalArgumentException("Invalid attachment format, unknown content!");
        }

        String content = attachment.getString("content");
        if(content.isEmpty()){
            throw new IllegalArgumentException("Empty attachment content!");
        }
        if(!this.isBase64Encoded(content)){
            throw new IllegalArgumentException("Invalid attachment format, unknown content format!");
        }
    }

    private boolean isBase64Encoded(String data){
        return data.length() >= 15 && data.matches("^[a-zA-Z0-9/+]*={0,2}$");
    }

    private String check_registered_type(String type) throws IllegalArgumentException{
        type = type.toUpperCase();
        if(type.equals("D") || type.equals("T")){
            return type;
        }
        throw new IllegalArgumentException("Invalid registered type!");
    }

    private String check_lang(String lang){
        if (lang == null || lang.isEmpty()) {
            throw new IllegalArgumentException("Empty lang!");
        }
        lang = lang.toUpperCase();

        boolean result = false;
        for(String l: Sender.LANGUAGES){
            if(l.equals(lang)){
                result = true;
                break;
            }
        }

        if(!result)
            throw new IllegalArgumentException("Invalid lang!");
        return lang;
    }

    private String check_src(String sender){
        sender = sender.trim();
        String[] replaces = {"\n", "\r", "'", ".", "(", ")", "+"};
        for(String replace : replaces){
            sender = sender.replace(replace, "");
        }

        if(sender.length() > 20){
            sender = sender.substring(0, 20);
        }
        return sender;
    }

    private String check_schedule(String schedule) throws Exception {
        String utc = new SimpleDateFormat("Z").format(new java.util.Date());
        if(schedule.matches("^[0-9]{12}[\\-\\+]+[0-9]{4}$")){
            if(Integer.parseInt(utc) >= 0){
                schedule = schedule.replace("+", "-");
            }else{
                schedule = schedule.replace("-", "+");
            }
        }else if(schedule.matches("[0-9]{12}")){
            if(Integer.parseInt(utc) >= 0){
                utc = utc.replace("+", "-");
            }else{
                utc = utc.replace("-", "+");
            }
            schedule = schedule.concat(utc);
        }else{
            throw new IllegalArgumentException("Empty schedule option");
        }

        return schedule;
    }

    private String check_prefix(String prefix){
        if(prefix.length() == 0)
            return "";

        if(prefix.charAt(0) == '+' || prefix.charAt(0) == ' ')
            prefix = prefix.substring(1);
        else if(prefix.substring(0,2).equals("00"))
            prefix = prefix.substring(2);

        if(prefix.length() != 0 && Integer.parseInt(prefix) >= 0)
            return prefix;
        return "";
    }

    private String check_number(String num, String prefix){
        boolean firstWasSpace = false;

        if(num.length() == 0)
            return "";

        //we need to check this before "trimming" the string.
        if(num.charAt(0) == ' '){
            firstWasSpace = true;
            num = num.substring(1);
        }

        num = num.trim();
        String[] replaces = {"\n", "\r", "'", "\"", " ", ".", ";", ",", "(", ")", "-"};
        for(String replace : replaces){
            num = num.replace(replace, "");
        }

        if(num.length() < MAX_LENGTH_PREMIUM_NUMBERS)
            return "";

        if(num.charAt(0) == '+'){
            num = num.substring(1);
        }else if(num.substring(0,2).equals("00")){
            num = num.substring(2);
        }else if(!firstWasSpace){
            /**
             * As long as we haven't found "+", " " or "00" in the number
             * we'll assume they missed the prefix and we'll add it if
             * there is not.
             */
            prefix = this.check_prefix(prefix);
            if(prefix.length() > 0 && num.substring(0, prefix.length()).equals(prefix)){
                num = prefix.concat(num);
            }
        }

        if(num.matches("^[0-9]+$")){
            return "+".concat(num);
        }
        return "";
    }

    private String check_number(String num){
        return this.check_number(num, "+34");
    }

    private String check_email(String email){
        email = email.trim();
        String[] replaces = {"\n", "\r", "'", " ", "(", ")"};
        for(String replace : replaces){
            email = email.replace(replace, "");
        }

        if(email.toUpperCase().equals("INTERNAL") || email.toUpperCase().equals("INTERNALID")){
            return "INTERNALID";
        }

        String regex = "^(?:[\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\`\\{\\|\\}\\~]+\\.)*[\\w\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\`\\{\\|\\}\\~]+@(?:(?:(?:[a-zA-Z0-9_](?:[a-zA-Z0-9_\\-](?!\\.)){0,61}[a-zA-Z0-9_-]?\\.)+[a-zA-Z0-9_](?:[a-zA-Z0-9_\\-](?!$)){0,61}[a-zA-Z0-9_]?)|(?:\\[(?:(?:[01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}(?:[01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\]))$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);

        if(matcher.matches()){
            return email;
        }
        return "INTERNALID";
    }

    private boolean toBool(String value){
        value = value.toLowerCase();
        return value.equals("1") || value.equals("true") || value.equals("on") || value.equals("yes") || value.equals("y");
    }

    private boolean check_mimetype(String mime){
        boolean result = false;
        for(String m: Sender.MIMETYPES){
            if(m.equals(mime)){
                result = true;
                break;
            }
        }
        return result;
    }

    /* removing apikeys for log */
    private JsonObject protect_json(String json){
        JsonObject object = Json.createReader(new StringReader(json)).readObject();
        JsonObjectBuilder object_job = Json.createObjectBuilder(object);

        for(Iterator iterator = object.keySet().iterator(); iterator.hasNext();){
            String key = (String) iterator.next();
            JsonValue obj = object.get(key);

            if(obj.getValueType() == JsonValue.ValueType.OBJECT){
                object_job.add(key, this.protect_json(obj.toString()));
            }else if(key.equals("apikey")){
                object_job.add(key, "censored apikey");
            }
        }

        return object_job.build();
    }

    /* debug */
    private void debug(String message){
        if(logger != null){
            this.logger.log(Level.INFO, message);
        }
    }

    /* http requests */
    private String do_request(String service, String json) throws MalformedURLException, IOException{
        URL url = new URL(service);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        OutputStream os = con.getOutputStream();
        os.write(json.getBytes());
        os.flush();
        os.close();

        if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + con.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String output, result = "";
        while ((output = br.readLine()) != null) {
            result = result + output;
        }
        con.disconnect();
        return result;
    }

    /* parsers */
    private boolean response_parser(String response){
        JsonObject result = Json.createReader(new StringReader(response)).readObject();

        int code = Integer.parseInt(result.get("code").toString());
        String status = result.get("status").toString();

        this.debug("response_parser type: " + result.get("request") + " code: " + code);

        if(code == HttpURLConnection.HTTP_OK){
            this.set_error(false);
            return true;
        }
        this.set_error(status, code);
        return false;
    }

    private void set_error(String error, int code){
        this.error = error;
        this.errno = code;
    }

    private void set_error(boolean error){
        if(!error){
            this.error = "";
            this.errno = 0;
        }
    }

    private Status response_parser_status(String request, String id, String response){
        String state = "U";
        JsonObject result = Json.createReader(new StringReader(response)).readObject();

        int code = Integer.parseInt(result.get("code").toString());
        this.debug("id: " + id + " type: " + request + " code: " + code + ":" + result.get("status"));

        if(code == HttpURLConnection.HTTP_OK && result.containsKey("messages")){
            JsonArray messages = result.getJsonArray("messages");
            for(JsonValue m : messages){
                if(m.getValueType() == JsonValue.ValueType.OBJECT){
                    JsonObject message = (JsonObject) m;
                    if(message.containsKey("state")){
                        state = message.getString("state");
                    }
                }
            }
        }

        this.debug("id: "+ id + " type: "+ request + " state: " + state);
        return Status.getStatus(state);
    }
}
