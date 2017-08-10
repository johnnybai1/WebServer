import java.io.IOException;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple server to handle HTTP 1.0 requests
 */
public class Server {

    // Date format the server will use
    static final SimpleDateFormat sdf = new SimpleDateFormat(
            "EEE dd MMM yyyy HH:mm:ss z");
    static final HashMap<Integer, String> statusCodes =
            new HashMap<Integer, String>() {
                {
                    put(200, "OK");
                    put(400, "Bad Request");
                    put(403, "Forbidden");
                    put(404, "Not Found");
                    put(405, "Method Not Allowed");
                    put(415, "Unsupported Media Type");
                    put(505, "HTTP Version Not Supported");
                }
            };

    private String root; // document root
    private int port; // port this server listens to
    private ServerSocket serverSocket; // this server's socket

    private ExecutorService threadPool;

    public Server(String root, int port) {
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        this.root = root;
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(e);
        }
        threadPool = Executors.newCachedThreadPool();
    }

    public String root() {
        return root;
    }

    public int port() {
        return port;
    }

    public ServerSocket serverSocket() {
        return serverSocket;
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java Server " +
                    "-document_root [\"/path/to/root\"] -port [port number])");
            System.exit(1);
        }
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(args[0], args[1]);
        parameters.put(args[2], args[3]);
        String root = parameters.get("-document_root");
        int port = Integer.parseInt(parameters.get("-port"));

        Server server = new Server(root, port);
        while(true) {
            try {
                server.threadPool.submit(new ServerWorker(server, server.serverSocket.accept()));
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }
    }

}
