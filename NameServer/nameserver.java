package NameServer;

import java.net.*;
import java.io.*;
import java.util.Scanner;

import Info.ServerInfo;

class Globals {
    public final static int port = 3000;
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
                //wait for lookup, insert, or delete request
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
        Globals.id = 10;
        //Globals.id = id from file

        try {
            ServerSocket ss = new ServerSocket(Globals.port);
            Thread task = new Task(ss);
            task.start();

            while(true) {
                Scanner sc = new Scanner(System.in);
                System.out.println("Enter a command, bitch> ");
                String input = sc.nextLine();

                if(input.equals("enter")) {
                    Socket sock = new Socket("localhost", 3768);//Use localhost for now. Port is the port given in the config file
                    PrintStream ps = new PrintStream(sock.getOutputStream());

                    ps.println("enter:" + Globals.id);//send enter message along with id
                    //maybe wait for ack so the node knows it's in the system and then update the pred and succ

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