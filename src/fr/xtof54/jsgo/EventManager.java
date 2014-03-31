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
	enum eventType {loginStarted, loginEnd, downloadListStarted, downloadListEnd, downloadListGamesEnd, downloadGameStarted, downloadGameEnd, GameOK, moveSent};
	
	private static EventManager em = new EventManager();
	public static EventManager getEventManager() {
		return em;
	}
	private EventManager() {}
	
	public interface EventListener {
		public void reactToEvent();
	}
	HashMap<eventType, List<EventListener>> listeners = new HashMap<EventManager.eventType, List<EventListener>>();
	
	public void registerListener(eventType e, EventListener f) {
		List<EventListener> l = listeners.get(e);
		if (l==null) {
			l=new ArrayList<EventManager.EventListener>();
			listeners.put(e, l);
		}
		l.add(f);
	}
	public void unregisterListener(eventType e, EventListener f) {
		List<EventListener> l = listeners.get(e);
		if (l!=null) {
			l.remove(f);
			if (l.size()==0) listeners.remove(e);
		}
	}
	
	public void sendEvent(eventType e) {
		List<EventListener> l = listeners.get(e);
		System.out.println("Event sent: "+e+" "+l);
		if (l!=null) {
		    // make a copy of the listeners list to avoid concurrent modification of the original list
		    ArrayList<EventListener> list = new ArrayList<EventManager.EventListener>();
		    list.addAll(l);
            for (EventListener f : list)
                f.reactToEvent();
		}
	}
}
