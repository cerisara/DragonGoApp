package fr.xtof54.jsgo;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.xtof54.jsgo.EventManager.eventType;

public class Game {
	final static String cmdGetListOfGames = "quick_do.php?obj=game&cmd=list&view=status";

	private static ArrayList<Game> games2play = new ArrayList<Game>();
	public static Game gameShown = null;

	private int gid;
	private JSONArray gameinfo;
	List<String> sgf = null;
	int moveid, boardsize;
	// contains the last dead stones used to compute the score
	// if the player accepts this score, then these same dead stones are sent again with command AGREE
	String deadstInSgf=null;
	// contains the new stones that should be marked as dead
	String deadstProposal=null;

	static void createDebugGame() {
		Game g = new Game(null, 1);
		games2play.add(g);
		gameShown = g;
	}

	public static void loadStatusGames(final ServerConnection server) {
		games2play.clear();
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener f = new EventManager.EventListener() {
			@Override
			public synchronized void reactToEvent() {
				JSONObject o = server.o;
				if (o==null) return;
				try {
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
				} catch (JSONException e) {
					e.printStackTrace();
				}
				System.out.println("end of loadstatusgame, unregistering listener "+games2play.size());
				em.unregisterListener(eventType.downloadListEnd, this);
				em.sendEvent(eventType.downloadListGamesEnd);
			}
			@Override
			public String getName() {return "loadStatusGame";}
		};
		em.registerListener(eventType.downloadListEnd, f);
		server.sendCmdToServer(cmdGetListOfGames,eventType.downloadListStarted,eventType.downloadListEnd);
	}

	public static List<Game> getGames() {return games2play;}

	Game(JSONArray gameObject, int gameid) {
		gid=gameid;
		gameinfo = gameObject;
	}

	public String getGameStatus() {
		return gameinfo.getString(4);
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
		gameShown=this;
	}
	/**
	 * Warning: once a first player has marked stone and send his agreement to the server, the dead stones are stored in the server
	 * and the new game status is SCORE2: this should be the correct way to detect this stage.
	 * And we cannot any more send new dead stones to know the score (?)
	 */
	private void checkIfDeadStonesMarked() {
		for (int i=0;i<sgf.size();i++) {
			String s=sgf.get(i);
			int j=s.indexOf("MA[");
			if (j>=0) {
				// there are dead stones marked !
				deadstInSgf="";
				j+=3;
				while (j<s.length()) {
					deadstInSgf+=s.substring(j,j+2);
					if (j+3>=s.length()||s.charAt(j+3)!='[') break;
					j+=4;
				}
				break;
			}
		}
	}
	// should never be used ?
	public void removeDeadStonesFromSgf() {
		deadstInSgf=null;
		int debdeadstones=-1, enddeadstones=-1, deadidx=-1;
		for (int i=0;i<sgf.size();i++) {
			String s=sgf.get(i);
			int j=s.indexOf("MA[");
			if (j>=0) {
				// there are dead stones marked !
				debdeadstones=j; deadidx=i;
				j+=3;
				while (j<s.length()) {
					if (j+3>=s.length()||s.charAt(j+3)!='[') break;
					j+=4;
				}
				enddeadstones=j+4;
				break;
			}
		}
		if (debdeadstones>=0) {
			String s = sgf.get(deadidx);
			s=s.substring(0, debdeadstones);
			sgf.set(deadidx, s);
		}
	}

	public void finishedWithThisGame() {
		games2play.remove(this);
		gameShown=null;
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
	public void downloadGame(final ServerConnection server) {
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener f = new EventManager.EventListener() {
			@Override
			public synchronized void reactToEvent() {
				em.unregisterListener(eventType.downloadGameEnd, this);
				sgf = new ArrayList<String>();
				for (String s : server.sgf) sgf.add(""+s);
				// look for move_id and size
				for (String s: sgf) {
					int i=s.indexOf("XM[");
					if (i>=0) {
						int j=s.indexOf(']',i+3);
						moveid = Integer.parseInt(s.substring(i+3, j));
					}
					i=s.indexOf("SZ[");
					if (i>=0) {
						int j=s.indexOf(']',i+3);
						boardsize = Integer.parseInt(s.substring(i+3, j));
					}
				}
				checkIfDeadStonesMarked();
				System.out.println("sgf: "+sgf);
				System.out.println("moveid "+moveid);
				System.out.println("deadstones in SGF: "+deadstInSgf);
				em.sendEvent(eventType.GameOK);
			}
			@Override
			public String getName() {return "downloadGame";}
		};
		em.registerListener(eventType.downloadGameEnd, f);
		server.downloadSgf(gid, true);
	}

	public int getBoardSize() {return boardsize;}

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

	public void acceptScore(final ServerConnection server) {
		System.out.println("deadstones "+deadstProposal);
		String cmd = "quick_do.php?obj=game&cmd=score&gid="+getGameID()+"&toggle=uniq&move="+deadstProposal+"&move_id="+moveid;
		server.sendCmdToServer(cmd,eventType.moveSentStart,eventType.moveSentEnd);
	}

	/**
	 * In SCORE mode, we can send directly the stones marked by the user,
	 * but in SCORE2 mode, we have to toggle the stones marked with the stones on the server !
	 * 
	 * 
	 * @param deadstones are the stones marked with X on the goban by the user
	 * @param server
	 */
	public void sendDeadstonesToServer(String deadstones, final ServerConnection server, boolean toggle) {
		System.out.println("deadstones on the goban"+deadstones);
		if (getGameStatus().equals("SCORE2")) {
			if (!toggle) {
				deadstProposal = ""+deadstones;
			} else {
				// compute the dead stones proposal
				deadstProposal="";
				ArrayList<String> stonesRemovedFromGoban = new ArrayList<String>();
				for (int j=0;j<deadstInSgf.length();j+=2) {
					String sgfMarkedStone = deadstInSgf.substring(j,j+2);
					stonesRemovedFromGoban.add(sgfMarkedStone);
				}
				for (int i=0;i<deadstones.length();i+=2) {
					String gobanMarkedStone = deadstones.substring(i,i+2);
					// is this stone already marked in the SGF ?
					boolean isStoneMarkedInSgf = false;
					int j;
					for (j=0;j<stonesRemovedFromGoban.size();j++) {
						String s=stonesRemovedFromGoban.get(j);
						if (s.equals(gobanMarkedStone)) {
							isStoneMarkedInSgf=true;
							break;
						}
					}
					if (isStoneMarkedInSgf) {
						// we don't add it to the proposal, because it's already in there (?)
						stonesRemovedFromGoban.remove(j);
					} else {
						deadstProposal+=gobanMarkedStone;
					}
				}
				for (int j=0;j<stonesRemovedFromGoban.size();j++) {
					// we have to toggle off the stones that are not marked anymore
					deadstProposal+=stonesRemovedFromGoban.get(j);
				}
			}
		} else if (getGameStatus().equals("SCORE")) {
			deadstProposal = ""+deadstones;
		} else {
			System.out.println("ERROR senddeadstones without being in SCORE mode !");
			return;
		}
		System.out.println("deadstones proposal "+deadstProposal);
		String cmd = "quick_do.php?obj=game&cmd=status_score&gid="+getGameID()+"&toggle=uniq&move="+deadstProposal;
		server.sendCmdToServer(cmd,eventType.moveSentStart,eventType.moveSentEnd);
	}

	public void sendMove2server(String move, final ServerConnection server) {
		System.out.println("move "+move);
		String cmd = "quick_do.php?obj=game&cmd=move&gid="+getGameID()+"&move_id="+moveid+"&move="+move;
		if (move.toLowerCase().startsWith("tt")) {
			// pass move
			cmd = "quick_do.php?obj=game&cmd=move&gid="+getGameID()+"&move_id="+moveid+"&move=pass";
		}
		server.sendCmdToServer(cmd,eventType.moveSentStart,eventType.moveSentEnd);
	}
}
