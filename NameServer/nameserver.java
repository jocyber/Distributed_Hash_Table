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

    public static String keySpace[] = new String[1024];

    public static void setKeySpace() {
        //set key space
        //keySpace[0] = 0;//position 0 is itself with an id of 0
        for(int i = 0; i < 1024; i++) {
            keySpace[i] = null;
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
                    case "updatePred&Succ":
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
                        break;
                    case "updateSucc":
                        int succId = Integer.parseInt(br.readLine());
                        String succIP = br.readLine();
                        int succPort = Integer.parseInt(br.readLine());
                        Globals.nsi.setSucc(succId, succIP, succPort);
                        break;
                    case "enter":
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

                            // send namespace

                            // send traversal

                            // update predecessor to new Name Server

                        } else {
                            // send to succ
                            /* copy and pasted from bootstrap, not updated
                            Socket succSock = new Socket(Globals.bsi.getSuccIP(), Globals.bsi.getSuccPort());
                            PrintWriter out = new PrintWriter(succSock.getOutputStream());
                            String msg = command + "\n" + Integer.toString(nsID) + "\n" + Integer.toString(nsPort) + "\n" + "0";
                            out.write(msg);
                            out.flush();
                            out.close();
                            succSock.close();
                            */
                        }
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

                System.out.print("Enter a command, bitch> ");
                String input = sc.nextLine();

                switch(input) {
                    case "enter":
                    String cmd = input + "\n" + Globals.id + "\n" + Globals.port + "\n";
                        sendToBootstrap(cmd);
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch(Exception e) {}
                        break;
                    case "print":
                        for(int i = 0; i < Globals.keySpace.length; i++) {
                            if (Globals.keySpace[i] != null) {
                                System.out.println("[" + i + "] " + Globals.keySpace[i]);
                            }
                        }
                        break;
                    case "exit":
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
    }
} // nameserver