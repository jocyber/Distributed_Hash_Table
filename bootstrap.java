import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;

class Task implements Runnable {

    ServerSocket ss;
    Socket sock;
    BufferedReader br;
    PrintStream ps;

    public Task(ServerSocket ss) { // constructor
        this.ss = ss;
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

            pool.shutdown();
        }
        catch(IOException exc) {
            System.err.println(exc);
        }
    }
}
