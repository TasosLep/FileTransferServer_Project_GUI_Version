package examples;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;


public class Server extends PacketExchanger
{
    private int maxPacketSize;      // The maximum payload
    private String fileExtention;   // The extension of the file to transfer, declaring its type

    // Constructor of a server, using its IP and port as parameters
    public Server(InetAddress sourceAddress, int sourcePort) throws SocketException
    {
        super(sourceAddress, sourcePort);
    }

    // Wait for connections from clients and execute a 3-way handshake with them
    public boolean listen() throws IOException
    {
        boolean connectionEstablished = true;
        DatagramPacket packet = null;
        GUI.jTextArea1.append("\nWaiting for a client!");
        try
        {
            // Receive Syn
            byte[] header = new byte[1];
            packet = recvACK(header);
            setDestinationAddress(packet.getAddress());
            setDestinationPort(packet.getPort());
            header = takeHeader(packet, header.length);

            if (header[0] != 7) // if the packet received is not the SYN sent by the client
            {
                GUI.jTextArea1.append("\nSYN packet failed");
                connectionEstablished = false;
                return connectionEstablished;
            }
            GUI.jTextArea1.append("\nReceived SYN packet from the client.");


            // Send SynAck
            header = new byte[1];
            header[0] = 8;
            packet = sendAck(header);
            GUI.jTextArea1.append("\nSend SynAck packet to the client.");


            // Receive Ack
            header = new byte[1];
            packet = recvACK(2, header);
            header = takeHeader(packet, header.length);

            if (header[0] != 9) // if the packet received is not the ACK sent by the client
            {
                GUI.jTextArea1.append("\nACK packet failed");
                connectionEstablished = false;
                return connectionEstablished;
            }
            GUI.jTextArea1.append("\nReceived ACK packet from the client.");


        } catch (SocketTimeoutException ste)
        {
            ste.printStackTrace();
            connectionEstablished = false;
        }
        return connectionEstablished;
    }

    // Get the file sent by the client and save it to a specified directory path
    public void receiveFile()
    {
        if (!receiveInfo()) // if the max payload or the file extension are not received by the server
        {
            GUI.jTextArea1.append("\nThe server will now exit.");
            return;
        }

        String outputPath = System.getProperty("user.dir") + "/out." + fileExtention;
        byte[] header = new byte[1];
        byte[] payload = new byte[maxPacketSize];
        //packetBuf = new byte[header.length + payload.length];
        boolean flag = true, end = false; // flag is true when we receive a packet out of order
        FileOutputStream fileOut = null;
        DatagramPacket packet = null;
        try
        {
            // Initialize the output stream to write the data to the new file
            fileOut = new FileOutputStream(outputPath);
            int packetId = 0;

            while (!end)
            {
                try
                {
                    if (!flag)  // if all packets are received in order
                    {
                        fileOut.write(payload); // write payload.length bytes from the payload array to fileOut
                        packetId = (packetId + 1) % 2;
                    }

                    // Reveive the data packet from the server.
                    packet = recvPacket(header,payload);
                    GUI.jTextArea1.append("\nReceived packet.");
                    header = takeHeader(packet, header.length);
                    payload = takePayload(packet, header.length);

                    byte receivedID = header[0];

                    // If it is the end of the file we are done
                    if (receivedID == 2)
                        end = true;

                    if (receivedID == 12)
                    {
                        header = new byte[1];
                        header[0] = 12;
                        packet = sendAck(header);

                        // Receive Disc
                        header = new byte[1];
                        packet = recvACK(2, header);
                        header = takeHeader(packet, header.length);
                        if (header[0] == 13)
                        {
                            GUI.jTextArea1.append("\nClient disconnected!");
                            break;
                        }
                    }
                    flag = false;

                    if (!end && packetId != receivedID)
                    {
                        GUI.jTextArea1.append("\nout of order");
                        flag = true;
                        header[0] = receivedID;
                        // We send an ack with the received packet header as a header.
                    }else
                        header[0] = (byte) packetId;
                    GUI.jTextArea1.append("\nSending ACK to the client.");
                    packet = sendAck(header);
                } catch (IOException ioe)
                {
                    GUI.jTextArea1.append("\nIOE");
                }

            }

        }catch (IOException ioe)
        {
            GUI.jTextArea1.append("\nIOE");
        } finally
        {
            try
            {
                fileOut.close();
                GUI.jTextArea1.append("\nThe file is READY!!!" + "\nYou are exiting the program...");
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private boolean receiveInfo()
    {
        try
        {
            String temp = recvString(2);

            if (temp == null)
            {
                GUI.jTextArea1.append("\nFailed to receive information!(1)");
                return false;
            }
            byte[] header = new byte[1];
            header[0] = 10;
            sendAck(header);
            maxPacketSize = Integer.parseInt(temp);
            temp = recvString(2);

            if (temp == null)
            {
                GUI.jTextArea1.append("\nFailed to receive information!(2)");
                return false;
            }

            header = new byte[1];
            header[0] = 10;
            sendAck(header);
            fileExtention = temp;
        }catch (IOException ioe)
        {
            GUI.jTextArea1.append("\nFailed to receive information!(3)");
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

    /*public static void main(String[] args) throws SocketException
    {
        try
        {
            Server s = new Server(InetAddress.getByName("localhost"), 7777);
            boolean test = s.listen();
            if (test)
            {
                System.out.println("Client connected\nFile transfer begins.");
                s.receiveFile();
                System.out.println("Filed transfer completed.");
            }else
                System.out.println("Client connection failed.");
        } catch (UnknownHostException uhe)
        {
            uhe.printStackTrace();
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }*/

}
