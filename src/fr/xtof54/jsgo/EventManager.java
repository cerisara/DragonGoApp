package fr.xtof54.jsgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is used to receive and dispatch events
 * 
 * @author xtof
 *
 */
public class EventManager {
	enum eventType {loginStarted, loginEnd, downloadListStarted, downloadListEnd, downloadListGamesEnd, downloadGameStarted, downloadGameEnd, GameOK, moveSentStart, moveSentEnd, gobanReady,
		msgSendStart, msgSendEnd, ladderStart, ladderEnd, ladderChallengeStart, ladderChallengeEnd, showMessage, copyEidogoStart, copyEidogoEnd};

	private static EventManager em = new EventManager();
	public static EventManager getEventManager() {
		return em;
	}
	private EventManager() {}

	public interface EventListener {
		public void reactToEvent();
		public String getName();
	}
	private HashMap<eventType, List<EventListener>> listeners = new HashMap<EventManager.eventType, List<EventListener>>();

	public void registerListener(eventType e, EventListener f) {
		System.out.println("DGSAPP registering event listener "+e+" "+f.getName());
		List<EventListener> l = listeners.get(e);
		final String name = f.getName();
		if (l==null) {
			l=new ArrayList<EventManager.EventListener>();
			listeners.put(e, l);
		} else {
			for (EventListener el : l)
				if (el.getName().equals(name)) {
					System.out.println("refusing event "+el.getName());
					// refuse to register 2 times the same listener !
					return;
				}
		}
		l.add(f);
	}
	public void unregisterListener(eventType e, EventListener f) {
		System.out.println("DGSAPP unregistering event listener "+e+" "+f.getName());
		List<EventListener> l = listeners.get(e);
		if (l!=null) {
			l.remove(f);
			if (l.size()==0) listeners.remove(e);
		}
	}

	public String message=null;
	public void sendEvent(final eventType e, String msg) {
		message=""+msg;
		sendEvent(e);
	}
	public void sendEvent(final eventType e) {
		Thread tt = new Thread(new Runnable() {
			@Override
			public void run() {
				List<EventListener> l = listeners.get(e);
				System.out.println("DGSAPP Event sent: "+e+" "+l);
				if (l!=null) {
					// make a copy of the listeners list to avoid concurrent modification of the original list
					ArrayList<EventListener> list = new ArrayList<EventManager.EventListener>();
					list.addAll(l);
					for (EventListener f : list)
						f.reactToEvent();
				}
			}
		});
		tt.start();
	}
}
