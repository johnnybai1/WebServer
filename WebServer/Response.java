import java.io.*;
import java.util.Date;
import java.util.StringJoiner;

/**
 * The Response class stores all the necessary information for the Server
 * to send an appropriate response to a client's request. This class stores
 * header information as well as the appropriate resource (either the requested
 * resource or an .html error page)
 */
public class Response {

    int code; // status code
    File resource; // requested resource (or .html error page)
    String version; // HTTP Version to respond in

    public Response(int code, File resource, String version) {
        this.code = code;
        this.resource = resource;
        this.version = version;
    }

    public Response(int code, String root, String version) {
        this(code, new File(root + "/" + code + ".html"), version);
    }

    public void sendHeader(OutputStream out, Server server) throws IOException {
        out.write(buildHeader(server).getBytes());
        out.flush();
    }

    public void sendResource(OutputStream out) throws IOException {
        byte[] fArray = new byte[(int)resource.length()];
        FileInputStream fis = new FileInputStream(resource);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(fArray, 0, fArray.length);
        out.write(fArray, 0, fArray.length);
        out.write("\r\n\r\n".getBytes());
        out.flush();
    }

    private String getContentType() {
        String name = resource.getName();
        if (name.endsWith(".html")) {
            return "text/html";
        }
        if (name.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".gif")) {
            return "image/jpeg";
        }
        return "plain/text";
    }

    public String buildHeader(Server server) {
        StringJoiner sj = new StringJoiner("\r\n");
        String status = version + " " + code + " " +
                Server.statusCodes.get(code);
        sj.add(status);

        String connection = "Connection: close";
        String timeout = null;
        if (version.contains("1.1")) {
            String host = "Host: localhost:" + server.port();
            sj.add(host);
            connection = "Connection: keep-alive";
            timeout = "Keep-alive: timeout=" + ServerWorker.getTimeOutTime();
        }
        String date = "Date: " + Server.sdf.format(new Date());
        sj.add(date);
        String type = "Content-Type: " + getContentType();
        sj.add(type);
        String length = "Content-Length: " + (int) resource.length();
        sj.add(length);
        if (timeout != null) {
            sj.add(timeout);
        }
        sj.add(connection);
        return sj.toString() + "\r\n\r\n";
    }

}
