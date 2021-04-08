import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;

public class HTTPServer3 {
    static final File WEB_ROOT = new File("lib/");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    static final int PORT = 8081;

    private static LibDatabase database = LibDatabase.getDatabase();

    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started.\n Listening for connections on port: " + PORT + "...\n");

            while (true) {
                // we listen until user halts server execution
                Socket connection = server.accept();
                System.out.println("Connection opened. (" + new Date() + ")");
                runServer(connection);
            }

        } catch (IOException e) {
            System.out.println("Error while accepting request!");
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                System.out.println("Unable to close the server socket!");
            }
        }
    }

    private static void runServer(Socket connection) {
        BufferedReader request = null;
        BufferedOutputStream response = null;
        try {
            // READ CHARACTERS FROM THE CLIENT VIA INPUT STREAM ON THE SOCKET
            request = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            // WE GET character output stream to client (for headers)
            response = new BufferedOutputStream(connection.getOutputStream());

            // Read only the first line of the request, and break it where there is a space
            // character
            String[] fragments = request.readLine().split(" ");
            String method = fragments[0].toUpperCase();
            String fileRequested = fragments[1].toLowerCase();
            System.out.println(fileRequested);
            // we support only GET, POST and HEAD methods, we check
            if (!method.equals("GET") && !method.equals("POST") && !method.equals("HEAD")) {
                replyMethodNotSupported(response, method);

            } else {
                try {
                    fileRequested = replyWithRequestedFile(response, fileRequested, method, request);

                } catch (FileNotFoundException fnfe) {
                    try {
                        respondFileNotFound(response, fileRequested);

                    } catch (IOException ioe) {
                        System.out.println("Error with file not found exception: " + ioe.getMessage());
                    }
                }
            }

        } catch (IOException ioe) {
            replyServerError(ioe);

        } finally {
            closeConnection(connection, request, response);
        }
    }

    private static void closeConnection(Socket connect, BufferedReader in, BufferedOutputStream dataOut) {
        try {
            in.close();
            dataOut.close();
            connect.close();

        } catch (Exception e) {
            System.err.println("Error closing stream: " + e.getLocalizedMessage());

        }

        System.out.println("Connection closed.\n");
    }

    private static void replyServerError(IOException ioe) {
        System.out.println("Server error: " + ioe);
    }

    private static String replyWithRequestedFile(
            BufferedOutputStream response,
            String fileRequested,
            String method,
            BufferedReader request)
            throws IOException
    {
        String htmlPath = "/" + DEFAULT_FILE;

        File file = new File(WEB_ROOT, htmlPath);
        int fileLength = (int) file.length();
        String content = getContentType(htmlPath);

        //because the output is a outputstream, so we need to change the file to byte[] array.
        byte[] fileData = readFileData(file, fileLength);
        //call the method to format headers.
        formatHttpResponseHeader(response, "200 OK", fileLength, content);

        if (method.equals("GET")){
            response.write(fileData, 0, fileLength);
        }

        else if (method.equals("POST")){

            String line = null;
            final PrintWriter res = new PrintWriter(response);


            // User Name Validation
            if (fileRequested.equals("/orm-lib")){

                while (request.ready()){
                    line = request.readLine();
                    if (line.equals("Host: localhost")){
                        res.println("Requested body is:");
                        res.println();
                    }
                    else if(!line.trim().isBlank()){ // if line is not blank
                        res.println(line);
                        switch (line) {
                            case "Client 1 -  with QUESTION1":
                                addUser(res,request);
                                res.println("Insert Completed!");
                                break;
                            case "Client 2 -  with QUESTION2":
                                res.println("Update Completed!");
                                break;
                            case "Client 3 -  with QUESTION3":
                                res.println("Delete Completed!");
                                break;
                            default:
                                //res.println("invalid question");
                                break;
                        }
                    }
                }
            }

            else {
                // Return the request.
                while (request.ready()){
                    line = request.readLine();
                    if (line.equals("Host: localhost")){
                        res.println("Requested body is:");
                    }
                    else{
                        res.println(line);
                    }
                }
            }
            res.print("\n\n");
            res.flush();
        }
        response.flush();
        System.out.println("Method " +  method + " File " + fileRequested + " of type " + content + " returned!");
        return fileRequested;
    }

    private static void replyMethodNotSupported(BufferedOutputStream response, String method) throws IOException {
        System.out.println("501 NOT IMPLEMENTED: " + method + " method.");

        File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
        int fileLength = (int) file.length();
        String contentMimeType = "text/html";

        formatHttpResponseHeader(response, "501 not implemented", fileLength, contentMimeType);

        // read content to return to client
        byte[] fileData = readFileData(file, fileLength);
        response.write(fileData, 0, fileLength);
        response.flush();
    }

    private static void formatHttpResponseHeader(BufferedOutputStream dataOut, String responseStatus, int fileLength,
                                                 String contentMimeType) {
        final PrintWriter out = new PrintWriter(dataOut);
        out.println("HTTP/1.1 " + responseStatus);
        out.println("Server: Java HTTP Server from Di: 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + contentMimeType);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and body. VERY IMPORTANT.
        out.flush();
    }

    private static byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        fileIn = new FileInputStream(file);
        fileIn.read(fileData);

        fileIn.close();
        return fileData;
    }

    // return supported Mime Types
    private static String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
            return "text/html";
        else
            return "text/plain";
    }

    private static void respondFileNotFound(BufferedOutputStream response, String fileRequested) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();

        formatHttpResponseHeader(response, "404 file not found", fileLength, "text/html");

        byte[] fileData = readFileData(file, fileLength);
        response.write(fileData, 0, fileLength);
        response.flush();
        System.out.println("File " + fileRequested + " not found!");
    }


    private static void addUser(PrintWriter res, BufferedReader req){
        String userName = "";
        String userPassword = "";
        try {
            while (req.ready()) {

                String line[] = req.readLine().split("=");

                switch (line[0]) {
                    case "userName":
                        userName = line[1];
                        break;
                    case "userPassword":
                        userPassword = line[1];
                }
            }
            database.addUser(userName, userPassword);
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}