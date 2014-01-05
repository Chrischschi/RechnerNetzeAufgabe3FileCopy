package christian.filecopy;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.net.*;

import util.huebner.BoundedBuffer;
import util.huebner.BoundedBufferSyncMonitor;

public class FileCopyClient extends Thread {

 

// -------- Constants
  public final static boolean TEST_OUTPUT_MODE = false;

  public final int SERVER_PORT = 23000;

  public final int UDP_PACKET_SIZE = 1008;
  
  public final int END_OF_FILE = -1;

  // -------- Public parms
  public String servername;

  public String sourcePath;

  public String destPath;

  public int windowSize;

  public long serverErrorRate;

  // -------- Variables
  // current default timeout in nanoseconds
  private long timeoutValue = 100000000L;
  
  InputStream fileToSend;
  
  private BoundedBuffer<FCpacket> sendBuffer;
  
  private DatagramSocket toServer;

  // ... ToDo


  // Constructor
  public FileCopyClient(String serverArg, String sourcePathArg,
    String destPathArg, String windowSizeArg, String errorRateArg) throws SocketException {
    servername = serverArg;
    sourcePath = sourcePathArg;
    destPath = destPathArg;
    windowSize = Integer.parseInt(windowSizeArg);
    serverErrorRate = Long.parseLong(errorRateArg);
    sendBuffer = new BoundedBufferSyncMonitor<>(windowSize);
    toServer = new DatagramSocket();
  }

  public void runFileCopyClient() {

	  
	try {
		 fileToSend = new FileInputStream(sourcePath);
	} catch (FileNotFoundException e) {
		System.err.println("Could not find File " + sourcePath + " !");
		e.printStackTrace();
	}
	  
	  //1. Init: Kontroll-Paket packen, an SenderThread übergeben und abschicken lassen
	  // ACK dafür muss kommen
	  
	  /* Kontroll-Paket Packen */
	  FCpacket controlPacket = makeControlPacket();
	  
	  Thread sendThread = new PacketSender(controlPacket); 
	 
	  sendThread.start();
	  //2. Normaler ablauf: Daten-Pakete schicken
	  //3. Ende


  }

  /**
  *
  * Timer Operations
  */
  public void startTimer(FCpacket packet) {
    /* Create, save and start timer for the given FCpacket */
    FC_Timer timer = new FC_Timer(timeoutValue, this, packet.getSeqNum());
    packet.setTimer(timer);
    timer.start();
  }

  public void cancelTimer(FCpacket packet) {
    /* Cancel timer for the given FCpacket */
    testOut("Cancel Timer for packet" + packet.getSeqNum());

    if (packet.getTimer() != null) {
      packet.getTimer().interrupt();
    }
  }

  /**
   * Implementation specific task performed at timeout
   */
  public void timeoutTask(long seqNum) {
  // ToDo
  }


  /**
   *
   * Computes the current timeout value (in nanoseconds)
   */
  public void computeTimeoutValue(long sampleRTT) {

  // ToDo
  }


  /**
   *
   * Return value: FCPacket with (0 destPath;windowSize;errorRate)
   */
  public FCpacket makeControlPacket() {
   /* Create first packet with seq num 0. Return value: FCPacket with
     (0 destPath ; windowSize ; errorRate) */
    String sendString = destPath + ";" + windowSize + ";" + serverErrorRate;
    byte[] sendData = null;
    try {
      sendData = sendString.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return new FCpacket(0, sendData, sendData.length);
  }

  public void testOut(String out) {
    if (TEST_OUTPUT_MODE) {
      System.err.printf("%,d %s: %s\n", System.nanoTime(), Thread
          .currentThread().getName(), out);
    }
  }
  
  private void sendToServer(FCpacket aPacket) throws SocketException,IOException {
			//Daten von FCpacket auf DatagramPacket umpacken 
			byte[] sendData = aPacket.getData();
			DatagramPacket sendPacket = new DatagramPacket(sendData, aPacket.getLen());
			
			InetAddress serverAddress = InetAddress.getByName(servername);
			
			sendPacket.setAddress(serverAddress);
			
			toServer.send(sendPacket);
		
	}

  public static void main(String argv[]) throws Exception {
    FileCopyClient myClient = new FileCopyClient(argv[0], argv[1], argv[2],
        argv[3], argv[4]);
    myClient.runFileCopyClient();
  }
  
  class PacketSender extends Thread {
	   
	  
	private FCpacket controlPacket;
	
	private long nextSeqNum = 0;
	
	  
	  
	public PacketSender(FCpacket initialPacket) {
		this.controlPacket = initialPacket;
 	}
	
	@Override
	public void run() {
		
		//Kontroll-Paket abschicken
		try {
			sendToServer(controlPacket);
		} catch (IOException e1) {
			System.err.println("Sender: IOException while sending control packet!");
			e1.printStackTrace();
		} 
		sendBuffer.enter(controlPacket);
		startTimer(controlPacket);
		
		
		
		nextSeqNum = nextSeqNum + 1; 
		
		byte[] packetData = new byte[UDP_PACKET_SIZE];
		
		boolean endOfFileReached = false; 
		
		while (!endOfFileReached) { //Datagramme verschicken
			int readStatus = 0; //status, da wenn diese variable -1 ist, das dateiende erreicht wurde.
			try {
				//Daten fuer naechstes zu verschickenes paket abholen
				readStatus = fileToSend.read(packetData, 0, UDP_PACKET_SIZE); 
			} catch (IOException e) {
				System.err.println("Sender: IOException while reading from fileToSend");
				e.printStackTrace();
			} 
			
			if (readStatus == END_OF_FILE)  {
				System.out.println("Sender: Reached end of file");
				endOfFileReached = true; //Datei komplett eingelesen
			}
			else {
				FCpacket dataPacket = makeDataPacket(nextSeqNum,packetData);
				try {
					sendToServer(dataPacket);
				} catch (SocketException e) {
					System.err.println("Sender: SocketException while sending to Server");
					e.printStackTrace();
				} catch (IOException e) {
					System.err.println("Sender: Other IOException while sending to Server");
					e.printStackTrace();
				}
				sendBuffer.enter(dataPacket);
				
				startTimer(dataPacket);
				
				nextSeqNum = nextSeqNum + 1;
			}
		}
	}

	private FCpacket makeDataPacket(long seqNum,byte[] packetData) {
		 return new FCpacket(seqNum, packetData, UDP_PACKET_SIZE);
	}
  }
  
  class ACKReciever extends Thread { 
	  private DatagramSocket fromServer = toServer;
	  
	  @Override
	  public void run() {
		  
		   DatagramPacket recvPacket = new DatagramPacket(new byte[UDP_PACKET_SIZE], UDP_PACKET_SIZE); 
		try {
			fromServer.receive(recvPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		FCpacket fcReceivePacket = new FCpacket(recvPacket.getData(), recvPacket.getLength());
		
		if (sendBuffer.contains(fcReceivePacket) ) {
			fcReceivePacket.setValidACK(true); //Markiere Paket n als quittiert
			
			fcReceivePacket.getTimer().interrupt(); //Timer für Paket n stoppen
			
			timeoutValue = computeTimeoutValue(sampleRTT); //Timeoutwert mit gemessener RTT für Paket n neu berechnen
			
			if(fcReceivePacket.getSeqNum() == sendBuffer.peek().getSeqNum()) { //Wenn n = sendbase,
				 /* dann lösche ab n alle Pakete, bis ein noch nicht quittiertes Paket im Sen-
				 depuffer erreicht ist, und setze sendbase auf dessen Sequenznummer */
				while(sendBuffer.peek().isValidACK()) {
					sendBuffer.remove();
				}
				/*Das setzen von sendbase entfällt, da sendBase immer das erste element
				 * des Puffers referenziert, den "head". 
				 */
				
			}
			
			
		}
		   
		   


		  
	  }
  }
}  
