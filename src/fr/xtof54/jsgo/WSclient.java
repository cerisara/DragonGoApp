package fr.xtof54.jsgo;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

/**
 * WebSocket client:
 * used to connect to a WS server that gets informed of moved played by opponents, and immediately
 * warns the appropriate client of the new move.
 * This is realized by establishing a long-term connection with the server and maintaining this connection
 * alive with low-cost ping/pongs
 * 
 * @author xtof
 *
 */
public class WSclient {
	final String server = "ws://127.0.0.1:8080";
	final byte CMD_GAMEIDS = 0;
	final byte CMD_MOVE = 1;

	private WebSocketClient client=null;
	boolean isConnected = false;
	private ArrayList<int[]> justSent = new ArrayList<int[]>();

	private static WSclient wsclient = null;
	public static WSclient getWSclient() {
		if (wsclient==null) wsclient=new WSclient();
		return wsclient;
	}
	
	public boolean sendGameIDs(int[] gameIDS) {
		ByteBuffer bb = ByteBuffer.allocate(1);
		bb.put(CMD_GAMEIDS);
		for (int i=0;i<gameIDS.length;i++) bb.putInt(gameIDS[i]);
		try {
			client.send(bb.array());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean sendMove(int gameid, int moveid, String move) {
		ByteBuffer bb = ByteBuffer.allocate(1);
		bb.put(CMD_MOVE);
		bb.putInt(gameid);
		bb.putInt(moveid);
		for (int i=0;i<move.length();i++) bb.putChar(move.charAt(i));
		try {
			client.send(bb.array());
			int[] s = {gameid,moveid};
			boolean alreadyin=false;
			for (int i=0;i<justSent.size();i++)
				if (justSent.get(i)[0]==gameid) {
					justSent.set(i, s);
					alreadyin=true; break;
				}
			if (!alreadyin) justSent.add(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void gotMove(ByteBuffer bb) {
		switch (bb.get(0)) {
		case CMD_MOVE:
			int gameid = bb.getInt();
			int moveid = bb.getInt();
			// The server should send the move only to the opponent, but I add this in there just in case the sender also receives its own move
			for (int i=0;i<justSent.size();i++)
				if (justSent.get(i)[0]==gameid && justSent.get(i)[1]==moveid) return;
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

	private WSclient() {
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
				}

				@Override
				public void onClose( int code, String reason, boolean remote ) {
					isConnected=false;
				}

				@Override
				public void onError( Exception ex ) {
					ex.printStackTrace();
				}
			};
			client.connect();
		} catch ( URISyntaxException ex ) {
			System.out.println(server+ " is not a valid WebSocket URI\n" );
		}
	}
}
