package fr.xtof54.jsgo;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.util.Log;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
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
	boolean isConnected = false;
	private ArrayList<int[]> movesAlreadySent = new ArrayList<int[]>();
	private static WSclient wsclient = null;
	private static int uid = -1;
	
	public static void nit(int userid) {
		uid=userid;
		if (wsclient==null) wsclient=new WSclient(userid);
	}
	
	public static void setConnect(boolean selected) {
		doConnect=selected;
	}
	
	final static int intsize=4, charsize=2;

	private static void sendMove(final int gameid, final int moveid, final String move, final int oppid, boolean retry) {
        if (doConnect) {
            Thread push = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Send move to pushserver");
                    HttpParams httpparms = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
                    HttpConnectionParams.setSoTimeout(httpparms, 6000);
                    HttpClient httpclient = new DefaultHttpClient(httpparms);
                    try {
                        String cmd = "http://talc1.loria.fr/users/cerisara/DGSmove.php?v=" +
                                gameid + "Z" + moveid + "Z" + move + "Z" + oppid;
                        Log.i("login", cmd);
                        HttpGet httpget = new HttpGet(cmd);
                        httpclient.execute(httpget);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            push.start();
        }
    }

    public static void sendMove(int gameid, int moveid, String move, int oppid) {
		sendMove(gameid, moveid, move, oppid, true);
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
	}
}
