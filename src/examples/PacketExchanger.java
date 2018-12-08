package examples;

import java.io.*;
import java.net.*;

public abstract class PacketExchanger
{
    private DatagramSocket udpSocket; // The socket through which the server communicates with all of its clients
    private InetAddress sourceAddress, destinationAddress; // The IP address of the socket and the server, respectively
    private int sourcePort, destinationPort;    // The port of the socket and the server, respectively

    //******************** Getters and Setters ********************//

    public DatagramSocket getUdpSocket()
    {
        return udpSocket;
    }

    public void setUdpSocket(DatagramSocket udpSocket)
    {
        this.udpSocket = udpSocket;
    }

    public InetAddress getSourceAddress()
    {
        return sourceAddress;
    }

    public void setSourceAddress(InetAddress sourceAddress)
    {
        this.sourceAddress = sourceAddress;
    }

    public InetAddress getDestinationAddress()
    {
        return destinationAddress;
    }

    public void setDestinationAddress(InetAddress destinationAddress)
    {
        this.destinationAddress = destinationAddress;
    }

    public int getSourcePort()
    {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort)
    {
        this.sourcePort = sourcePort;
    }

    public int getDestinationPort()
    {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort)
    {
        this.destinationPort = destinationPort;
    }


    //******************** Constructors ********************//

    // Constructor using an IP address and a port to initialize the UDP Socket
    public PacketExchanger(InetAddress sourceAddress, int sourcePort) throws SocketException
    {
        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.destinationAddress = null;
        this.destinationPort = -1;

        udpSocket = new DatagramSocket(this.sourcePort);

    }

    // Default constructor that initializes the UDP socket using any available port on the local host machine
    public PacketExchanger() throws SocketException
    {
        this.destinationAddress = null;
        this.destinationPort = -1;
        udpSocket = new DatagramSocket();
        this.sourceAddress = udpSocket.getInetAddress();
        this.sourcePort = udpSocket.getPort();

    }

    //******************** Methods for creating and exchanging packets ********************//

    // Create a buffer consisting of the header and the payload
    private byte[] createPacketBuffer(byte[] header, byte[] payload)
    {
        // Create the new packet with size the total of the payload and its header
        byte[] out = new byte[header.length + payload.length];
        // Insert the header at the beginning of the new packet
        for (int i = 0; i < header.length; i++)
            out[i] = header[i];
        // Insert the payload after the header
        for (int i = 0; i < payload.length; i++)
            out[i + header.length] = payload[i];
        return out;
    }

    // Obtain the header of a packet
    public byte[] takeHeader(DatagramPacket packet, int headerLength)
    {
        byte[] packetBuf = packet.getData(), header = new byte[headerLength];
        for (int i = 0; i < header.length; i++)
            header[i] = packetBuf[i];

        return header;
    }

    // Obtain the payload of a packet
    public byte[] takePayload(DatagramPacket packet, int headerLength)
    {
        /*The payload length is the number of bytes read minus
        the number of the header length in bytes*/
        byte[] packetBuf = packet.getData();
        byte[] payload = new byte[packet.getLength() - headerLength];
        for (int i = 0; i < payload.length; i++)
            payload[i] = packetBuf[i + headerLength];


        return payload;
    }

    // Send a packet consisting of a header and a payload through the UDP socket
    public DatagramPacket sendPacket(byte[] header, byte[] payload) throws IOException
    {
        byte[] packetBuf = createPacketBuffer(header, payload);
        DatagramPacket packet = new DatagramPacket(packetBuf, packetBuf.length, destinationAddress, destinationPort);
        udpSocket.send(packet);

        return  packet;
    }

    // Receive a packet through the UDP socket using timeout
    public DatagramPacket recvPacket(float sec, byte[] header, byte[] payload) throws  SocketTimeoutException
    {
        DatagramPacket packet = null;

        try
        {
            udpSocket.setSoTimeout((int)(sec * 1000));
            byte[] packetBuf = createPacketBuffer(header, payload);
            packet = new DatagramPacket(packetBuf, packetBuf.length);
            udpSocket.receive(packet);
        }catch (SocketTimeoutException ste)
        {
            // The timeout expired so we send the same packet.
            System.out.println("Timeout");
            throw new SocketTimeoutException();
        }catch (IOException ioe)
        {
            System.out.println("IOE");
            ioe.printStackTrace();
        }

        return packet;
    }

    // Receive a packet through the UDP socket
    public DatagramPacket recvPacket(byte[] header, byte[] payload)
    {
        DatagramPacket packet = null;

        try
        {
            udpSocket.setSoTimeout(0);
            byte[] packetBuf = createPacketBuffer(header, payload);
            packet = new DatagramPacket(packetBuf, packetBuf.length);
            udpSocket.receive(packet);
        }catch (IOException ioe)
        {
            System.out.println("IOE");
            ioe.printStackTrace();
        }

        return packet;
    }

    // Receive an acknowledgement using timeout
    public DatagramPacket recvACK(float sec, byte[] header) throws  SocketTimeoutException
    {
        DatagramPacket packet = null;

        try
        {
            udpSocket.setSoTimeout((int)(sec * 1000));
            packet = new DatagramPacket(header, header.length);
            udpSocket.receive(packet);
        }catch (SocketTimeoutException ste)
        {
            // The timeout expired so we send the same packet.
            System.out.println("Timeout");
            throw new SocketTimeoutException();
        }catch (IOException ioe)
        {
            System.out.println("IOE");
            ioe.printStackTrace();
        }

        return packet;
    }

    // Receive an acknowledgement
    public DatagramPacket recvACK(byte[] header) throws  SocketTimeoutException
    {
        DatagramPacket packet = null;

        try
        {
            udpSocket.setSoTimeout(0);
            packet = new DatagramPacket(header, header.length);
            udpSocket.receive(packet);
        }catch (IOException ioe)
        {
            System.out.println("IOE");
            ioe.printStackTrace();
        }

        return packet;
    }

    // Send an acknowledgement
    public DatagramPacket sendAck(byte[] header) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(header, header.length, destinationAddress, destinationPort);
        udpSocket.send(packet);

        return packet;
    }

    // Send the header and payload data
    public DatagramPacket sendString(String string) throws IOException
    {
        byte[] header = new byte[1], payload;
        header[0] = 10;

        header[0] = 11;
        payload = string.getBytes();

        return sendPacket(header, payload);
    }

    // Receive the header and payload data
    public String recvString()
    {
        DatagramPacket packet = null;
        String out = null;
        byte[] header = new byte[1], payload = null;
        try
        {
            byte[] packetBuf = createPacketBuffer(header, payload);
            packet = new DatagramPacket(packetBuf, packetBuf.length);
            udpSocket.setSoTimeout(0);
            udpSocket.receive(packet);
            header = takeHeader(packet, header.length);
            payload = takePayload(packet, header.length);
            out = new String(payload, 0, payload.length);
        }catch (IOException ioe)
        {
            System.out.println("IOE");
            ioe.printStackTrace();
        }

        return out;
    }

    // Receive the header and payload data using timeout
    public String recvString(float sec)
    {
        DatagramPacket packet = null;
        String out = null;
        byte[] header = new byte[1], payload = new byte[60000]; // Fix it someday lol
        try
        {
            udpSocket.setSoTimeout((int)(sec * 1000));
            byte[] packetBuf = createPacketBuffer(header, payload);
            packet = new DatagramPacket(packetBuf, packetBuf.length);
            udpSocket.receive(packet);
            header = takeHeader(packet, header.length);
            payload = takePayload(packet, header.length);
            out = new String(payload, 0, payload.length);
        }catch (SocketTimeoutException ste)
        {
            // The timeout expired so we send the same packet.
            out = null;
            System.out.println("Timeout while receiving a string.");
        }catch (IOException ioe)
        {
            out = null;
            System.out.println("IOE");
            ioe.printStackTrace();
        }

        return out;
    }

}
