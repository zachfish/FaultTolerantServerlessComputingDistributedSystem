package edu.yu.cs.com3800;


import java.util.logging.*;

public interface LoggingServer {

    default Logger initializeLogging(String str){
        Logger logger = Logger.getLogger(str);
        //logger.removeHandler();

        FileHandler fileHandler = null;
        try {
          fileHandler = new FileHandler("logs/" +str + ".log", true);
        } catch (Exception e) {
            System.out.println("eeee");
            e.printStackTrace();
        }
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);
       // ConsoleHandler cHandler = new ConsoleHandler();
       // cHandler.setLevel(Level.SEVERE);
       // logger.addHandler(cHandler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        SimpleFormatter formatter = new SimpleFormatter();
        fileHandler.setFormatter(formatter);
        return logger;
    }

}
