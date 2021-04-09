package HTTPServer;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.logging.*;

public class HTTPServer{

    // instance fields
    private Socket connectionSocket;


    static File WEB_ROOT; // Stores probed working directory
    // Literal directory and file names used as fake server "file system"
    static final String ROOT_NAME = "lib";
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "501.html";

    // Port Setting
    static final int PORT = 8080;

    // DEBUGGING
    // ----------------------------------------------
    // Set this field to dump request message to terminal, instead of handling it
    private static final boolean DUMP_REQUEST_TO_TERMINAL = false;


    // Logging
    private static Logger logger;
    private static FileHandler fileLogger;

    public static void main(String[] args) {

        WEB_ROOT = new File(System.getProperty("user.dir") + "/" + ROOT_NAME);

        // Set up logger
        loggerSetup();
        logger.severe("WEB_ROOT is: " + WEB_ROOT);

        try {
            ServerSocket listenSocket = new ServerSocket(PORT);
            logger.severe("Server started.... \n Port is open on " + PORT);
            logger.info("test");
            //Listen until connection is required from user


            // TODO: 02/03/2021 Make this close gracefully. Add external shutdown command?
            // // TODO: 02/03/2021 Multi threading for request handling and clients?
            do {
                Socket connectionSocket = listenSocket.accept();
                HTTPHandler handler = new HTTPHandler(connectionSocket);
                Thread handlerThread = new Thread(handler);
                handlerThread.setDaemon(true);
                handlerThread.start();
                logger.severe("Started handler on thread: " + handlerThread.getName());


                logger.info("Connection established on date: " + new Date() + "\n");
                Thread.sleep(2000);
                //runServer(connectionSocket);
            } while (true);

        } catch (IOException | InterruptedException e) {
            System.out.println("Server error as: " + e);
        }

    }

    /**
     * Sets up system logging for server.
     */
    private static void loggerSetup() {
        logger = Logger.getLogger("ServerLog");
        logger.setLevel(Level.INFO);
        ConsoleHandler consoleLogger = new ConsoleHandler();
        consoleLogger.setFormatter(new SimpleFormatter());
        try {
            fileLogger = new FileHandler(
                    WEB_ROOT.getPath() + "/serverLog%u.log", true);
            logger.addHandler(fileLogger);
        } catch (IOException ioe) {
            logger.info("Failed to create log file 'log.txt'. IOException: \n" + ioe.getMessage());
        }
    }

    /**
     * Debugging function for dumping the HTTP request message literal to stdout.
     *
     * @param in BufferedReader to read message from.
     */
    private static void dumpRequest(BufferedReader in) {
        in.lines().forEach(System.out::println);
    }

    /**
     * Closes given Socket, BufferedReader and BufferedOutputStream objects.
     *
     * @param connection Connected socket.
     * @param reader     BufferedReader related to socket being closed.
     * @param outStream  BufferedOutputStream related to socket being closed.
     */
    private static void closeConnection(Socket connection, BufferedReader reader, BufferedOutputStream outStream) {
        try {
            if (reader != null) reader.close();
            if (outStream != null) outStream.close();
            if (connection != null) connection.close();
        } catch (IOException ioe) {
            logger.severe("Server socket closed.");
            logger.info(ioe.getMessage());
        } finally {
            fileLogger.close(); // Write tail to logger file
        }
    }

    /**
     * Formats the given Instant according to RFC 1123.
     * Example: Sun, 06 Nov 1994 08:49:37 GMT.
     *
     * @param time The Instant to format.
     * @return The Instant, formatted according to RFC 1123.
     */
    private static String toServerHTTPTime(Instant time) {
        return DateTimeFormatter // RFC 1123 compliant HTTP time
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"))
                .format(time);

    }


}
