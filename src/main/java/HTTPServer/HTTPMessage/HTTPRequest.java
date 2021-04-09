package HTTPServer.HTTPMessage;

import java.util.LinkedHashMap;

/**
 * A simple implementation of an object representing an HTTP request message, following RFC 2616.
 * Contains all data and values from the request, accessible through getter methods.
 *
 * @author Olav Valle
 * @version 280221
 */
public class HTTPRequest {
    private final String method;
    private final String url;
    private final String version;
    private final LinkedHashMap<String, String> headFields;
    private final String body;

    /**
     * Returns the METHOD of this HTTP request.
     * @return the METHOD of this HTTP request.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the URL of the file requested in this HTTP request.
     * @return the URL of the file requested in this HTTP request.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the HTTP version used by this request.
     * Format is "HTTP/x.x".
     * @return the HTTP version used by this request.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns a map of the header fields and their values in this request.
     * Elements are stored in the order they were added to the original map.
     * @return map of header fields and values.
     */
    public LinkedHashMap<String, String> getHeadFields() {
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, String> headCopy = (LinkedHashMap<String, String>) headFields.clone();
        return headCopy;
    }

    /**
     * Returns the content of the body of this HTTP request, as a single string.
     * @return the body of this HTTP request.
     */
    public String getBody() {
        return body;
    }

    private HTTPRequest(Builder builder) {
       this.method = builder.method;
       this.url = builder.url;
       this.version = builder.version;
       this.headFields = builder.headFields;
       this.body = builder.body;
    }

    /**
     * String representation of request, formatted according to RFC 2616.
     * @return String representation of request.
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(method).append(" ").append(url).append(" ").append(version).append(" \n");
        for (String k : headFields.keySet()){
            s.append(k) //field
                    .append(" ")
                    .append(headFields.get(k)) // value
                    .append("\r\n"); //new line
        }
        s.append("\r\n"); //blank new line delimits body start
        s.append(body);

        return s.toString();
    }

    /**
     * Builder class for HTTPRequest objects.
     */
    public static class Builder {
        // Required fields for HTTP request
        private final String method;
        private final String url;
        private final String version;

        // Optional fields for HTTP request
        private final LinkedHashMap<String, String> headFields = new LinkedHashMap<>();
        private String body = "";

        /**
         * Build an HTTPRequest object.
         * Required parameters are a minimal HTTP request,
         * according to RFC 2616:
         * Method SP Request-URI SP HTTP-Version CRLF
         *
         * @param method  HTTP request method name.
         * @param url     URL of requested file.
         * @param version HTTP version used by request.
         */
        public Builder(String method, String url, String version) {
            //TODO: 02/03/2021 make this safer.
            // Throw som param excepts and stuff.
            this.method = method.toUpperCase();
            this.url = url.toLowerCase();
            this.version = version.toUpperCase();
        }

        /**
         * Add field and value to request head.
         * If field is present already, value is updated to new value.
         *
         * @param field Name of field to add.
         * @param value Value of field to add.
         * @return This Builder object.
         */
        public Builder addHeaderField(String field, String value) {
            this.headFields.put(field, value);
            return this;
        }

        /**
         * Append string to request body.
         *
         * @param bodyText String to append to request body.
         * @return This Builder object.
         */
        public Builder appendBodyString(String bodyText) {
            this.body = this.body + bodyText;
            return this;
        }

        /**
         * Builds and returns this HTTP request message object.
         *
         * @return The HTTPRequest object.
         */
        public HTTPRequest build() {
            return new HTTPRequest(this);
        }
    } //Builder

}
