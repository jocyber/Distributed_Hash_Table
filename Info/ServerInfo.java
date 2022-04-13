package Info;

//information 'struct'
public class ServerInfo {
    private int id = 0;

    //mx = current node
    //key space range = [H(pred(mx)) + 1, H(mx)]
    //successor & predecessor info class
    class NameServer {
        public int id;
        public String IP;
        public int port;

        public NameServer(int id, String IP, int port) {
            this.id = id;
            this.IP = IP;
            this.port = port;
        }
    }

    //pred and succ start out as itself
    //private NameServer pred = new NameServer(0, "127.0.0.1", Globals.port);
    //private NameServer succ = new NameServer(0, "127.0.0.1", Globals.port);
    private NameServer pred = null;
    private NameServer succ = null;

    // see if the bootstrap is the only server
    public boolean isOnlyServer() {
        return pred == null && succ == null;
    }

    //get key space ranges
    public int getStartingRange() {
        return pred.id + 1;
    }

    public int getEndingRange() {
        return id;
    }

    //get IPs
    public String getPredIP() {
        return pred.IP;
    }

    public String getSuccIP() {
        return succ.IP;
    }

    //get ports
    public int getPredPort() {
        return pred.port;
    }

    public int getSuccPort() {
        return succ.port;
    }

    //get id's
    public int getPredID() {
        return pred.id;
    }

    public int getSuccID() {
        return succ.id;
    }

    public void setID(int id) {this.id = id;}

    //reset the succ and pred
    public void setSucc(int id, String IP, int port) {
        if(succ == null) {
            succ = new NameServer(id, IP, port);
            return;
        }

        succ.id = id;
        succ.IP = IP;
        succ.port = port;
    }

    public void setPred(int id, String IP, int port) {
        if(succ == null) {
            succ = new NameServer(id, IP, port);
            return;
        }
        
        pred.id = id;
        pred.IP = IP;
        pred.port = port;
    }
}


