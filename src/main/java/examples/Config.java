package examples;

import java.io.File;

public class Config {

    public static String USERNAME = "";
    public static String APIKEY = "";
    public static String RECIPIENT = "";
    public static String RESOURCES_PATH;

    static {
        File currentDirFile = new File("");
        Config.RESOURCES_PATH = currentDirFile.getAbsolutePath() + File.separator + "resources" + File.separator;
    }
}
