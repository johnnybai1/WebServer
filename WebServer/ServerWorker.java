import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerWorker implements Runnable {

    static int count = 0; // Keep track of how many connections were created

    private Server server;
    private Socket socket;

    boolean sending; // Do not timeout this thread if it is sending a resource
    int idleTime; // track how long it idles for for timeout
    private ExecutorService es; // Execute the timer thread for timeouts
    private TimeoutThread timer;
    private int id; // To verify Keep-alive feature

    public ServerWorker(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.sending = false;
        timer = new TimeoutThread(this);
        es = Executors.newFixedThreadPool(1);
        es.submit(timer);
        count++;
        id = hashCode();
    }

    public Socket socket() {
        return socket;
    }

    @Override
    public void run() {
        try (
            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                String input;
                String request = null;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("GET")) {
                        request = input;
                    }
                    else if (input.equals("")) {
                        break;
                    }
                }
                if (request != null) {
                    sending = true;
                    idleTime = 0;
                    Response response = parseRequest(request);
                    String header = response.buildHeader(server);
                    out.write(header.getBytes());
                    File resource = response.resource;
                    // The below print is to verify Keep-alive feature
                    System.out.println(id + " is sending: " + resource.getName());
                    byte[] fArray = new byte[(int)resource.length()];
                    FileInputStream fis = new FileInputStream(resource);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    bis.read(fArray, 0, fArray.length);
                    out.write(fArray, 0, fArray.length);
                    out.write("\r\n".getBytes());
                    out.flush();
                    sending = false;
                    if (response.version.equals("HTTP/1.0")) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            if (!e.getMessage().equals("Socket closed")) {
                System.out.println("Server worker " + id + ": " + e);
                e.printStackTrace();
                count--;
            }
        }
        count--;
    }

    // Seconds
    public static int getTimeOutTime() {
        if (count <= 10) {
            return 20;
        }
        if (count <= 50) {
            return 10;
        }
        return 5;
    }


    private Response parseRequest(String request) {
        String[] split = request.split(" ");
        String root = server.root();
        if (split.length != 3) {
            return new Response(400, root, "HTTP/1.0");
        }
        String version = split[2];
        if (!version.equals("HTTP/1.0") && !version.equals("HTTP/1.1")) {
            return new Response(505, root, "HTTP/1.0");
        }
        if (!split[0].equals("GET")) {
            return new Response(405, root, version);
        }
        if (split[1].equals("/")) {
            split[1] = "/index.html";
        }
        File resource = new File(root + split[1]);
        if (!resource.exists()) {
            return new Response(404, root, version);
        }
        if (!resource.canRead()) {
            return new Response(403, root, version);
        }
        if (!resource.isFile()) {
            return new Response(415, root, version);
        }
        return new Response(200, resource, version);
    }
}
