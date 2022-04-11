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
    public final static int port = 2002;//for thread pool
    public final static int port2 = 2003;//for user-interaction thread

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

                /*
                ----------------------------------------------
                also need to handle enter/exit of a nameserver
                ----------------------------------------------
                */

                //if key is not found in the last nameserver, then it will send a packet to the
                //bootstrap with the 'NF' message for 'Not Found.'
                if(msg.equals("NF")) {
                    sendACK("Key not found.");
                }
                else if(msg.substring(0, 2).equals("F:")) {// when node contacts bootstrap of found key
                    //packet format when key found: "F:sequence/of/nodes/to/key"
                    sendACK(msg.substring(2, msg.length()));
                }
                else {
                    //parse packet for specific command
                    int i = 0;
                    for(; i < msg.length() && msg.charAt(i) != ':'; i++) {}
                    String command = msg.substring(0, i);

                    int key = Integer.parseInt(msg.substring(i + 1, msg.length()));

                    switch(command) {
                        case "lookup":
                            //if in key space 
                            if(key >= Globals.bsi.getStartingRange() && key <= Globals.bsi.getEndingRange()) {
                                sendACK("0/" + key);//path to key 
                            }
                            else {
                                sendToSucc(key);
                            }

                            break;
                    }
                }

                sock.close();
            }
            catch(IOException ioe) {
                System.err.println("Error with the socket.");
                System.err.println(ioe);
            }
            catch(Exception ex) { //should go last when other exceptions are required
                System.err.println(ex);
            }
        }
    }

    private void sendToSucc(int key) throws IOException {
        if(Globals.bsi.getSuccID() == 0) { //if bootstrap is the only node in the system
            sendACK("Key not found.");
        }
        else { // if key not found and there's another node in the system, send to successor
            Socket succ_sock = new Socket(Globals.bsi.getSuccIP(), Globals.bsi.getSuccPort());
            ps = new PrintStream(succ_sock.getOutputStream());

            //will send two packets. nameservers need to expect two so keeping track of node path and key is easier
            ps.println("0/");
            ps.println(key);
            succ_sock.close();
        }
    }

    private void sendACK(String message) throws IOException {
        Socket ack_sock = new Socket("localhost", Globals.port2);
        ps = new PrintStream(ack_sock.getOutputStream());

        ps.println(message);
        ack_sock.close();
    }
}

//main class
public class bootstrap {
    public static void main(String[] args) {
        final int threadCount = 10;
        Globals gb = new Globals();//initialize keyspace array

        try {
            ServerSocket ss = new ServerSocket(Globals.port);
            ServerSocket ack_ss = new ServerSocket(Globals.port2);//wait for ACK
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);//thread pool class

            //execute 10 threads in thread pool
            for(int i = 0; i < threadCount; i++)
                pool.execute(new Task(ss));

            //user interation thread
            Scanner sc = new Scanner(System.in);
            Socket ack_sock;

            while(true) {
                //sock from thread pool
                //have to establish each time because of accept()
                Socket sock = new Socket("localhost", Globals.port);
                PrintStream ps = new PrintStream(sock.getOutputStream());

                System.out.print("Enter a command> ");
                String input = sc.nextLine();

                //parse the command
                int i = 0;
                for(; i < input.length() && input.charAt(i) != ' '; i++) {}
                String command = input.substring(0, i);

                String key = "";
                if(i < input.length())
                    key = input.substring(i + 1, input.length());

                boolean flag = false;//only want to recieve ACK if command was valid

                switch(command) {
                    case "lookup":
                        //send packet to localhost to start the key space search
                        ps.println("lookup:" + key); 
                        break;

                    case "insert":
                        ps.println("insert:" + key); 
                        break;

                    case "delete":
                        ps.println("delete:" + key); 
                        break;

                    default:
                        System.err.println("Command not recognized.");
                        flag = true;
                }

                if(!flag) {
                    //ACK from the thread pool so prints don't overlap
                    //block until response is received
                    ack_sock = ack_ss.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(ack_sock.getInputStream()));

                    System.out.println(br.readLine());
                    ack_sock.close();
                }

                sock.close();
            }
        }
        catch(BindException be) {
            System.err.println("ServerSocket already binded.");
        }
        catch(IOException exc) {
            System.err.println(exc);
        }
        catch(Exception ex) {
            System.err.println(ex);
        }
    }
}
