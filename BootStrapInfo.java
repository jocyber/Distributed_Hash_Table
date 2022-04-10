//information 'struct'
public class BootStrapInfo {
    final private int id = 0;

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
    private NameServer pred = new NameServer(0, "127.0.0.1", 2000);
    private NameServer succ = new NameServer(0, "127.0.0.1", 2000);

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

    //reset the succ and pred
    public void setSucc(int id, String IP, int port) {
        succ.id = id;
        succ.IP = IP;
        succ.port = port;
    }

    public void setPred(int id, String IP, int port) {
        pred.id = id;
        pred.IP = IP;
        pred.port = port;
    }
}