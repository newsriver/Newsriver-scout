package ch.newsriver.scout;


import org.apache.commons.cli.*;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;


import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by Elia Palme on 10/03/16.
 */
public class ScoutMain {


    private static final Logger logger = LogManager.getLogger(ScoutMain.class);
    private static final SimpleDateFormat fmt = new SimpleDateFormat("'Current time: ' yyyy-MM-dd HH:mm:ssZ");
    private static int port = 9098;

    static Scout scout;
    static ch.newsriver.scout.Console webConsole;
    public static void main(String[] args){


        Options options = new Options();

        options.addOption("f","pidfile", true, "pid file location");
        options.addOption(Option.builder("p").longOpt("port").hasArg().type(Number.class).desc("port number").build());



        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse( options, args);
        }
        catch( ParseException exp ) {
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Newsriver scout", options );
            return;
        }


        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutting down!");
                if(webConsole!=null)webConsole.stop();
                if(scout !=null) scout.stop();
            }
        });

        String pid = getPid();
        System.out.print(getManifest());


        //GET PID
        BufferedWriter out = null;
        try {

            String pidFile = "/var/run/Newsriver-pid.pid";
            if(cmd.hasOption("pidfile")){
                pidFile = cmd.getOptionValue("pidfile");
            }

            File file = new File(pidFile);
            file.createNewFile();
            FileWriter fstream = new FileWriter(file, false);
            out = new BufferedWriter(fstream);
            out.write(pid);
        } catch (Exception e) {
            logger.error("Unable to save process pid to file", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {};
        }

        try {
            if (cmd.hasOption("p")) {
                port = ((Number) cmd.getParsedOptionValue("p")).intValue();
            }
        }catch (ParseException e ){
            logger.fatal("Unable to parse port number:" +cmd.getOptionValue("p"));
            return;
        }

        try {
            webConsole = new ch.newsriver.scout.Console(port);
            webConsole.start();
        } catch (IOException ex) {
            logger.fatal("Unable to bind http port:" + port + "\n"+ ex.getLocalizedMessage());
            return;
        }

        try {
            scout = new Scout();
            new Thread(scout).start();
        } catch (Exception e) {
            logger.fatal("Unable to initialize scout", e);
        }

    }

    private static String getPid() {

        String processName = ManagementFactory.getRuntimeMXBean().getName();
        if(processName.indexOf("@")>-1){
            return processName.split("@")[0];
        }
        return null;
    }


    private static String getVersion() {
        InputStream inputStream = null;
        try {
            Properties prop = new Properties();
            String propFileName = "version.properties";
            inputStream = ScoutMain.class.getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

           return prop.getProperty("version");

        } catch (Exception e) {
            logger.error("Unable to read current version number",e);
        } finally {
            try {
            inputStream.close();
            } catch (Exception e) { }
        }
        return null;
    }


    private static String getWelcome() {
        try {
            InputStream inputStream = ScoutMain.class.getClassLoader().getResourceAsStream("welcome.txt");
            return IOUtils.toString(inputStream, "utf-8");
        } catch (Exception ex) {
            return "";
        }
    }

    private static String getHostName() {

        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            hostname = "Unknown Host";
            logger.error("Unable to retrieve host name", ex);
        }
        return hostname;
    }

    public static String getManifest(){

        StringBuilder result = new StringBuilder();
        result.append(getWelcome()).append("\n");
        result.append("Version: ").append(getVersion()).append("\n");
        result.append("Hostname: ").append(getHostName()).append("\n");
        result.append("PORT: ").append(port).append("\n");
        result.append("PID: ").append(getPid()).append("\n");
        result.append(fmt.format(new Date())).append("\n");

        return result.toString();
    }

}