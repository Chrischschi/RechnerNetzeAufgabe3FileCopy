package util.huebner;

/* Consumer.java
 Version 1.0
 Autor: M. H�bner
 Zweck: Code der Verbraucher-Threads f�r ein Erzeuger/Verbrauchersystem
 */
import java.util.*;

public class Consumer extends Thread {
  /* Code der Verbraucher-Threads f�r ein Erzeuger/Verbrauchersystem */

  public final int MAX_IDLE_TIME = 100;  // max. Pause zwischen den Pufferzugriffen in ms

  private BoundedBuffer<Date> currentBuffer;
  private Date item;

  /* Konstruktor mit �bergabe des Puffers */
  public Consumer(BoundedBuffer<Date> buffer) {
    currentBuffer = buffer;
  }

  public void run() {
    /*
     * Entnimm ein Date-Objekt aus dem Puffer. Nach jeder Entnahme f�r eine
     * Zufallszeit anhalten.
     */

    while (!isInterrupted()) {
      statusmeldungZugriffswunsch();
      // Date-Objekt dem Puffer entnehmen, dazu Puffer-Zugriffsmethode
      // aufrufen --> Synchronisation �ber den Puffer!
      item = currentBuffer.remove();

      if (!isInterrupted()) {
        /* F�r unbestimmte Zeit anhalten */
        pause();
      }
    }
  }

  public void statusmeldungZugriffswunsch() {
    /* Gib einen Zugriffswunsch auf der Konsole aus */
    System.err.println("                                           "
        + this.getName() + " m�chte auf den Puffer zugreifen!");
  }

  public void pause() {
    /*
     * Verbraucher benutzen diese Methode, um f�r eine Zufallszeit unt�tig
     * zu sein
     */
    int sleepTime = (int) (MAX_IDLE_TIME * Math.random());

    try {
      // Thread blockieren
      Thread.sleep(sleepTime);
    } catch (InterruptedException e) {
      // Erneutes Setzen des Interrupt-Flags f�r den eigenen Thread
      this.interrupt();
    }
  }
}
