package fr.xtof54.jsgo;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import android.widget.Toast;

/**
 * WebSocket client:
 * used to connect to a WS server that gets informed of moved played by opponents, and immediately
 * warns the appropriate client of the new move.
 * This is realized by establishing a long-term connection with the server and maintaining this connection
 * alive with low-cost ping/pongs -TODO
 * 
 * @author xtof
 *
 */
public class WSclient {
	final String server = "ws://192.168.43.1:8080";
	final static byte CMD_USERID = 0;
	final static byte CMD_MOVE = 1;
	private final static byte WSCLIENT_VERSION = 1; // useful for the server to be back-compatible with multiple old versions

	private static boolean doConnect = true;
	private WebSocketClient client=null;
	boolean isConnected = false;
	private ArrayList<int[]> movesAlreadySent = new ArrayList<int[]>();
	private static WSclient wsclient = null;
	private static int uid = -1;
	
	public static void init(int userid) {
		uid=userid;
		if (!checkDoConnect()) return;
		System.out.println("WSCLIENT "+wsclient);
		if (wsclient==null) wsclient=new WSclient(userid);
	}
	
	public static void setConnect(boolean selected) {
		doConnect=selected;
		checkDoConnect();
	}
	
	final static int intsize=4, charsize=2;

	private static boolean sendMove(int gameid, int moveid, String move, int oppid, boolean retry) {
		if (!checkDoConnect()) return false;
		if (wsclient==null) {
			System.out.println("ERROR WSCLIENT cannot send moves");
			return false;
		}
		ByteBuffer bb = ByteBuffer.allocate(1+3*intsize+move.length()*charsize);
		bb.put(CMD_MOVE);
		bb.putInt(gameid);
		bb.putInt(moveid);
		bb.putInt(oppid);
		for (int i=0;i<move.length();i++) bb.putChar(move.charAt(i));
		try {
			wsclient.client.send(bb.array());
			int[] s = {gameid,moveid};
			boolean alreadyin=false;
			for (int i=0;i<wsclient.movesAlreadySent.size();i++)
				if (wsclient.movesAlreadySent.get(i)[0]==gameid) {
					wsclient.movesAlreadySent.set(i, s);
					alreadyin=true; break;
				}
			if (!alreadyin) wsclient.movesAlreadySent.add(s);
			return true;
		} catch (Exception e) {
			if (retry) {
				problemConnect();
				return sendMove(gameid, moveid, move, oppid, false);
			} else abortConnect();
			return false;
		}
	}
	public static boolean sendMove(int gameid, int moveid, String move, int oppid) {
		return sendMove(gameid, moveid, move, oppid, true);
	}
	
	// ===================================================================

	private void gotMove(ByteBuffer bb) {
		switch (bb.get(0)) {
		case CMD_MOVE:
			int gameid = bb.getInt();
			int moveid = bb.getInt();
			// The server should send the move only to the opponent, but I add this in there just in case the sender also receives its own move
			for (int i=0;i<movesAlreadySent.size();i++)
				if (movesAlreadySent.get(i)[0]==gameid && movesAlreadySent.get(i)[1]==moveid) return;
			String move="";
			while (bb.hasRemaining()) move+=bb.getChar();
			System.out.println("got move from server gid="+gameid+" "+moveid+" "+move);
			// TODO: send a receipt ?
			Game.gotOpponentMove(gameid, moveid, move);
			break;
		default:
			System.out.println("ERROR: unknown cmd from server "+bb.get(0));
		}
	}

	private WSclient(int userid) {
		try {
			client = new WebSocketClient( new URI(server), new Draft_17()) {

				@Override
				public void onMessage( String message ) {
					System.out.println("received message "+message);
				}

				@Override
				public void onMessage( ByteBuffer bytes ) {
					gotMove(bytes);
				}

				@Override
				public void onOpen( ServerHandshake handshake ) {
					isConnected=true;
					ByteBuffer bb = ByteBuffer.allocate(2+intsize);
					bb.put(CMD_USERID);
					bb.put(WSCLIENT_VERSION);
					bb.putInt(uid);
					try {
						client.send(bb.array());
					} catch (Exception e) {
						System.out.println("PROBLEM client server just after open ");
						e.printStackTrace();
						abortConnect();
					}
				}

				@Override
				public void onClose( int code, String reason, boolean remote ) {
					isConnected=false;
				}

				@Override
				public void onError( Exception ex ) {
					abortConnect();
					System.out.println("problem connection to client server");
					ex.printStackTrace();
				}
			};
			client.connect();
		} catch ( URISyntaxException ex ) {
			System.out.println(server+ " is not a valid WebSocket URI\n" );
		}
	}

	private static void problemConnect() {
		Toast.makeText(GoJsActivity.main.getApplicationContext(), "Pb w/ client server. retrying...", Toast.LENGTH_SHORT).show();
		wsclient.isConnected=false;
		System.out.println("problem connect to clients server");
		System.out.println("trying reconnect...");
		wsclient.client.close();
		init(uid);
		try {
			Thread.sleep(400);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void abortConnect() {
		Toast.makeText(GoJsActivity.main.getApplicationContext(), "Pb w/ client server. aborting...", Toast.LENGTH_SHORT).show();
		if (wsclient!=null) wsclient.client.close();
		wsclient=null;
		doConnect=false;
	}
	
	private static boolean checkDoConnect() {
		if (!doConnect) {
			if (wsclient!=null) {
				wsclient.client.close();
				wsclient=null;
			}
			return false;
		}
		return true;
	}
}
