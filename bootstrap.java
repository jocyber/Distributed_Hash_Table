import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscriber;
import java.io.*;
import java.net.*;

class Task implements Runnable {

    private ServerSocket ss;
    private Socket sock;
    private BufferedReader br;
    private PrintStream ps;

    //class to modify and receieve pred and succ
    private BootStrapInfo bsi = new BootStrapInfo(); // automatically sets pred and succ to itself
    int keySpace[] = new int[1024];//hash value is already given, so make an array instead and the id will be the position
    //doesn't need to be resized when new nodes are added since the range will be checked first.
    //when a node is removed or added, you can simply update the values back to -1 that are not within the range, if necessary

    public Task(ServerSocket ss) { // constructor
        this.ss = ss;

        //set key space
        keySpace[0] = 0;//position 0 is itself with an id of 0
        for(int i = 1; i < 1024; i++) {
            keySpace[i] = -1;
        }
    }

    @Override
    public void run() { // method to execute within thread
        while(true) {
            try {
                sock = ss.accept(); //wait for connection

                br = new BufferedReader(new InputStreamReader(sock.getInputStream()));//used to read from socket
                ps = new PrintStream(sock.getOutputStream());//used to write to socket

                String msg;
                
                //read the information from the socket
                do {
                    msg = br.readLine();
                    //ps.println(msg);//send to the socket
                    System.out.println(msg);

                }while(!msg.equals("end"));
            }
            catch(Exception ex) { //should go last when other exceptions are required
                System.err.println(ex);
            }
        }
    }
}

//main class
public class bootstrap {
    public static void main(String[] args) {
        final int port = 2002;
        final int threadCount = 10;

        try {
            ServerSocket ss = new ServerSocket(port);
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);//thread pool class

            //execute 10 threads in thread pool
            for(int i = 0; i < threadCount; i++)
                pool.execute(new Task(ss));

        }
        catch(IOException exc) {
            System.err.println(exc);
        }
        catch(Exception ex) {
            System.err.println(ex);
        }
    }
}
