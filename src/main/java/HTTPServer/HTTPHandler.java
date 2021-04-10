package HTTPServer;

import HTTPServer.HTTPMessage.HTTPRequest;
import HTTPServer.HTTPMessage.HTTPResponse;
import HTTPServer.HTTPStatusCode.HttpStatusCode;
import HTTPServer.Services.LibDatabase;
import HTTPServer.Services.PokerSend;
import HTTPServer.Services.ValidUserName;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static HTTPServer.HTTPServer.*;

public class HTTPHandler implements Runnable {

    private static Logger logger;

    // flag to enable
    private final static boolean DEBUG = false;

    private Socket socket;


    public HTTPHandler(Socket connectionSocket) {
        this.socket = connectionSocket;
        logger = Logger.getLogger("ServerLog");
        logger.info("HTTPHandler up and running.");
    }

    /**
     * Debugging function for dumping the HTTP request message literal to stdout.
     *
     * @param in BufferedReader to read message from.
     */
    private static void dumpRequest(BufferedReader in) {
        in.lines().forEach(System.out::println);
    }


    @Override
    public void run() {

        //Manage client connection.
        BufferedReader inReader = null;
        BufferedOutputStream outStream = null;



        try {

            //Read characters from the client via input stream.
            inReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            if (DEBUG) {
                dumpRequest(inReader);
                closeConnection(socket, inReader, outStream);
                Thread.currentThread().interrupt();
            }
            //Parse HTTP request.

            HTTPRequest request = parseRequest(inReader);

            // Format and send response
            HTTPResponse response = formatResponse(request);

            // buffer the socket output stream
            outStream = new BufferedOutputStream(socket.getOutputStream());

            // Send message
            if (response != null) {
                writeOutAndFlush(outStream, response);
                logger.info("Response Returned.");

            } else {
                outStream.flush();
            }
            //closeConnection(connectionSocket, inReader, outStream);

        } catch (IOException e) {
            System.out.println("IO EXCEPTION: " + e.getMessage());
        } finally {
            closeConnection(socket, inReader, outStream);
        }

    }

    /**
     * Parses the content of a BufferedReader, creating an HTTPRequest object from its contents.
     * This method naively parses any BufferedReader object as if its contents had the
     * formatting of an HTTP request according to RFC 2616.
     *
     * @param inReader A BufferedReader holding the data of an HTTP request.
     * @return An HTTPRequest object of the request, or null if object creation failed for any reason.
     */
    private static HTTPRequest parseRequest(BufferedReader inReader) {
        String method;
        String url;
        String version = "";
        HTTPRequest request = null;
        HTTPRequest.Builder reqBuilder;

        try {
            // HTTP request line has format "METHOD /path/to/requested.file HTTP/x.x"
            String[] requestLine = inReader.readLine().split(" ");
            method = requestLine[0]; // METHOD
            logger.info(method);
            url = requestLine[1];// /path/to/requested.file
            logger.info(url);
            if (requestLine.length > 2) {
                version = requestLine[2]; // HTTP/x.x
                logger.info(version);
            }
            // HTTPRequest uses builder pattern
            // Required parameters are method, url and HTTP version of request
            reqBuilder = new HTTPRequest.Builder(method, url, version);
            logger.info("Successfully created request builder.");


            //  double check using the readLine function as loop invariants on these.
            //  Will we get stuck on inner loop if inReader has no more lines?
            //  From my current understanding, readLine can halt the thread if there is not a
            //  complete line to read, and that using read(), which returns the next char value,
            //  into a char[], StringBuilder or similar is more reliable.

            // Add request header fields to Builder
            if (inReader.ready()) { // Check for presence of first header line
                String headLine = inReader.readLine();
                while (!headLine.trim().isBlank()) { // Head ends with blank newline, i.e. "\r\n".
                    String[] keyValue = headLine.split(" "); // split into "FieldName:" "FieldValue"
                    reqBuilder.addHeaderField(keyValue[0], keyValue[1]); // "FieldName:" is key, "FieldValue" is value
                    if (inReader.ready()) { // next line from buffer, if present.
                        headLine = inReader.readLine();
                    } else { // or a blank line if not, just to ensure that the loop ends.
                        headLine = "";
                    }
                }// done with header fields
                logger.info("Done with header fields");
            }

            // fixme: 02/03/2021 This "solution" to the POST content-read bug,
            //  is to only try to read the body if the request is a POST.
            //  This assumes that no GET or HEAD requests will ever contain a body,
            //  which is not necessarily a correct assumption according to RFC 2616
            //  We should try to implement this in a more elegant fashion

            StringBuilder bodyBuilder = new StringBuilder();
            if (method.equalsIgnoreCase("POST")) {
                // Adding request body to Builder
                logger.info("Building request body string.");

                // fixme: 01/03/2021 Find some better solution to the below.
                //  Some way to wait for line, only if we are certain there will be one.
                // Sleep the thread, to give inReader time to "catch up",
                // otherwise we sometimes end up with false return from inRead.ready()...
                Thread.sleep(1);

                do {
                    bodyBuilder.append((char) inReader.read());
                } while (inReader.ready());

            }
            String body = bodyBuilder.toString();
            // Add body string to Builder
            if (!body.isBlank()) {
                // If body is not blank
                logger.info("Body string added to request object");
                reqBuilder.appendBodyString(body);
            } else {
                // Don't add body if blank
                logger.info("Body string is blank, nothing to add.");
            }


            // Finally we build the HTTPRequest object
            request = reqBuilder.build();
            logger.info("Printing request.toString:\n" + request.toString());

        } catch (IOException | InterruptedException ioe) {
            logger.info(ioe.toString());
        }

        return request;
    }

    /**
     * Writes an HTTPResponse message to the provided OutputStream.
     * Used to send the finished HTTP message to the client.
     *
     * @param outStream OutputStream to write response message content to.
     * @param response  The HTTPResponse message to write out.
     */
    private static void writeOutAndFlush(BufferedOutputStream outStream, HTTPResponse response) {
        // PrintWriter is buffered by outStream
        PrintWriter writeOut = new PrintWriter(outStream);

        // Format and append the response status-line to message
        // HTTP-Version SP Status-Code SP Reason-Phrase CRLF
        writeOut.println(
                response.getVersion()
                        + " " +
                        response.getStatusCode()
                        + " " +
                        response.getReasonPhrase());

        // Append head fields to response message
        response.getHeadFields()
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .forEach(writeOut::println);
        writeOut.print("\r\n");

        // Append message body, if any, to response
        if (!response.getBody().equals("")) {

            writeOut.println(response.getBody());
        }

        // Flush writer and output stream.
        writeOut.flush();
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
        }
    }

    /**
     * Formats an HTTP response to the provided HTTP Request.
     *
     * @param request HTTPRequest object to create a response for.
     * @return HTTPResponse object containing response message,
     * or null if HTTPResponse object creation failed for any reason.
     */
    private static HTTPResponse formatResponse(HTTPRequest request) {
        HTTPResponse response = null;
        HTTPResponse.Builder builder = null;

        // Add more cases here as other HTTP methods are implemented.
        switch (request.getMethod().toUpperCase()) {
            case "GET":
                logger.info("GET request");
                builder = handleGETRequest(request);
                break;
            case "HEAD":
                logger.info("HEAD request");
                builder = handleHEADRequest(request);
                break;
            case "POST":
                logger.info("POST request");
                builder = handlePOSTRequest(request);
                break;
            default:
                logger.info("Not implemented");
                builder = handleNYIRequest(request);
        }
        if (builder != null) {
            response = builder.build();
        }

        return response;
    }

    /**
     * Handler for HTTP methods that are not yet implemented.
     *
     * @param request The HTTPRequest representing the request message.
     */
    private static HTTPResponse.Builder handleNYIRequest(HTTPRequest request) {
        logger.info("Building response");
        HTTPResponse.Builder resBuilder = new HTTPResponse.Builder(request.getVersion(), "501", "Not Implemented");

        File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
        formatGeneralHeaders(resBuilder);
        formatContentHeaders(resBuilder, file);
        formatResponseBody(resBuilder, file);

        return resBuilder;
    }

    /**
     * Formats a response to a HEAD request message.
     *
     * @param request The HEAD request message to respond to.
     * @return A Builder object for the response message.
     */
    private static HTTPResponse.Builder handleHEADRequest(HTTPRequest request) {
        return formatMessage(request);
    }

    /**
     * Formats a response to a GET request message.
     *
     * @param request The GET request message to respond to.
     * @return A Builder object for the response message.
     */
    private static HTTPResponse.Builder handleGETRequest(HTTPRequest request) {
        return formatMessage(request);
    }


    /**
     * Formats a response to a POST request message, and handles the request body content.
     *
     * @param request The POST request message to respond to.
     * @return A Builder object for the response message.
     */
    private static HTTPResponse.Builder handlePOSTRequest(HTTPRequest request) {
        HTTPResponse.Builder response = formatMessage(request);

        // Add request body to response message body
        response.appendBodyString("\r\n\r\n");
        response.appendBodyString("Request body is: ");
        response.appendBodyString("\r\n\r\n");
        response.appendBodyString(request.getBody());
        response.appendBodyString("\r\n\r\n");

        switch (request.getUrl().toLowerCase()) {
            case ("/orm-lib"):
                System.out.println("Library API request:");
                System.out.println(request.toString());
                POSTLibraryAPI(response, request);
                break;
            case ("/uservalidation/"):
                POSTUserValidation(response, request);
                break;
            case ("/pokerdistribution/"):
                POSTPoker(response, request);
                break;
            case ("/usertextupload/"):
                // Shouldn't this be a PUT request, not POST..?
                String filePath = null;
                try {
                    filePath = POSTUserTextUpload(request);
                    response.appendBodyString("File upload succeeded. The path is /" + filePath + "\r\n");
                } catch (IOException e) {
                    response.appendBodyString("File upload failed.");
                    logger.info(e.getMessage());
                }
                //String filePath = "did/you/really/think/I/would/enable/indiscriminate/uploading/to/my/server.txt";
                break;
            default:
        }
        // Update response message with correct content-length header
        response.addHeaderField("Content-length", String.valueOf(response.getBodyLength()));

        return response;
    }

    private static void POSTLibraryAPI(HTTPResponse.Builder response, HTTPRequest request) {

        LibDatabase db = LibDatabase.getDatabase();
        String[] reqBodyLines = request.getBody().split("\n");
        boolean success = false;
        if (reqBodyLines.length > 1) {
            switch (reqBodyLines[1]) {
                case ("insertBook"):
                    // Question 1
                    success = db.insertBook(
                            reqBodyLines[2].split("=")[1],
                            reqBodyLines[3].split("=")[1]
                    );
                    if (success) {
                        response.appendBodyString("Book insertion successful.");
                        System.out.println("Added book");
                    } else {
                        response.appendBodyString("Warning: Book insertion failed.");
                    }
                    break;
                case ("updateUser"):
                    // Question 2
                    success = db.updateUser(reqBodyLines[2].split("=")[1],
                            reqBodyLines[3].split("=")[1],
                            reqBodyLines[4].split("=")[1]);
                    if (success) {
                        response.appendBodyString("User details updated..");

                    } else {
                        response.appendBodyString("Warning: Failed to update user details");
                    }
                    break;
                case ("removeBorrowByIds"):
                    // Question 3
                    try {
                        db.removeBorrowByIds(reqBodyLines[2].split("=")[1],
                                reqBodyLines[3].split("=")[1],
                                reqBodyLines[4].split("=")[1]);
                        response.appendBodyString("Book loan instance successfully deleted.");
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        response.appendBodyString("Warning: Failed to delete book loan instance.");
                    }
                    break;
                case ("findBorrowedBooksByTitle"):
                    // Question 4
                    try {
                        ResultSet res = db.findBorrowedBooksByTitle(reqBodyLines[2].split("=")[1]);
                        response.appendBodyString("Results:\n----\n");
                        while (res.next()){
                            response.appendBodyString("Book title: " + res.getString("book_title")+"\n");
                            response.appendBodyString("Name of borrower: " + res.getString("user_name")+"\n");
                            response.appendBodyString("Branch Name: " + res.getString("branch_name")+"\n");
                            response.appendBodyString("\n");

                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    break;
                case ("findBorrowedBooksByDueDateFromBranchName"):
                    // Question 5
                    try {
                        db.findBorrowedBooksByDueDateFromBranchName(
                                reqBodyLines[2].split("=")[1],
                                reqBodyLines[3].split("=")[1]);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    break;
                case ("getNumberOfBorrowsPerBranch"):
                    // Question 6
                    try {
                        db.getNumberOfBorrowsPerBranch();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    break;
            }
        }




    }

    /**
     * Reads POST request message body content into a file.
     *
     * @param request POST request message to read into file.
     * @return the path and name of the created file.
     */
    private static String POSTUserTextUpload(HTTPRequest request) throws IOException {
        String fileName = "/message-" + new Date() + ".txt";
        String filePath = WEB_ROOT + fileName;
        String returnValue = "";
        try {
            File messageFile = new File(filePath);
            if (messageFile.createNewFile()) {
                FileWriter fw = new FileWriter(filePath);
                fw.write(request.getBody());
                fw.close();
                returnValue = ROOT_NAME + fileName;
            } else {
                logger.info("File " + fileName + " somehow already existst... \n" +
                        "Cannot write.");
            }
        } catch (IOException e) {
            throw e;
        }
        return returnValue;
    }

    private static void POSTUserValidation(HTTPResponse.Builder response, HTTPRequest request) {
        response.appendBodyString("Validation results are: \r\n\r\n");
        // Get validation for all usernames in request body.
        ValidUserName
                .validateSeveralNames(request
                        .getBody()
                        .lines()
                        .skip(1) // The first line of request body is the number of usernames, and we don't care.
                        .collect(Collectors // method validateSeveralNames needs ArrayList parameter
                                .toCollection(ArrayList::new)))
                // Append results to response body
                .forEach(s -> response.appendBodyString(s + "\r\n"));
    }

    private static void POSTPoker(HTTPResponse.Builder response, HTTPRequest request) {
        //PokerSend poker = new PokerSend();
        PokerSend poker = new PokerSend();
        String username = request.getBody().split(" ")[2];
        String hand = poker.getPlayerhand(username);
        logger.info("username is: " + username);
        logger.info("Hand is: " + hand);
        response.appendBodyString("Cards are: \r\n\r\n");
        response.appendBodyString(hand);
    }


    /**
     * Creates an HTTPResponse builder object,
     * which has the appropriate status-line, header fields and body content for
     * the given HTTPRequest.
     *
     * @param request HTTPRequest object to create response for
     * @return HTTPResponse message builder object
     */
    private static HTTPResponse.Builder formatMessage(HTTPRequest request) {
        //Minimal response with only status-line
        HTTPResponse.Builder response = createResponseBuilder(request);

        File file;
        if (request.getUrl().equals("/")) {
            file = new File(WEB_ROOT, DEFAULT_FILE);        // Requested file was root, we return server specific default.
        } else if (Files.exists(Path.of(request.getUrl()))) {
            file = new File(WEB_ROOT, request.getUrl());    // Requested file exists
        }
//        else if (request.getMethod().equals("POST")) {
//            file = new File(WEB_ROOT, DEFAULT_FILE);
//            //TODO: 02/03/2021 This needs to go...
//            //  It's literally hardcoded to pass Di's test class
//            //  in regards to the server features we are supposed
//            //  to implement (poker, upload, etc).
//            //  It should be expanded to handle more generalised POST requests
//        }
        else {
            file = new File(WEB_ROOT, FILE_NOT_FOUND);      // Requested file does not exists
        }

        // General headers
        formatGeneralHeaders(response);

        // Content headers
        if (request.getMethod().equalsIgnoreCase("POST")) {
            //POST requests get content from API, not file
            formatPOSTContentHeaders(response);
        } else {
            formatContentHeaders(response, file);

        }
        // Response body, if request was GET
        if (request.getMethod().equalsIgnoreCase("GET")) {
            formatResponseBody(response, file);
        }

        return response;
    }


    /**
     * Creates a response message builder, and formats the status-line of the response based on the request message.
     * The builder object can be expanded with header fields and a body, before being built.
     *
     * @param request HTTP request being responded to.
     * @return A Builder object for an HTTP response, with only the status-line content.
     * Null if object creation fails for any reason.
     */
    private static HTTPResponse.Builder createResponseBuilder(HTTPRequest request) {
        HTTPResponse.Builder builder = null;
        if (request != null) {
            String statusCode = "";
            String reasonPhrase = "";
            HttpStatusCode status;

            // Status-Line
            if (Files.exists(Path.of(request.getUrl())) || request.getMethod().equals("POST")) {
                // 200 OK
                status = HttpStatusCode.getByValue(200);
            } else {
                // 404 File Not Found
                status = HttpStatusCode.getByValue(404);
            }

            statusCode = String.valueOf(status.getValue());
            reasonPhrase = status.getDescription();

            builder = new HTTPResponse
                    .Builder(request.getVersion(), // HTTP-version same as request
                    statusCode,
                    reasonPhrase);
        }
        return builder;
    }

    /**
     * Formats the general headers of an HTTP response.
     *
     * @param response Builder object for response message being formatted.
     */
    private static void formatGeneralHeaders(HTTPResponse.Builder response) {

        response.addHeaderField("Server", "Basic HTTP Server 0.1");
        response.addHeaderField("Date", toServerHTTPTime(new Date().toInstant()));

    }

    /**
     * Formats the content-related header fields for an HTTP response to a POST request.
     * @param response Builder object for response message being formatted.
     */
    private static void formatPOSTContentHeaders(HTTPResponse.Builder response) {
        response.addHeaderField("Content-type", "text/plain");
        // TODO: 10/04/2021 Other POST related headers?

    }

    /**
     * Formats the content-related header fields of an HTTP response, like length and type.
     *
     * @param response Builder object for response message being formatted.
     * @param file     the file holding the content, used for probing values.
     */
    private static void formatContentHeaders(HTTPResponse.Builder response, File file) {
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String filetype = Files.probeContentType(file.toPath());
            String modified = toServerHTTPTime(Files.getLastModifiedTime(file.toPath()).toInstant());
            ;
            logger.info("Content-type from probe: " + filetype);
            response.addHeaderField("Content-length", String.valueOf(file.length()));
            response.addHeaderField("Content-type", filetype);
            response.addHeaderField("Last-Modified", modified);
        } catch (IOException ioe) {
            logger.info(ioe.getMessage());
        }
    }

    /**
     * Adds the content of the requested file to the response message body.
     *
     * @param response Builder for response being formatted.
     * @param file     File requested.
     */
    private static void formatResponseBody(HTTPResponse.Builder response, File file) {
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            logger.info("Adding response body");
            // adding response body from file
            String s = Files.readString(file.toPath());
            response.appendBodyString(s);
        } catch (IOException ioe) {
            logger.info(ioe.getMessage());
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
