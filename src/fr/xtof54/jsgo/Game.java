package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

public class Game {
	final static String cmdGetListOfGames = "quick_do.php?obj=game&cmd=list&view=status";

	static ArrayList<Game> games2play = new ArrayList<Game>();
	
    private int gid;
    private JSONArray gameinfo;
    List<String> sgf = null;
    int moveid;
    
    public static int loadStatusGames(ServerConnection server) {
    	JSONObject o = server.sendCmdToServer(cmdGetListOfGames);
    	if (o==null) return -1;
    	int ngames = o.getInt("list_size");
	    if (ngames>0) {
            JSONArray headers = o.getJSONArray("list_header");
            int gid_jsonidx = -1;
            for (int i=0;i<headers.length();i++) {
                String h = headers.getString(i);
                System.out.println("jsonheader "+i+" "+h);
                if (h.equals("id")) gid_jsonidx=i;
            }
            JSONArray jsongames = o.getJSONArray("list_result");
            for (int i=0;i<jsongames.length();i++) {
                JSONArray jsongame = jsongames.getJSONArray(i);
                int gameid = jsongame.getInt(gid_jsonidx);
                Game g = new Game(jsongame, gameid);
                games2play.add(g);
            }
	    }
	    return ngames;
    }
    
    public static List<Game> getGames() {return games2play;}
    
    Game(JSONArray gameObject, int gameid) {
    	gid=gameid;
    	gameinfo = gameObject;
    }
    
    public int getGameID() {return gid;}

    public void showGame() {
    	if (sgf==null) {
    		System.out.println("ERROR impossible to show game "+gid);
    		return;
    	}
    	try {
    		PrintWriter fout = new PrintWriter(new FileWriter(GoJsActivity.main.eidogodir+"/example.html"));
    		for (String s : exampleFileHtmlHeader) fout.println(s);
    		for (int i=0;i<sgf.size();i++) fout.println(sgf.get(i));
    		for (String s : htmlend) fout.println(s);
    		fout.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }
    
    public boolean isTwoPasses() {
    	if (sgf==null) return false;
    	String lastb="12", lastw="32";
    	for (int i=0;i<sgf.size();i++) {
    		String s=sgf.get(i);
    		{
    			int z=s.lastIndexOf("B[");
    			int zz=s.indexOf(']',z);
    			if (z>=0) lastb=s.substring(z+2, zz);
    		}
    		{
    			int z=s.lastIndexOf("W[");
    			int zz=s.indexOf(']',z);
    			if (z>=0) lastw=s.substring(z+2, zz);
    		}
    	}
		// detect if in scoring phase
		System.out.println("debuglastmoves --"+lastw+"--"+lastb);
		if ((lastb.equals("")||lastb.equals("tt")) && ((lastw.equals("")||lastw.equals("tt")))) {
			return true;
		} else return false;
    }
    
    
    // est-ce qu'il faut garder le meme httpclient qu'avant pour pouvoir beneficier du proxy ? ==> OUI
    public void downloadGame(ServerConnection server) {
    	sgf = server.downloadSgf(gid);
    	
		// look for move_id
    	for (String s: sgf) {
    		int i=s.indexOf("XM[");
    		if (i>=0) {
    			int j=s.indexOf(']',i+3);
    			moveid = Integer.parseInt(s.substring(i+3, j));
    		}
    	}
    	
    	System.out.println("sgf: "+sgf);
    	System.out.println("moveid "+moveid);

//	    GoJsActivity.main.runInWaitingThread(new Runnable() {
//			@Override
//			public void run() {
//				HttpGet httpget = new HttpGet(GoJsActivity.main.server+"sgf.php?gid="+gid+"&owned_comments=1&quick_mode=1");
//				try {
//				    HttpParams httpparms = new BasicHttpParams();
//				    HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
//				    HttpConnectionParams.setSoTimeout(httpparms, 6000);
//				    HttpClient httpclient = new DefaultHttpClient(httpparms);
//					HttpResponse response = httpclient.execute(httpget);
//					Header[] heds = response.getAllHeaders();
//					for (Header s : heds)
//						System.out.println("[HEADER] "+s);
//					HttpEntity entity = response.getEntity();
//					if (entity != null) {
//						InputStream instream = entity.getContent();
//						BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
//						for (;;) {
//							String s = fin.readLine();
//							if (s==null) break;
//							s=s.trim();
//							if (s.length()>0&&s.charAt(0)!='[') {
//								// look for move_id
//								int i=s.indexOf("XM[");
//								if (i>=0) {
//									int j=s.indexOf(']',i+3);
//									moveid = Integer.parseInt(s.substring(i+3, j));
//								}
//								sgf.add(s);
//							}
//							System.out.println("SGFdownload "+s);
//						}
//						fin.close();
//					}
//					httpclient.getConnectionManager().shutdown();
//				} catch (Exception e) {
//					GoJsActivity.main.showMsg(GoJsActivity.main.netErrMsg);
//					e.printStackTrace();
//				}
//			}
//	    });
    }

	final String[] exampleFileHtmlHeader = {
			"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"",
			"    \"http://www.w3.org/TR/html4/loose.dtd\">",
			"<html>",
			"<head>",
			"    <title>Xtof54 GoJs</title>",
			"",
			"    <!--",
			"        Optional config - defaults given",
			"    -->",
			"    <script type=\"text/javascript\">",
			"    eidogoConfig = {",
			"        theme:          \"compact\",",
			"        mode:           \"play\",",
			"        showComments:    true,",
			"        showPlayerInfo:  false,",
			"        showGameInfo:    false,",
			"        showTools:       false,",
			"        showOptions:     false,",
			"        markCurrent:     true,",
			"        markVariations:  true,",
			"        markNext:        false,",
			"        problemMode:     false,",
			"        enableShortcuts: false",
			"    };",
			"    </script>",
			"    ",
			"    <!--",
			"        Optional international support (see player/i18n/ folder)",
			"    -->",
			"<script type=\"text/javascript\" src=\"player/js/lang.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/eidogo.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/util.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/i18n/en.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/gametree.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/sgf.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/board.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/rules.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/player.js\"></script>",
			"<script type=\"text/javascript\" src=\"player/js/init.js\"></script>",
			"</head>",
			"<body>",
			"    <div class=\"eidogo-player-auto\">",
	};
	final String[] htmlend = {
			"    </div>",
			"</body>",
			"</html>",
	};

	public static void main(String args[]) {
		String[] c = ServerConnection.loadCredsFromFile("creds.txt");
		ServerConnection server = new ServerConnection(0, c[0], c[1]);
		int ng = Game.loadStatusGames(server);
		System.out.println("ngames "+ng);
		
		Game g = Game.getGames().get(0);
		g.downloadGame(server);
	}
}
