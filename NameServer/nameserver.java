package NameServer;

import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.GroupLayout;

import Info.ServerInfo;

class Globals {
    //public final static int port2 = 3002; //for user-interaction
    public static ServerInfo nsi = new ServerInfo();
    public static int id;
    public  static int port;
    public static String bootstrapIP;
    public static int bootstrapPort;
    public static Object lock = new Object();

    public static String keySpace[] = new String[1024];

    public static void setKeySpace() {
        //set key space
        //keySpace[0] = 0;//position 0 is itself with an id of 0
        for(int i = 0; i < 1024; i++) {
            keySpace[i] = null;
        }
    }

    public static void staticWait() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (Exception e) {}
        }    
    }

    public static void staticNotify() {
        synchronized (lock) {
            lock.notify();
        }
    }
}

class Task extends Thread {
    ServerSocket ss;
    Socket sock;

    public Task(ServerSocket ss) {
        this.ss = ss;
    }

    @Override
    public void run() {
        while(true) {
            try {
                //wait for lookup, insert, or delete request. as well as enter and exit
                sock = ss.accept();
                BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String command = br.readLine();

                switch(command) {
                    case "updatePred&Succ": // used when adding a new Name Server
                        // update pred and succ
                        updatePredSucc(br, sock);
                        // update keyspace
                        updateKeyspace(br, sock);
                        // get traversal
                        String traversal = br.readLine();
                        // print data to user
                        System.out.println("KeySpace: " + Globals.nsi.getStartingRange() + " -> " + Globals.nsi.getEndingRange());
                        System.out.println("Predecessor: " + Globals.nsi.getPredID() + " Successor: " + Globals.nsi.getSuccID());
                        System.out.println("Path: " + traversal + "\n");
                        Globals.staticNotify();
                        break;
                    case "updateSucc": // used when adding a new Name Server and deleting
                        int succId = Integer.parseInt(br.readLine());
                        String succIP = br.readLine();
                        int succPort = Integer.parseInt(br.readLine());
                        Globals.nsi.setSucc(succId, succIP, succPort);
                        break;
                    case "enter": // used when adding a new Name server
                        // get id, IP, and port of Name Server and traversal
                        int nsID = Integer.parseInt(br.readLine()); // nameserver id
                        int nsPort = Integer.parseInt(br.readLine());
                        String nsIP = (((InetSocketAddress) sock.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
                        String trav = br.readLine();
                        
                        // if in range handle, else send to succ
                        if(nsID >= Globals.nsi.getStartingRange() && nsID < Globals.id) {
                            // change old predecessors succesor to new name server
                            Socket nsSock = new Socket(Globals.nsi.getPredIP(), Globals.nsi.getPredPort());
                            PrintWriter out = new PrintWriter(nsSock.getOutputStream());
                            String updateSucc = "updateSucc\n" + nsID + "\n" + nsIP + "\n" + nsPort + "\n";
                            out.write(updateSucc);
                            out.flush();
                            out.close();
                            nsSock.close();

                            // send Pred&Succ to new NameServer, namespace, and traversal
                            // update predecessor and succesor of new name space
                            String updateNS = "updatePred&Succ\n" + Globals.nsi.getPredID() + "\n" +
                                    Globals.nsi.getPredIP() + "\n" + Globals.nsi.getPredPort() + "\n" +
                                    Globals.id + "\n" + "bootstrap" + "\n" + 
                                    Globals.port + "\n";
                            nsSock = new Socket(nsIP, nsPort);
                            out = new PrintWriter(nsSock.getOutputStream());
                            out.write(updateNS);
                            out.flush();

                            // send key space
                            String keyspaceData = "";
                            for(int i = Globals.nsi.getPredID()+1; i < nsID; i++) { // update
                                if(Globals.keySpace[i] != null) {
                                    keyspaceData += i + " " + Globals.keySpace[i] + " ";
                                    Globals.keySpace[i] = null;
                                }
                            }
                            keyspaceData += "\n";
                            out.write(keyspaceData);
                            out.flush();

                            // send traversal
                            trav += " -> " + Globals.id;
                            out.print(trav);
                            out.flush();

                            out.close();
                            nsSock.close();

                            // update predecessor to new Name Server
                            Globals.nsi.setPred(nsID, nsIP, nsPort);

                        } else {
                            // send to succ
                            Socket succSock = new Socket(Globals.nsi.getSuccIP(), Globals.nsi.getSuccPort());
                            PrintWriter out = new PrintWriter(succSock.getOutputStream());
                            String msg = command + "\n" + Integer.toString(nsID) + "\n" + Integer.toString(nsPort) + "\n" + trav + " -> " + Globals.id;
                            out.write(msg);
                            out.flush();
                            out.close();
                            succSock.close();
                        }
                        break;
                    case "exit": // used when exiting a name server
                        // update predecessor
                        int id = Integer.parseInt(br.readLine());
                        String ip = br.readLine();
                        int port = Integer.parseInt(br.readLine());
                        Globals.nsi.setPred(id, ip, port);

                        // update keyspace
                        updateKeyspace(br, sock);

                        break;
                }
                br.close();
                sock.close();
            }
            catch(IOException ioe) {
                System.err.println(ioe);
            }
            catch(Exception e) {
                System.err.println(e);
            }
        }
    }
    public void updatePredSucc(BufferedReader br, Socket sock) {
        try {
            // set pred
            int id = Integer.parseInt(br.readLine());
            String IP = br.readLine();
            if(IP.equals("bootstrap")) {
                IP = Globals.bootstrapIP;
            }
            int port = Integer.parseInt(br.readLine());
            Globals.nsi.setPred(id, IP, port);

            // set succ
            id = Integer.parseInt(br.readLine());
            IP = br.readLine();
            if(IP.equals("bootstrap")) {
                IP = Globals.bootstrapIP;
            }
            port = Integer.parseInt(br.readLine());
            Globals.nsi.setSucc(id, IP, port);
        } catch(Exception e) {
            System.out.println("Error updating Pred and Succ");
        }
    } // updatePredSucc

    public void updateKeyspace(BufferedReader br, Socket sock) {
        try {
            String msg = br.readLine();
            String[] parse = msg.split("\\s+");
            int key;
            String data;
            for(int i = 0; i < parse.length; i +=2) {
                key = Integer.parseInt(parse[i]);
                data = parse[i+1];
                Globals.keySpace[key] = data;
            }
        } catch(Exception e) {
            System.err.println("Error updating keyspace.");
        }
    } // updateKeySpace
}


public class nameserver {
    public static void main(String[] args) {
        Globals.setKeySpace();

        try {
            // read data from file
            File inputFile = new File(args[0]);
            Scanner inputScan = new Scanner(inputFile);
            Globals.id = inputScan.nextInt();
            Globals.nsi.setID(Globals.id);
            Globals.port = inputScan.nextInt();
            Globals.bootstrapIP = inputScan.next();
            Globals.bootstrapPort = inputScan.nextInt();
            inputScan.close();

            // create server socket
            ServerSocket ss = new ServerSocket(Globals.port);
            Thread task = new Task(ss);
            task.start();

            while(true) {
                Scanner sc = new Scanner(System.in);

                System.out.print("Enter a command> ");
                String input = sc.nextLine();

                switch(input) {
                    case "enter":
                        String cmd = input + "\n" + Globals.id + "\n" + Globals.port + "\n";
                        sendToBootstrap(cmd);
                        Globals.staticWait();
                        break;
                    case "print":
                        System.out.println("Range: " + Globals.nsi.getStartingRange() + " -> " + Globals.nsi.getEndingRange());
                        Globals.nsi.printPred();
                        Globals.nsi.printSucc();
                        for(int i = 0; i < Globals.keySpace.length; i++) {
                            if (Globals.keySpace[i] != null) {
                                System.out.println("[" + i + "] " + Globals.keySpace[i]);
                            }
                        }
                        break;
                    case "exit":
                        // send exit command to successor
                        exitSystem();
                        break;
                    default:
                        continue;
                } // switch
            }
        }
        catch(IOException ioe) {
            System.err.println(ioe);
        }
        catch(Exception e) {
            System.err.println(e);
        }
    } // main

    public static void sendToBootstrap(String command) {
        try {
            Socket bootSock = new Socket(Globals.bootstrapIP, Globals.bootstrapPort);
            PrintWriter out = new PrintWriter(bootSock.getOutputStream());
            out.print(command);
            out.close();
            bootSock.close();
        } catch(Exception e) {
            System.err.println("Socket creation to bootstrap failed");
            System.exit(0);
        }
    } // sendToBootstrap

    public static void exitSystem() {
        try {
            // send command to successor
            Socket sock = new Socket(Globals.nsi.getSuccIP(), Globals.nsi.getSuccPort());
            PrintWriter out = new PrintWriter(sock.getOutputStream());
            out.write("exit\n");
            out.flush();

            // send updated predecessor to succesor
            String msg = Globals.nsi.getPredID() + "\n" + Globals.nsi.getPredIP() + "\n" + Globals.nsi.getPredPort() + "\n";
            out.write(msg);
            out.flush();

            // send over keySpace
            String keyspace = "";
            for(int i = Globals.nsi.getPredID()+1; i <= Globals.id; i++) { // update
                if(Globals.keySpace[i] != null) {
                    keyspace += i + " " + Globals.keySpace[i] + " ";
                    Globals.keySpace[i] = null;
                }
            }
            out.write(keyspace);
            out.flush();
            out.close();
            sock.close();

            // send command to predecessor
            sock = new Socket(Globals.nsi.getPredIP(), Globals.nsi.getPredPort());
            out = new PrintWriter(sock.getOutputStream());
            out.write("updateSucc\n");
            out.flush();
            String msg2 = Globals.nsi.getSuccID() + "\n" + Globals.nsi.getSuccIP() + "\n" + Globals.nsi.getSuccPort() + "\n";
            out.write(msg2);
            out.flush();
            out.close();
            sock.close();
            System.out.println("Succesful exit, handed over keyspace " + Globals.nsi.getStartingRange() + " -> " + Globals.id + " to NameServer " + Globals.nsi.getSuccID() + "\n");
        } catch(Exception e) {
            System.out.println("Error exiting from system");
        }

    } // exitSystem
} // nameserver