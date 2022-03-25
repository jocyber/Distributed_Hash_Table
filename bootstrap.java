import java.util.*;
import java.io.*;
import java.net.*;

public class bootstrap {
    public static void main(String[] args) {
        int port = 2002;

        try {
            ServerSocket ss = new ServerSocket(port);
            //client: Socket cs = new Socket("ip", port);
            //everything else is the same with the exclusion of the sock object
        }
        catch(IOException exc) {
            System.err.println(exc);
        }

        Socket sock;
        BufferedReader br;
        PrintStream ps;

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
