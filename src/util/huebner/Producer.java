package util.huebner;

/* Producer.java
 Version 1.0
 Autor: M. H�bner
 Zweck: Code der Erzeuger-Threads f�r ein Erzeuger/Verbrauchersystem
 */
import java.util.*;

public class Producer extends Thread {
  /* Code der Erzeuger-Threads f�r ein Erzeuger/Verbrauchersystem */

  public final int MAX_IDLE_TIME = 100; // max. Pause zwischen den
                      // Pufferzugriffen in ms

  private BoundedBuffer<Date> currentBuffer;
  private Date item;

  /* Konstruktor mit �bergabe des Puffers */
  public Producer(BoundedBuffer<Date> buffer) {
    currentBuffer = buffer;
  }

  public void run() {
    /*
     * Erzeuge Date-Objekte und lege sie in den Puffer. Halte nach jeder
     * Ablage f�r eine Zufallszeit an.
     */

    while (!isInterrupted()) {
      /* Date-Objekt erzeugen */
      item = new Date();
      statusmeldungZugriffswunsch();

      // Puffer-Zugriffsmethode aufrufen --> Synchronisation �ber den
      // Puffer!
      currentBuffer.enter(item);

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
     * Erzeuger benutzen diese Methode, um f�r eine Zufallszeit unt�tig zu
     * sein
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
