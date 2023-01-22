package edu.yu.cs.com3800;


import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public interface LoggingServer {
    default Logger initializeLogging(String str){
        Logger logger = Logger.getLogger("Log");
        FileHandler fileHandler = null;
        try {
          fileHandler = new FileHandler(str + ".log", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL);
        SimpleFormatter formatter = new SimpleFormatter();
        fileHandler.setFormatter(formatter);
        return logger;
    }

}
