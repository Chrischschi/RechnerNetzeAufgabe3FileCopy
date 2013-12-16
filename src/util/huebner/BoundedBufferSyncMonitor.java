package util.huebner;

/* BoundedBufferMonitor.java
 Version 1.0
 Autor: M. H�bner
 Zweck: Stellt einen generischen Datenpuffer mit Zugriffsmethoden und 
 Synchronisation �ber Java-Monitor des Puffers zur Verf�gung 
 */
import java.util.*;

public class BoundedBufferSyncMonitor<E> implements BoundedBuffer<E> {
	/* Datenpuffer f�r Elemente vom Typ E mit Zugriffsmethoden enter und remove */
	private int bufferMaxSize; // maximale Puffergr��e
	private LinkedList<E> buffer; // Liste als Speicher

	/* Konstruktor */
	public BoundedBufferSyncMonitor(int bufferSize) {
		bufferMaxSize = bufferSize;
		buffer = new LinkedList<E>();
	}

	/* Producer (Erzeuger) rufen die Methode ENTER auf */
	public synchronized void enter(E item) {
		/*
		 * Pufferzugriff sperren (bzw. ggf. auf Zugriff warten): geschieht
		 * automatisch durch Monitor-Eintritt ("synchronized" entspricht
		 * synchronized(this){...})
		 */

		/* Solange Puffer voll ==> warten! */
		while (buffer.size() == bufferMaxSize) {
			try {
				this.wait(); // --> Warten in der Wait-Queue und Monitor des
								// Puffers freigeben
			} catch (InterruptedException e) {
				/*
				 * Ausf�hrender Thread hat Interrupt erhalten --> Interrupt-Flag
				 * im ausf�hrenden Thread setzen und Methode beenden
				 */
				Thread.currentThread().interrupt();
				return;
			}
		}
		/* Item zum Puffer hinzuf�gen */
		buffer.add(item);
		System.err
				.println("          ENTER: "
						+ Thread.currentThread().getName()
						+ " hat ein Objekt in den Puffer gelegt. Aktuelle Puffergr��e: "
						+ buffer.size());

		/*
		 * Wartenden Consumer wecken --> es m�ssen ALLE Threads geweckt werden
		 * (evtl. auch andere Producer), da es nur eine Wait-Queue gibt!
		 */
		this.notifyAll();

		/*
		 * Pufferzugriff entsperren und ggf. Threads in Monitor-Queue wecken:
		 * geschieht automatisch durch Monitor-Austritt
		 */
	}

	/* Consumer (Verbraucher) rufen die Methode REMOVE auf */
	public synchronized E remove() {
		/*
		 * Pufferzugriff sperren (bzw. ggf. auf Zugriff warten): geschieht
		 * automatisch durch Monitor-Eintritt ("synchronized" entspricht
		 * synchronized(this){...})
		 */
		E item;

		/* Solange Puffer leer ==> warten! */
		while (buffer.size() == 0) {
			try {
				this.wait(); // --> Warten in der Wait-Queue und Monitor des
							// Puffers freigeben
			} catch (InterruptedException e) {
				/*
				 * Ausf�hrender Thread hat Interrupt erhalten --> Interrupt-Flag
				 * im ausf�hrenden Thread setzen und Methode beenden
				 */
				Thread.currentThread().interrupt();
				return null;
			}
		}
		/* Item aus dem Buffer entfernen */
		item = buffer.removeFirst();
		System.err
				.println("          REMOVE: "
						+ Thread.currentThread().getName()
						+ " hat ein Objekt aus dem Puffer entnommen. Aktuelle Puffergr��e: "
						+ buffer.size());

		/*
		 * Wartenden Producer wecken --> es m�ssen ALLE Threads geweckt werden
		 * (evtl. auch andere Consumer), da es nur eine Wait-Queue gibt!
		 */
		this.notifyAll();

		return item;
		/*
		 * Pufferzugriff entsperren und ggf. Threads in Monitor-Queue wecken:
		 * geschieht automatisch durch Monitor-Austritt
		 */
	}

	@Override
	public synchronized E peek() {
		return buffer.getFirst();
	}

	@Override
	public synchronized E get(int index) {
		return buffer.get(index);
	}

	@Override
	public synchronized boolean isEmpty() {
		return buffer.isEmpty();
	}
}
