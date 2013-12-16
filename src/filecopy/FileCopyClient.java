package filecopy;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FileCopyClient extends Thread {

	// -------- Constants
	public final static boolean TEST_OUTPUT_MODE = false;

	public final int SERVER_PORT = 23000;

	public final int UDP_PACKET_SIZE = 1008;

	// -------- Public parms
	public String servername;

	public String sourcePath;

	public String destPath;

	public int windowSize;

	public long serverErrorRate;

	// -------- Variables
	// current default timeout in nanoseconds
	private long timeoutValue = 100000000L;

	// -------- Constants added by us

	private long DELAY = 1; // This variable is used in the sendThreads to
							// simulate a RTT increased by the value of delay in
							// miliseconds

	private static final float INFLUENCE_COEFFICIENT = 0.1f; // the "x" value of
																// the RTT
																// calculation

	// -------- Variables added by us

	private InetAddress serverAdress;

	private DatagramSocket clientSocket;

	private LinkedList<FCpacket> sendBuffer; // A list of the packets of the
												// current window (Use a
												// ArrayList?)

	private long estimatedRTT; // The RoundTripTime used to calculate the
								// timeoutValue
								// EstimatedRTT = (1-x)*EstimatedRTT +
								// x*SampleRTT

	private long deviation; // Variable to incorporate RTT variance

	private int numberOfTimeouts = 0;

	private FileInputStream inFromFile; // Stream to read the File at the
										// sourcePath

	byte[] receiveData;

	InetAddress receivedIPAddress;

	DatagramPacket udpReceivePacket;

	long currentPacketNumber = 1;
	
	long time = 0;

	// ... ToDo

	// Constructor
	public FileCopyClient(String serverArg, String sourcePathArg,
			String destPathArg, String windowSizeArg, String errorRateArg)
			throws UnknownHostException {
		servername = serverArg;
		serverAdress = InetAddress.getByName(servername);
		sourcePath = sourcePathArg;
		destPath = destPathArg;
		windowSize = Integer.parseInt(windowSizeArg);
		serverErrorRate = Long.parseLong(errorRateArg);
		sendBuffer = new LinkedList<FCpacket>();
		receiveData = new byte[8];
	}

	public void runFileCopyClient() throws IOException {
		time = System.nanoTime();
		clientSocket = new DatagramSocket();
		inFromFile = new FileInputStream(sourcePath);
		int inputStreamReturnValue = 1;
		// 1a.KontrollPaket erstellen und in den Buffer schreiben
		// sendBuffer.add(makeControlPacket()); //KontrollPaket wird direkt beim
		// senden erstellt.
		// 1b.Buffer f�llen -> Lese x Pakete � 8Byte SeqNumber und 1000Byte
		// Daten ein.
//		while (sendBuffer.size() < windowSize && inputStreamReturnValue != -1) { // From
//																					// Packet
//																					// Number
//																					// 1
//																					// to
//																					// Number
//																					// windowSize-1
//			byte[] data = new byte[UDP_PACKET_SIZE - 8];
//			inputStreamReturnValue = inFromFile.read(data);
//			FCpacket packet = new FCpacket(currentPacketNumber, data,
//					UDP_PACKET_SIZE - 8);
//			sendBuffer.add(packet);
//			currentPacketNumber++;
//		}
		// 2.Sende KontrollPaket
		FCpacket controlPacket = makeControlPacket();
		sendBuffer.add(controlPacket);
		startTimer(controlPacket);
		new sendThread(toDatagramPacket(controlPacket)).start();
		
		while (!sendBuffer.isEmpty()) { //siehe punkt 9
			// 3.Warte auf Antwort
			udpReceivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(udpReceivePacket);
			receivedIPAddress = udpReceivePacket.getAddress();
			if (receivedIPAddress.equals(serverAdress)) {
				// 4.Antwort gekommen -> Markiere das Packet des ack als
				// empfangen
				FCpacket ackPacket = new FCpacket(udpReceivePacket.getData(),
						udpReceivePacket.getLength());
				testOut("ACK Packet " + ackPacket.getSeqNum());
				
				long SeqNum = ackPacket.getSeqNum();
				int sendBufferIndex = new Long(SeqNum - sendBuffer.get(0).getSeqNum()).intValue();
				if(sendBufferIndex>=0){
					FCpacket ackedPacket = sendBuffer.get(sendBufferIndex);
					ackedPacket.setValidACK(true);
					cancelTimer(ackedPacket);
				}
				// 5.Betrachte 1. element der Liste. Entferne es, wenn es als
				// empfangen markiert ist.
				// 6.wiederhole 6. bis das 1. element der liste nicht markiert
				// ist.
				while (!sendBuffer.isEmpty()&&sendBuffer.getFirst().isValidACK()) {
					sendBuffer.removeFirst();
				}
				// 7.F�lle den Buffer, bis dieser eine gr��e von windowSize
				// erreicht hat oder alle Daten der datei eingelesen wurden und
				// sende die neuen Pakete.
				List<FCpacket> newPackets = new ArrayList<FCpacket>();
				while (sendBuffer.size() < windowSize && inputStreamReturnValue != -1) {
					byte[] data = new byte[UDP_PACKET_SIZE - 8];
					inputStreamReturnValue = inFromFile.read(data, 0, data.length);
					testOut("trying to read Packet Number " + currentPacketNumber + "." + (inputStreamReturnValue==-1?" Reading failed":" Reading succeeded"));
					if(inputStreamReturnValue != -1){
					FCpacket packet = new FCpacket(currentPacketNumber, data,
							UDP_PACKET_SIZE - 8);
					sendBuffer.add(packet);
					newPackets.add(packet);
					currentPacketNumber++;
					}
				}
				// 8.Sende die Daten-Pakete (Alle auf einmal? -> ack verpasst?(vlt
				// eigener Thread zum warten auf antwort))
				for (FCpacket packet : newPackets) {
					startTimer(packet);
					new sendThread(toDatagramPacket(packet)).start();
				}
			}
		}// 9.gehe zu 3 wenn der buffer nicht leer ist
		clientSocket.close();
		inFromFile.close();
		System.out.println("Zeit: " + (System.nanoTime()-time));
		System.out.println("Timeouts: " + numberOfTimeouts);
		System.out.println("RTT: " + estimatedRTT);
		testOut("Error check...");
		if(sendBuffer.isEmpty()){
			testOut("No errors");
		}else{
		for (int i=0; i<sendBuffer.size();i++){
			testOut("ERROR: missed packet " + sendBuffer.get(i).getSeqNum());
		}
		}
	}

	/**
	 * 
	 * Timer Operations
	 */
	public void startTimer(FCpacket packet) {
		/* Create, save and start timer for the given FCpacket */
		FC_Timer timer = new FC_Timer(timeoutValue, this, packet.getSeqNum());
		packet.setTimestamp(System.nanoTime());
		packet.setTimer(timer);
		timer.start();
	}

	public void cancelTimer(FCpacket packet) {
		
		/* Cancel timer for the given FCpacket */
		testOut("Cancel Timer for packet" + packet.getSeqNum());
		if (packet.getTimer() != null) {
			packet.getTimer().interrupt();
		}
		long rTT = System.nanoTime() - packet.getTimestamp();
		computeTimeoutValue(rTT);
	}

	/**
	 * Implementation specific task performed at timeout
	 */
	public void timeoutTask(long seqNum) {
		int currentIndexOfPacket = new Long(seqNum
				- sendBuffer.get(0).getSeqNum()).intValue();
		if(currentIndexOfPacket>=0){
		numberOfTimeouts++;
		FCpacket packet = sendBuffer.get(currentIndexOfPacket);
//		cancelTimer(packet);
		startTimer(packet);
		sendThread send = new sendThread(toDatagramPacket(packet));
		send.start();
		testOut("Timeout of packet " + seqNum + ". " + (numberOfTimeouts+1) + ". Timeout" );
		}
	}

	private DatagramPacket toDatagramPacket(FCpacket packet) {
		byte[] data = packet.getSeqNumBytesAndData();
		return new DatagramPacket(data, data.length,
				serverAdress, SERVER_PORT);
	}

	/**
	 * 
	 * Computes the current timeout value (in nanoseconds)
	 */
	public void computeTimeoutValue(long sampleRTT) {
		estimatedRTT = new Float((1 - INFLUENCE_COEFFICIENT) * estimatedRTT
				+ INFLUENCE_COEFFICIENT * sampleRTT).longValue();
		deviation = new Float((1 - INFLUENCE_COEFFICIENT) * deviation
				+ INFLUENCE_COEFFICIENT *  Math.abs(sampleRTT - estimatedRTT))
				.longValue();
		timeoutValue = estimatedRTT + 4 * deviation;
	}

	/**
	 * 
	 * Return value: FCPacket with (0 destPath;windowSize;errorRate)
	 */
	public FCpacket makeControlPacket() {
		/*
		 * Create first packet with seq num 0. Return value: FCPacket with (0
		 * destPath ; windowSize ; errorRate)
		 */
		String sendString = destPath + ";" + windowSize + ";" + serverErrorRate;
		byte[] sendData = null;
		try {
			sendData = sendString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return new FCpacket(0, sendData, sendData.length);
	}

	/**
	 * Create thread to send a packet to the Server
	 * 
	 */
	private class sendThread extends Thread {
		/* Thread for sending of one ACK-Packet with propagation delay */
		DatagramPacket packet;

		public sendThread(DatagramPacket packet) {
			this.packet = packet;
		}

		public void run() {
			try {
				FCpacket packetData = new FCpacket(packet.getData(), packet.getLength());
				testOut("sending Paket " + packetData.getSeqNum() + ": " + packet.getAddress().toString() + ":" + packet.getPort());
				Thread.sleep(DELAY);
				clientSocket.send(packet);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Unexspected Error! " + e.toString());
				System.exit(-1);
			}
		}
	}

	public void testOut(String out) {
		if (TEST_OUTPUT_MODE) {
			System.err.printf("%,d %s: %s\n", System.nanoTime(), Thread
					.currentThread().getName(), out);
		}
	}

	public static void main(String argv[]) throws Exception {
		FileCopyClient myClient = new FileCopyClient(argv[0], argv[1], argv[2],
				argv[3], argv[4]);
		myClient.runFileCopyClient();
	}

}
