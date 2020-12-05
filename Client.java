
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.lang.*; 

public class Client {

     int serverPort;
     InetAddress ip=null; 
     Socket s; 
     ObjectOutputStream outputStream ;
     ObjectInputStream inputStream ;
     int peerID;
     int peer_listen_port;
     char FILE_VECTOR[];

     boolean gettingFile = false;
    // To do , create each peers own ServerSocket listener to monitor for incoming peer requests. start a listener thread in main();
    // I used the ServerSocketHandler to handle both client-server and peer-to-peer listeners. You can use a separate class. 90% of the code is repeated.
    // For the individual connections, again you can re-use the Connection class, and add some event handlers to process event codes that will be used to distibguis betwwen peer-to-peer or cleint-server communications, or create a separate class called peerConnection. It is completely your choice.
    public static void main(String args[])
    {
        Client client = new Client();
        boolean runClient=true;
        Scanner input = new Scanner(System.in);
        if (args.length==0 || args.length % 2 == 1){
            System.out.println("Parameters Required/Incorrect Format. See usage list");
            System.exit(0);
        }

        client.cmdLineParser(args);

        try
        { 
            if (client.ip==null)
                client.ip = InetAddress.getByName("localhost"); 

            client.s = new Socket(client.ip, client.serverPort); 
            client.outputStream = new ObjectOutputStream(client.s.getOutputStream());
            client.inputStream = new ObjectInputStream(client.s.getInputStream());
            System.out.println("Connected to Server ..." +client.s); 

            Packet p = new Packet();
            p.event_type=0;
            p.sender=client.peerID;
            p.peer_listen_port=client.peer_listen_port;
            p.FILE_VECTOR = client.FILE_VECTOR;

            client.send_packet_to_server(p);

            System.out.println("Packet Sent");
            
            Thread r = new PacketHandler(client);
            r.start();
            
            
            while (runClient){
                
                System.out.println ("Enter query");
                char cmd=input.next().charAt(0);
                switch(cmd)
                {
                    case 'q':
                    System.out.println("Getting ready to quit ..." +client.s); 
                    client.send_quit_to_server();
                    runClient=false;
                    break;

                    case 'f':
                        if (!client.gettingFile) {
                            System.out.println("Enter the file index you want ");
                            int findex = input.nextInt();
                            client.send_req_for_file(findex);
                            client.gettingFile = true;
                        }
                        break;

                    default:
                    System.out.println("Command not recognized. Try again ");

                }
                //Packet p = (Packet) inputStream.readObject();
                //p.printPacket();
            }
        }
        catch(Exception e){ 
            e.printStackTrace(); 
        }

    }

    void send_packet_to_server(Packet p)
    {
        try
        { 
            outputStream.writeObject(p);
        }
        catch(Exception e){
            System.out.println ("Could not send packet! ");
        }
    }

    void send_quit_to_server()
    {
        Packet p = new Packet();
        p.sender=peerID;
        p.event_type=5;
        p.port_number=peer_listen_port;
        send_packet_to_server(p);
        
    }

    public void cmdLineParser(String args[])
    {
        int i;
        for (i=0;i<args.length;i+=2)
        {
            String option=args[i];
            switch (option)
            {
                case "-c": //config_file
                File file = new File(args[i+1]);
                read_config_from_file(file);
                break;

                case "-i": //my ID
                peerID=Integer.parseInt(args[i+1]);
                break;

                case "-p": // my listen port
                peer_listen_port=Integer.parseInt(args[i+1]);
                break;

                case "-s": //server port
                serverPort=Integer.parseInt(args[i+1]);
                break;

                case "-n":
                try{ip = InetAddress.getByName(args[i+1]);} catch(Exception e){
                System.out.println ("Could not resolve hostname! " +args[i+1]);}
                break;

                default: System.out.println("Unknown flag "+args[i]);

            }

        }
    }

    public void read_config_from_file(File file )
    {
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String opt[]= line.split(" ",2);
                switch(opt[0])
                {
                    case "SERVERPORT": serverPort=Integer.parseInt(opt[1]);break;
                    case "CLIENTID": peerID=Integer.parseInt(opt[1]);break;
                    case "MYPORT": peer_listen_port=Integer.parseInt(opt[1]);break;
                    case "FILE_VECTOR": FILE_VECTOR = opt[1].toCharArray();break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void send_req_for_file(int findex)
    {
        if (FILE_VECTOR[findex] =='1'){
            System.out.println("I already have this file block!");
            return;
        }
        System.out.println(" I don't have this file. Let me contact server...");
        //request file from server
        Packet p = new Packet();
        p.sender=peerID;
        p.event_type=1;
        p.peer_listen_port=peer_listen_port;
        p.req_file_index=findex;
        send_packet_to_server(p);
        //disconnect();
    }

    void disconnect()
    {
        try {
                outputStream.close();
                inputStream.close();
                s.close();
                System.out.println("Closed Socket");
            }
        catch (Exception e) { System.out.println("Couldn't close socket!");}
    }

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

class PacketHandler extends Thread
{
    Client client;
    FileGetter fileGetter;

    public PacketHandler(Client client)
    {
        this.client=client;
    }

    public void run()
    {
        Packet p;

        while(true){
        try { 
            p = (Packet) client.inputStream.readObject();
            process_packet_from_server(p);
        }
        catch (Exception e) { 
             //e.printStackTrace();
             break;
        }
    }

    }

    void process_packet_from_server(Packet p)
    {
     int e = p.event_type;

     switch (e)
     {
        case 2: //server reply for req. file
            if (p.peerID==-1)
                System.out.println("Server says that no client has file "+p.req_file_index);
            else{
                System.out.println("Server says that peer "+p.peerID+" on listening port "+p.peer_listen_port+" has file "+p.req_file_index);
                //PeerToPeerHandler(p.peerIP,p.peer_listen_port,p.req_file_index,p.req_file_index); // TO DO
                fileGetter = new FileGetter(client, p.peerIP,p.peer_listen_port,p.req_file_index,p.req_file_index);
            }
            break;
        case 3:
             fileGetter.hashCheck = p.fileHash;
            break;

        case 6: //server wants to quit. I should too.
                System.out.println("Server wants to quit. I should too! ");
                client.disconnect();
                System.exit(0);

     }

    }

    void PeerToPeerHandler(InetAddress remotePeerIP, int remotePortNum, int remotePeerID, int findex)
    {
        // To implement.
        
        // connect to peer
        // while file not received correctly
            // request_file_from_peer
            // receive_file_from_peer
            // verify file_hash
            // if correct, send positve ack, break
            // if incorrect, send negative ack, loop back
            
        //once, file has been received, send update file request to server.
        
    }
}

/**
 * FileGetter class
 * @author Weston Smith
 * This class is designed to retrieve a file from another client
 */
class FileGetter extends Thread{
    Socket fileSocket;
    ObjectOutputStream outputStream ;
    ObjectInputStream inputStream ;

    Client client;

    InetAddress remotePeerIP;
    int remotePortNum, remotePeerID, findex;
    int blockNum;
    boolean running = true;
    boolean correctFile = false;

    byte[] fileVector;
    int fileSize = 20000;
    int blockSize = 1000;
    String hashCheck = null;

    /**
     * Constructor
     * @param client
     * @param remotePeerIP
     * @param remotePortNum
     * @param remotePeerID
     * @param findex
     */
    public FileGetter (Client client, InetAddress remotePeerIP, int remotePortNum, int remotePeerID, int findex) {
        fileVector = new byte[fileSize];

        this.client = client;
        this.remotePeerIP = remotePeerIP;
        this.remotePortNum = remotePortNum;
        this.remotePeerID = remotePeerID;
        this.findex = findex;
    }

    /**
     * Implements runnable
     */
    public void run() {
        // Connect with peer
        try {
            fileSocket = new Socket(remotePeerIP, remotePortNum);
            outputStream = new ObjectOutputStream(fileSocket.getOutputStream());
            inputStream = new ObjectInputStream(fileSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Keep going until file is correctly received or until told to stop
        while (running&&!correctFile) {
            try {
                requestFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Receive file
            for (blockNum = 1; blockNum <= 20; blockNum++) {
                try {
                    processPacket((Packet)inputStream.readObject());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            // Ask server for hash to verify the file
            try {
                Packet p = new Packet();
                p.event_type = 3;
                p.req_file_index = findex;
                outputStream.writeObject(p);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Wait for packet handler to receive hashCheck
            while (running&&hashCheck.equals(null)){
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Check the file's hash with the server's hash
            String fileHash = client.find_file_hash(fileVector);
            Packet p = new Packet();
            p.event_type = 3;
            if (hashCheck.equals(fileHash)){
                p.gotFile = true;
            } else {
                p.gotFile = false;
            }
            try {
                outputStream.writeObject(p);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        disconnect();
        client.gettingFile = false;
    }

    /**
     * This method requests a file from another client
     * @throws IOException
     */
    private void requestFile() throws IOException {
        Packet p = new Packet();
        p.sender=client.peerID;
        p.event_type=1;
        p.peer_listen_port=client.peer_listen_port;
        p.req_file_index=findex;

        outputStream.writeObject(p);
        outputStream.flush();
    }

    /**
     * This method processes incoming packets from another client
     * @param p
     */
    private void processPacket(Packet p) {
        switch (p.event_type) {
            case 2:
                storePacket(p);
                break;
            case 6:
                disconnect();
                break;
        }
    }

    /**
     * This method store a packets contents to a byte array
     * @param p
     */
    private void storePacket(Packet p){
        for (int i = 0; i<blockSize; i++) {
            fileVector[i*blockNum] = p.DATA_BLOCK[i];
        }
    }

    /**
     * This method severs the connection
     */
    private void disconnect()
    {
        try {
            outputStream.close();
            inputStream.close();
            fileSocket.close();
            running = false;
            System.out.println("Closed connection with peer");
        }
        catch (Exception e) { System.out.println("Couldn't close connection with peer!");}
    }
}
