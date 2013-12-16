package filecopy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import util.huebner.BoundedBuffer;

public class FCReciever extends Thread {

	private DatagramPacket udpReceivePacket;

	private byte[] receiveData;

	private DatagramSocket clientSocket;

	private InetAddress receivedIPAddress;
	private InetAddress serverAdress;

	private BoundedBuffer<FCpacket> sendBuffer;
	
	private FileCopyClient myFCC;

	public FCReciever(BoundedBuffer<FCpacket> buffer, InetAddress serverAdress, FileCopyClient myFCC) {
		receiveData = new byte[8];
		sendBuffer = buffer;
		this.serverAdress = serverAdress;
		this.myFCC = myFCC;
	}

	public void run() {
		// 3.Warte auf Antwort
		try {
			clientSocket = new DatagramSocket();
			udpReceivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(udpReceivePacket);
			receivedIPAddress = udpReceivePacket.getAddress();
			if (receivedIPAddress.equals(serverAdress)) {
				// 4.Antwort gekommen -> Markiere das Packet des ack als
				// empfangen
				FCpacket ackPacket = new FCpacket(udpReceivePacket.getData(),
						udpReceivePacket.getLength());

				long SeqNum = ackPacket.getSeqNum();
				int sendBufferIndex = new Long(SeqNum
						- sendBuffer.peek().getSeqNum()).intValue();
				if (sendBufferIndex >= 0) {
					FCpacket ackedPacket = sendBuffer.get(sendBufferIndex);
					ackedPacket.setValidACK(true);
					myFCC.cancelTimer(ackedPacket);
				}
				// 5.Betrachte 1. element der Liste. Entferne es, wenn es als
				// empfangen markiert ist.
				// 6.wiederhole 6. bis das 1. element der liste nicht markiert
				// ist.
				while (sendBuffer.peek().isValidACK()) {
					sendBuffer.remove();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myFCC.notify();
		
	}
}
