package examples;

import java.io.*;
import java.net.*;
import java.math.*;

public class Client extends PacketExchanger
{
	
	private static int payload_length = 0;  // the max payload of the file
	private static int numb_of_packets = 0; // a counter for the packets sent by the client
	long startTime;                         // the transfer starting time
	long endTime;                           // the transfer finishing time
	long totalFileLength = 0;               // the size of the file

    // Constructor of a client, using
    public Client(InetAddress sourceAddress, int sourcePort) throws SocketException
    {
        super(sourceAddress, sourcePort);
    }

    public Client() throws SocketException
    {
        super();
    }

    public boolean connect(InetAddress destAddress, int destPort) throws IOException
    {
        setDestinationAddress(destAddress);
        setDestinationPort(destPort);
        boolean connectionEstablished = true;
        DatagramPacket packet = null;

        try
        {
            // Send Syn
            byte[] header = new byte[1];
            header[0] = 7;
            packet = sendAck(header);
            GUI.jTextArea2.append("\nSending SYN packet to the server.");

            // Receive SynAck
            header = new byte[1];
            packet = recvACK(2, header);
            header = takeHeader(packet, header.length);
            if (header[0] != 8)
            {
                GUI.jTextArea2.append("\nSynAck packet failed");
                connectionEstablished = false;
                return connectionEstablished;
            }
           GUI.jTextArea2.append("\nReceived SynAck packet from the server.");


            // Send Ack
            header = new byte[1];
            header[0] = 9;
            packet = sendAck(header);
            GUI.jTextArea2.append("\nSending ACK packet to the server. \n");
            //System.out.println();

        } catch (SocketTimeoutException ste)
        {
            ste.printStackTrace();
            connectionEstablished = false;
        }

        return connectionEstablished;
    }


    public boolean disconnect() throws IOException
    {

        if (getDestinationAddress() == null || getDestinationPort() == -1)
            return true;

        DatagramPacket packet = null;
        try
        {
            // Send Disc
            byte[] header = new byte[1], payload = new byte[1];
            header[0] = 12;
            packet = sendPacket(header, payload);

            // Receive Disc
            header = new byte[1];
            packet = recvACK(2, header);
            header = takeHeader(packet, header.length);
            if (header[0] != 12)
            {
                GUI.jTextArea2.append("\nDisconnect failed!");
                return false;
            }

            // Send Ack
            header = new byte[1];
            header[0] = 13;
            packet = sendAck(header);

        } catch (SocketTimeoutException ste)
        {
            ste.printStackTrace();
        }

        return true;
    }

    public void sendFile(String filePath, int maxPacketLength)
    {
        String[] tokens = filePath.split("\\.");
        if (!sendInfo(tokens[tokens.length-1], maxPacketLength))
        {
            GUI.jTextArea2.append("\nThe Client will now exit.");
            return;
        }
        File file = null;
        FileInputStream fileIn = null;
        boolean end = false, flag = false, send = true;
        byte[] header = new byte[1];
        byte[] payload = new byte[maxPacketLength];
        int packetId = -1;
        DatagramPacket packet = null;
		payload_length = maxPacketLength;
        try
        {
            GUI.jTextArea2.append("\nFile transfer started!");
            file = new File(filePath);
            fileIn = new FileInputStream(file);
            totalFileLength = file.length();
			long progress = 0;
            GUI.jTextArea2.append("\nTotal file size " + totalFileLength + " bytes.");


            while (!end)    // While the end of the file has not been reached
            {
                try
                {
                    if (!flag)  // If we do not need to retransmit the packet
                    {
                        payload = new byte[maxPacketLength];
                        int len = fileIn.read(payload);
                        if (len == -1)  // If there is no data because the end of the file has been reached
                            end = true; // Tell it to the client
                        else
                        {
                            /*Adjust the payload length to the exact number of bytes
                            that were read from the file(always <= payload.length)*/
                            byte[] temp = new byte[len];
                            for (int i = 0; i < len; i++)
                                temp[i] = payload[i];
                            payload = temp;
                        }
                        // Increment the packet id(slide the window)
                        packetId = (packetId + 1) % 2;
                        if (end)
                            progress = totalFileLength;
                        else
                            progress += len;
                        // Test disconnect
                        /*if ((float) progress / totalFileLength > 0.5)
                            if (disconnect())
                            {
                                System.out.println("Disconnected!");
                                break;
                            }*/
                        //System.out.printf("\rProgress: %f%%", (float) progress / totalFileLength);
                    }
                    header = new byte[1];
                    // We have a special header for the end of the file.
                    if (end)
                        header[0] = 2;  // The client will not recognise it as a packet
                    else
                        header[0] = (byte) packetId;

					if(send){
						packet = sendPacket(header, payload);

						numb_of_packets++;
						GUI.jTextArea2.append("\nSending packet number: " + numb_of_packets);
					}
                    
                    // Receive the acknowledgement(A header that contains the id of the packet we sent).
                    packet = recvACK(2, header);
                    GUI.jTextArea2.append("\nReceived ACK.");
                    header = takeHeader(packet, header.length);
                    // If no exception occured.
                    flag = false;
                    // Check the header.
                    if (header[0] != packetId)
                    {
                        flag = true;
			send = false;
                    }
		    else
		    	send = true;
		
                    // Simulate packet loss.
                    /*if (Math.random() < 0.5)
                        flag = true;*/

                } catch (SocketTimeoutException ste)
                {
                    // The timeout expired so we send the same packet.
                    flag = true;
		    send = true;
                    GUI.jTextArea2.append("\nTimeout expired.");
                } catch (IOException ioe)
                {
                    GUI.jTextArea2.append("\nIOE");
                    ioe.printStackTrace();

                }
            }
            // If the socket cannot be opened or cannot be bound to the specific port
        } catch (FileNotFoundException fnfe)
        {
            GUI.jTextArea2.append("\nFNFE");

        } finally
        {
            try
            {
                fileIn.close();
            } catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }

        GUI.jTextArea2.append("\n");
    }

    private boolean sendInfo(String fileExtention, int maxPacketSize)
    {
        DatagramPacket packet = null;
        try
        {
            String temp = maxPacketSize + "";
            sendString(temp);
            byte[] header = new byte[1];
            packet = recvACK(2,header);
            header = takeHeader(packet, header.length);
            if (header[0] != 10)
            {
                GUI.jTextArea2.append("\nFailed to send the information(1)");
                return false;
            }
            sendString(fileExtention);
            header = new byte[1];
            header[0] = 10;
            packet = recvACK(2,header);
            header = takeHeader(packet, header.length);
            if (header[0] != 10)
            {
                 GUI.jTextArea2.append("\nFailed to send the information(2)");
                return false;
            }
        }catch (SocketTimeoutException ste)
        {
             GUI.jTextArea2.append("\nFailed to send the file information due to timeout!");
            ste.printStackTrace();
            return false;
        }catch (IOException ioe)
        {
             GUI.jTextArea2.append("\nFailed to send the file information!");
            ioe.printStackTrace();
            return false;
        }

        return true;
    }
	
    public void statistics()
    {
	float totalTime = (float)((endTime - startTime)*Math.pow(10,-9)); // calculate the duration and convert to sec
        GUI.jTextArea2.append("\nStatistics!!!\n");
        GUI.jTextArea2.append("\nThe total time of the transfer was " + totalTime + " sec\n");
        GUI.jTextArea2.append("\nThe speed of the transfer was " + (totalFileLength/1000)/totalTime + " Kbyte/sec\n");
        GUI.jTextArea2.append("\nThe total number of UDP/IP packets of the transfer was " + numb_of_packets + "\n");
        GUI.jTextArea2.append("\nThe payload length of the packet was " + payload_length + "\n");
    }
	

    /*public static void main(String[] args) throws SocketException
    {
        try
        {
            Client c = new Client(InetAddress.getByName("localhost"), 6666);
            boolean test = c.connect(InetAddress.getByName("localhost"), 7777);
            if (test)
            {
                System.out.println("File transfer begins.");
                //c.sendFile("/home/marios/Downloads/Blade.Runner.2049/Blade.Runner.2049.mkv", 60000);
                //c.sendFile("/home/marios/Downloads/Client.java", 5);
                c.sendFile("F:/Users/Kostas/Videos/Clips/Source.mp4", 60000);
                System.out.println("File transfer completed.");
            } else
                System.out.println("Server Connection failed!");
        } catch (UnknownHostException uhe)
        {
            uhe.printStackTrace();
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }*/
}
