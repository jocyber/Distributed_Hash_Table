package Bootstrap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.*;
import java.util.Scanner;

import Info.ServerInfo;

//class of global variables
class Globals {
    // automatically sets pred and succ to itself
    public static ServerInfo bsi = new ServerInfo();
    public static int port;//for thread pool
    public final static int port2 = 2003;//for user-interaction thread
    public static int id;

    public static String keySpace[] = new String[1024];//hash value is already given, so make an array instead and the id will be the position
    //doesn't need to be resized when new nodes are added since the range will be checked first.
    //when a node is removed or added, you can simply update the values back to -1 that are not within the range, if necessary

    public static void setKeySpace() {
        //set key space
        //keySpace[0] = 0;//position 0 is itself with an id of 0
        for(int i = 0; i < 1024; i++) {
            keySpace[i] = null;
        }
    }

    public static boolean sendToSucc(int key) throws IOException {
        // if key not found and there's another node in the system, send to successor
       Socket succ_sock = new Socket(bsi.getSuccIP(), bsi.getSuccPort());
       PrintStream ps = new PrintStream(succ_sock.getOutputStream());

       //will send two packets. nameservers need to expect two so keeping track of node path and key is easier
       ps.println(key + ":0/");

       //create a server socket here and wait for oncoming connections from the name servers
       ServerSocket ss = new ServerSocket(Globals.port2);
       Socket ack_sock;

       boolean flag = false;

       ack_sock = ss.accept();
       BufferedReader br = new BufferedReader(new InputStreamReader(ack_sock.getInputStream()));
       String ack = br.readLine();

       if(ack.equals("NF")) {
           System.out.println("Key not found.");
       }
       else if(ack.substring(0, 2).equals("F:")) {// if found, F:path/to/key
           System.out.println(key + ':' + ack.substring(2, ack.length()));//key:path/to/key
           flag = true;
       }

       ack_sock.close();
       succ_sock.close();
       ss.close();

       return flag;
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
                
                //read the command from the socket
                String command = br.readLine();

                // switch on command
                switch(command) {
                    case "updateSucc":
                        int succId = Integer.parseInt(br.readLine());
                        String succIP = br.readLine();
                        int succPort = Integer.parseInt(br.readLine());
                        Globals.bsi.setSucc(succId, succIP, succPort);
                        break;
                    case "enter":
                        // get id and port of Name Server
                        int nsID = Integer.parseInt(br.readLine()); // nameserver id
                        int nsPort = Integer.parseInt(br.readLine());
                        // add ip and port to string
                        String nsIP = (((InetSocketAddress) sock.getRemoteSocketAddress()).getAddress()).toString().replace("/","");

                        // if the only server
                        if(Globals.bsi.isOnlyServer()) {
                            // change predecessors and succesors
                            Globals.bsi.setPred(nsID, nsIP, nsPort);
                            Globals.bsi.setSucc(nsID, nsIP, nsPort);

                            // update predecessor and succesor of new name space
                            String updateNS = "updatePred&Succ\n" + Globals.id + "\n" + "bootstrap\n" + Globals.port + "\n" +
                                Globals.id + "\n" + "bootstrap\n" + Globals.port + "\n";
                            Socket nsSock = new Socket(nsIP, nsPort);
                            PrintWriter out = new PrintWriter(nsSock.getOutputStream());
                            out.write(updateNS);
                            out.flush();

                            // send over name space
                            String keyspaceData = "";
                            for(int i = 0; i < Globals.bsi.getStartingRange(); i++) {
                                if(Globals.keySpace[i] != null) {
                                    keyspaceData += i + " " + Globals.keySpace[i] + " ";
                                    Globals.keySpace[i] = null;
                                }
                            }
                            keyspaceData += "\n";
                            out.write(keyspaceData);
                            out.flush();

                            // send over the conformation, append path taken
                            String traversal = "0\n";
                            out.print(traversal);
                            out.flush();

                            out.close();
                            nsSock.close();
                            Globals.bsi.setOnlyServer(false);
                        } else {
                            // if in range, else send to succesor
                            if(nsID >= Globals.bsi.getStartingRange() && nsID <= 1023) {
                                // change old predecessors succesor to new name server
                                Socket nsSock = new Socket(Globals.bsi.getPredIP(), Globals.bsi.getPredPort());
                                PrintWriter out = new PrintWriter(nsSock.getOutputStream());
                                String updateSucc = "updateSucc\n" + nsID + "\n" + nsIP + "\n" + nsPort + "\n";
                                out.write(updateSucc);
                                out.flush();
                                out.close();
                                nsSock.close();

                                // send Pred&Succ to new NameServer, namespace, and traversal
                                // update predecessor and succesor of new name space
                                String updateNS = "updatePred&Succ\n" + Globals.bsi.getPredID() + "\n" +
                                    Globals.bsi.getPredIP() + "\n" + Globals.bsi.getPredPort() + "\n" +
                                    Globals.id + "\n" + "bootstrap" + "\n" + 
                                    Globals.port + "\n";
                                nsSock = new Socket(nsIP, nsPort);
                                out = new PrintWriter(nsSock.getOutputStream());
                                out.write(updateNS);
                                out.flush();

                                // send namespace
                                String keyspaceData = "";
                                for(int i = Globals.bsi.getPredID()+1; i < nsID; i++) { // update
                                    if(Globals.keySpace[i] != null) {
                                        keyspaceData += i + " " + Globals.keySpace[i] + " ";
                                        Globals.keySpace[i] = null;
                                    }
                                }
                                keyspaceData += "\n";
                                out.write(keyspaceData);
                                out.flush();

                                // send traversal
                                String traversal = "0\n";
                                out.print(traversal);
                                out.flush();

                                out.close();
                                nsSock.close();

                                // update bootstrap Predecessor to new nameserver
                                Globals.bsi.setPred(nsID, nsIP, nsPort);

                            } else {
                                // send to succ, restructure command and append path taken
                                Socket succSock = new Socket(Globals.bsi.getSuccIP(), Globals.bsi.getSuccPort());
                                PrintWriter out = new PrintWriter(succSock.getOutputStream());
                                String msg = command + "\n" + Integer.toString(nsID) + "\n" + Integer.toString(nsPort) + "\n" + "0";
                                out.write(msg);
                                out.flush();
                                out.close();
                                succSock.close();
                            }
                        }
                        break;
                } // switch
                sock.close();
            }
            catch(NullPointerException npe) {
                System.err.println("Null pointer in thread pool.");
                System.err.println(npe);
            }
            catch(NumberFormatException nfe) {
                System.err.println("Couldn't parse id in thread pool.");
                System.err.println(nfe);
            }
            catch(StringIndexOutOfBoundsException sio) {
                System.err.println("String out of bound error in thread pool.");
                System.err.println(sio);
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
}

//main class
public class bootstrap {
    public static void main(String[] args) {
        final int threadCount = 10;
        Globals.setKeySpace();//initialize keyspace array

        // initialize from input file
        try {
            File inputFile = new File(args[0]);
            Scanner inputScan = new Scanner(inputFile);
            Globals.id = inputScan.nextInt();
            Globals.port = inputScan.nextInt();
  
            int index;
            String data;

            while(inputScan.hasNext()) {
                index = inputScan.nextInt();
                data = inputScan.next();
                Globals.keySpace[index] = data;
            }

            inputScan.close();
        } 
        catch (FileNotFoundException fe) {
            System.err.println("Input file [" + args[0] + "] not found.");
            System.exit(1);
        }

        try {
            ServerSocket ss = new ServerSocket(Globals.port);
            //ServerSocket ack_ss = new ServerSocket(Globals.port2);//wait for ACK
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);//thread pool class

            //execute 10 threads in thread pool
            for(int i = 0; i < threadCount; i++)
                pool.execute(new Task(ss));

            //user interation thread
            Scanner sc = new Scanner(System.in);

            while(true) {
                System.out.print("Enter a command> ");
                String input = sc.nextLine();

                //parse the command
                int i = 0;
                for(; i < input.length() && input.charAt(i) != ' '; i++) {}
                String command = input.substring(0, i);

                //parse the key
                String keyS = ""; i++;
                while(i < input.length() && input.charAt(i) != ' ') {keyS += input.charAt(i); i++;}

                //parse the value
                String value = ""; i++;
                while(i < input.length() && input.charAt(i) != ' ') {value += input.charAt(i); i++;}

                // find if the key is inRange
                boolean inRange = true;
                int key = 0;

                if(keyS.length() > 0) {
                    key = Integer.parseInt(keyS);

                    if(key > 1023 | key < 0) {
                        System.out.println("Key out of range: [0-1023]");
                        continue;
                    }

                    if(Globals.bsi.isOnlyServer()) {
                        inRange = true;
                    } 
                    else {
                        inRange = key >= Globals.bsi.getStartingRange() && key <= 1023;
                        if(key == 0) {
                            inRange = true;
                        }
                    }
                }

                switch(command) {
                    case "lookup":
                        // if in keyspace print, if not send to succesor
                        if(inRange) {
                            if(Globals.keySpace[key] == null) {
                                System.out.println("Key not found.");
                            } 
                            else {
                                System.out.println(Globals.keySpace[key] + ":0/");
                            }
                        } 
                        else {
                            Globals.sendToSucc(key);
                        }

                        break;

                    case "insert":
                        // if in keyspace, enter into table, if not send to succesor
                        if(inRange) {
                            System.out.println(key + ":0/");
                            Globals.keySpace[key] = value;
                        } 
                        else {
                            Globals.sendToSucc(key);
                        }
                        
                        break;

                    case "delete":
                        // if in keyspace, enter into table, if not, send to succesor
                        if(inRange) {
                            if(Globals.keySpace[key] == null) {
                                System.out.println("Key not found.");
                            } 
                            else {
                                System.out.println(key + ":0/\nSuccessful deletion.");
                                Globals.keySpace[key] = null;
                            }
                        } 
                        else {
                            if(Globals.sendToSucc(key))
                                System.out.println("Successful deletion.");
                        }
                        
                        break;

                    case "print":
                        System.out.println("ID 0:");

                        for(int j = 0; j <= 1023; j++) {
                            if(Globals.keySpace[j] != null) {
                                System.out.println("\t[" + j + "] " + Globals.keySpace[j]);
                            }
                        }

                        if(!Globals.bsi.isOnlyServer()) {
                            ;//send to successor to print the rest of the list
                        } 
                        
                        break;

                    default:
                        System.err.println("Command not recognized.");
                }
            }
        }
        catch(StringIndexOutOfBoundsException sio) {
            System.err.println("String out of bound error in user-interaction.");
            System.err.println(sio);
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

