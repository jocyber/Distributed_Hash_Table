package Bootstrap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.util.Scanner;

//class of global variables
class Globals {
    // automatically sets pred and succ to itself
    public static BootStrapInfo bsi = new BootStrapInfo();
    public final static int port = 2002;

    public static int keySpace[] = new int[1024];//hash value is already given, so make an array instead and the id will be the position
    //doesn't need to be resized when new nodes are added since the range will be checked first.
    //when a node is removed or added, you can simply update the values back to -1 that are not within the range, if necessary

    public Globals() {
        //set key space
        keySpace[0] = 0;//position 0 is itself with an id of 0
        for(int i = 1; i < 1024; i++) {
            keySpace[i] = -1;
        }
    }
}

class Task implements Runnable {

    private ServerSocket ss;
    private Socket sock;
    private BufferedReader br;
    private PrintStream ps;

    public Task(ServerSocket ss) { // constructor
        this.ss = ss;
    }

    @Override
    public void run() { // method to execute within thread
        while(true) {
            try {
                sock = ss.accept(); //wait for connection

                br = new BufferedReader(new InputStreamReader(sock.getInputStream()));//used to read from socket
                
                //read the information from the socket
                String msg = br.readLine();
                int key = Integer.parseInt(msg);
                //assumes key is the only input (will be different for name servers)
                //will need to be modified for when nodes want to be added/deleted

                //if in key space 
                if(key >= Globals.bsi.getStartingRange() && key <= Globals.bsi.getEndingRange()) {
                    Globals.keySpace[key] = key;
                }
                else { // if not, send to successor
                    Socket succ_sock = new Socket(Globals.bsi.getSuccIP(), Globals.bsi.getSuccPort());
                    ps = new PrintStream(succ_sock.getOutputStream());

                    ps.println(key);
                    succ_sock.close();
                }

                sock.close();
            }
            catch(SocketException se) {
                System.err.println("Error with the socket.");
                System.err.println(se);
            }
            catch(IOException ioe) {
                System.err.println("Error with I/O.");
                System.err.println(ioe);
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
        final int threadCount = 10;
        Globals gb = new Globals();//initialize keyspace array

        try {
            ServerSocket ss = new ServerSocket(Globals.port);
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);//thread pool class

            //execute 10 threads in thread pool
            for(int i = 0; i < threadCount; i++)
                pool.execute(new Task(ss));

            //user interation thread
            Scanner sc = new Scanner(System.in);
            Socket sock = new Socket("localhost", Globals.port);

            while(true) {
                System.out.print("Enter a command> ");
                String input = sc.nextLine();

                int i = 0;
                for(; i < input.length() && input.charAt(i) != ' '; i++) {}
                String command = input.substring(0, i);

                switch(command) {
                    case "lookup":
                        PrintStream ps = new PrintStream(sock.getOutputStream());
                        String key = input.substring(i + 1, input.length());

                        //send packet to localhost to start the key space search
                        ps.println(key); 
                        //maybe add ACK here to gaurantee the print statements don't overlap each other
                        //if so, then change the port number for sock in this context
                        break;

                    case "insert":
                        break;

                    case "delete":
                        break;

                    default:
                        System.err.println("Command not recognized.");
                }
            }
        }
        catch(SocketException se) {
            System.err.println(se);
        }
        catch(IOException exc) {
            System.err.println(exc);
        }
        catch(Exception ex) {
            System.err.println(ex);
        }
    }
}
