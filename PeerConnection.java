import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class PeerConnection extends Connection{
    public PeerConnection(Socket socket, ArrayList<Connection> connectionList) throws IOException
    {
        super(socket, connectionList);
        connectionList.add(this);
    }

    public void eventHandler(Packet p)
    {
        int event_type = p.event_type;
        switch (event_type)
        {
            case 1: // client is requesting a file
                clientReqFile(p);break;

            case 5:
                clientWantsToQuit(p);break;
        };
    }

    public void clientReqFile(Packet p)
    {
        boolean sendingFile = true;
        int findex = p.req_file_index;
        byte[] file = generate_file(findex, 20000);
        for (int blockNum = 0; blockNum < 20&&sendingFile; blockNum++) {
            byte[] block = new byte[1000];
            for(int i = 0; i<1000; i++){
                block[i] = file[blockNum*1000+i];
            }
            Packet packet = new Packet();
            packet.event_type = 2;
            packet.DATA_BLOCK = block;
            try {
                send_packet_to_client(packet);

                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                sendingFile = false;
                super.connectionList.remove(this);
            }
        }
    }
}
