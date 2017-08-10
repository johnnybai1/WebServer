import java.io.IOException;

/**
 * Monitors the idle time of a ServerWorker thread and closes the socket
 * the ServerWorker has been idle for too long.
 */
public class TimeoutThread extends Thread {

    ServerWorker worker;

    public TimeoutThread(ServerWorker worker) {
        this.worker = worker;
    }

    public void run() {
        boolean running = true;
        while (running) {
            try {
                long start = System.currentTimeMillis();
                if (!worker.sending) {
                    Thread.sleep(500); // Ping every 500 ms
                    long current = System.currentTimeMillis();
                    worker.idleTime = worker.idleTime +
                            (int) (current - start);
                    if (ServerWorker.getTimeOutTime() * 1000 <= worker.idleTime) {
                        try {
                            worker.socket().close();
                            running = false;
                            Thread.currentThread().interrupt();
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                    }
                }
                else worker.idleTime = 0; // Not idle, set idle time to 0
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
    }
}
