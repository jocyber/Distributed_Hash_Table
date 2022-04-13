package NameServer;

import java.net.*;
import java.io.*;
import java.util.Scanner;

import Info.ServerInfo;

class Globals {
    public final static int port = 3000;//for listening to lookups, etc
    public final static int port2 = 3002; //for user-interaction
    public static ServerInfo nsi = new ServerInfo();
    public static int id;

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
                br.readLine();

                
            }
            catch(IOException ioe) {
                System.err.println(ioe);
            }
            catch(Exception e) {
                System.err.println(e);
            }
        }
    }
}


public class nameserver {
    public static void main(String[] args) {
        Globals.setKeySpace();
        Globals.id = 50;//temporary
        //Globals.id = id from file

        try {
            ServerSocket ss = new ServerSocket(Globals.port);
            Thread task = new Task(ss);
            task.start();

            while(true) {
                Scanner sc = new Scanner(System.in);

                System.out.print("Enter a command, bitch> ");
                String input = sc.nextLine();

                if(input.equals("enter")) {
                    Socket sock = new Socket("localhost", 3768);//Use localhost for now. Port is the port given in the config file
                    PrintStream ps = new PrintStream(sock.getOutputStream());

                    ps.println("enter:" + Globals.id);//send enter message along with id
                    BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                    String resp;
                    
                    while(!((resp = br.readLine()).equals("end"))) {
                        //parse packet and update keyspace array
                        int i = 0;
                        for(;resp.charAt(i) != ':'; i++) {;}

                        int key = Integer.parseInt(resp.substring(0, i));
                        String val = resp.substring(i + 1, resp.length());

                        Globals.keySpace[key] = val;
                    }

                    //temporary for testing
                    for(int i = 0; i < Globals.id; i++) {
                        System.out.println("key: " + i + " value: " + Globals.keySpace[i]);
                    }

                    //update succ and pred
                    //Globals.nsi.setPred(id, IP, port);
                    //Globals.nsi.setSucc(id, IP, port);

                    //print successful entry on completion
                    //print the range of keys for this server
                    //print the id of the pred and succ
                    //print the id's of the servers that were traversed

                    sock.close();//must close for the connection to be accepted on next connection
                }
                else if(input.equals("exit")) {
                    ;//fuckin party or whatever
                }
            }
        }
        catch(IOException ioe) {
            System.err.println(ioe);
        }
        catch(Exception e) {
            System.err.println(e);
        }
    }   
}