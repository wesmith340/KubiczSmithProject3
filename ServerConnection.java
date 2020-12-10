import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerConnection extends Connection{

    public ServerConnection(Socket socket, ArrayList<Connection> connectionList) throws IOException
    {
        super(socket, connectionList);
    }

    @Override
    public void eventHandler(Packet p) {
        int event_type = p.event_type;
        switch (event_type)
        {
            case 0: //client register
                clientRegister(p);break;

            case 1: // client is requesting a file
                clientReqFile(p);break;

            case 5: // client wants to close the connection
                clientWantsToQuit(p);break;

            case 3: // client has the file
                clientGotFile(p);break;

            case 4: // client is requesting a file hash
                hashRequest(p);break;
        }
    }

    @Override
    public void clientReqFile(Packet p) {
        System.out.println("Client "+p.sender+" is requesting file "+p.req_file_index);
        int findex = p.req_file_index;
        Packet packet = new Packet();
        packet.event_type=2;
        packet.req_file_index=findex;

        for (int i=0;i<connectionList.size();i++)
        {
            if (connectionList.get(i).FILE_VECTOR[findex]=='1')
            {
                packet.peerID=connectionList.get(i).peerID;
                packet.peer_listen_port=connectionList.get(i).peer_listen_port;
                break;

            }
        }
        send_packet_to_client(packet);
    }

    /**
     * @author Weston Smith
     * @param p
     */
    public void hashRequest(Packet p) {
        byte[] file = generate_file(p.req_file_index, 20000);
        String hash = find_file_hash(file);

        Packet packet = new Packet();
        packet.event_type = 3;
        packet.fileHash = hash;
        System.out.println("User "+peerID+" is requesting a hash");

        send_packet_to_client(packet);
    }

    public void clientGotFile(Packet p)
    {
        System.out.println("Update file vector");
        FILE_VECTOR[p.req_file_index] = '1';
    }
}
