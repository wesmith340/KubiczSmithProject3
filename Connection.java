import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

abstract class Connection extends Thread
{
    Socket socket;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    int peerPort;
    int peer_listen_port;
    int peerID;
    InetAddress peerIP;
    char FILE_VECTOR[];
    ArrayList<Connection> connectionList;
    boolean runFlag=true;

    public Connection () {
        runFlag=false;
    }

    public Connection(Socket socket, ArrayList<Connection> connectionList) throws IOException
    {
        this.connectionList=connectionList;
        this.socket=socket;
        this.outputStream=new ObjectOutputStream(socket.getOutputStream());
        this.inputStream=new ObjectInputStream(socket.getInputStream());
        this.peerIP=socket.getInetAddress();
        this.peerPort=socket.getPort();
    }

    @Override
    public void run() {
        //wait for register packet.
        Packet p= new Packet();

        try {p = (Packet) inputStream.readObject();}
        catch (Exception e) {System.out.println("Could not register client");return;}
        eventHandler(p);

        while (runFlag){
            try {
                //printConnections();
                p = (Packet) inputStream.readObject();
                eventHandler(p);
                // p.printPacket();

            }
            catch (Exception e) {break;}
        }
    }

    public void printConnections()
    {
        System.out.println("---------------");
        for(int i = 0; i < connectionList.size(); i++) {

            System.out.println("Peer ID :"+connectionList.get(i).peerID);
            System.out.println("FILE_VECTOR :"+String.valueOf(connectionList.get(i).FILE_VECTOR));
            System.out.println("---------------");
        }
    }

    public void send_packet_to_client(Packet p) throws IOException {
        outputStream.writeObject(p);
        outputStream.flush();
        // System.out.println("Packet Sent ");
        //p.printPacket();
    }
    public void closeConnection()
    {
        try {
            outputStream.close();
            inputStream.close();
            socket.close();
            System.out.println("Closed clientSocket");
        }
        catch (Exception e) { System.out.println("Couldn't close socket!");
            //e.printStackTrace();

        }
    }

    void send_quit_message()
    {
        Packet p = new Packet();
        p.event_type=6;
        try {
            send_packet_to_client(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract public void eventHandler(Packet p);

    abstract public void clientReqFile(Packet p);

    public void clientRegister(Packet p)
    {
        FILE_VECTOR=p.FILE_VECTOR;
        peer_listen_port=p.peer_listen_port;
        peerID=p.sender;
        connectionList.add(this);
        System.out.println("Client connected. Total Registered Clients : "+connectionList.size() );
        printConnections();
    }

    public void clientWantsToQuit(Packet p)
    {
        //remove client from list. close thread.
        int clientPos=searchForClient(p.sender);
        System.out.println("Removing client "+p.sender);
        connectionList.remove(clientPos);
        System.out.println("Total Registered Clients : "+connectionList.size() );
        closeConnection();

    }
    public int searchForClient(int ID)
    {
        for (int i=0;i<connectionList.size();i++)
        {
            if (connectionList.get(i).peerID==ID)
                return i;
        }
        return -1;
    }
    //----------------------------------------------------------------------------------------------------------------------
    // Hashing methods
    public byte[] generate_file(int findex, int length)
    {
        byte[] buf= new byte[length];
        Random r = new Random();
        r.setSeed(findex);
        r.nextBytes(buf);
        try{
            System.out.println(SHAsum(buf));
        }
        catch (Exception e){System.out.println("SHA1 error!");}
        return buf;
    }
    public String find_file_hash(byte [] buf)
    {
        String h = "";
        try {
            h = SHAsum(buf);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return h;
    }

    public String SHAsum(byte[] convertme) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return byteArray2Hex(md.digest(convertme));
    }

    private static String byteArray2Hex(byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}