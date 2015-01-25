package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import fr.xtof54.dragonGoApp.R;
import fr.xtof54.jsgo.EventManager.eventType;
import fr.xtof54.jsgo.GoJsActivity.guistate;

public class Game {
	final static String cmdGetListOfGames = "quick_do.php?obj=game&cmd=list&view=status";

	public static ArrayList<Game> games2play = new ArrayList<Game>();
	public static Game gameShown = null;

	private int gid;
	private JSONArray gameinfo;
	List<String> sgf = null;
	int moveid, boardsize; // moveid comes from the sgf file
	public String oppMove = null;
	public int newMoveId=0; // newMoveId comes from the Status games sent by the server: TODO: this should be the only one !
	// contains the last dead stones used to compute the score
	// if the player accepts this score, then these same dead stones are sent again with command AGREE
	String deadstInSgf=null;
	// contains the new stones that should be marked as dead
	String deadstProposal=null;

	private CharSequence msg = null;

	public CharSequence getMessage() {return msg;}
	public void setMessage(CharSequence m) {msg=m;}
	
	public static Game createDebugGame() {
		Game g = new Game(null, 1);
		games2play.add(g);
		gameShown = g;
		return g;
	}

	static void savedGameChosen(final File sgffile, final int gid) {
		class ConfirmDialogFragment extends DialogFragment {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout
				View v = inflater.inflate(R.layout.error, null);
				// Add action buttons
				builder.setView(v).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						ConfirmDialogFragment.this.getDialog().cancel();
					}
				})
				.setPositiveButton("Show", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Game g = new Game(null, gid	);
						g.loadSGFLocally();
						g.prepareGame();
						ConfirmDialogFragment.this.getDialog().cancel();
						GUI.getGUI().showHome();
						GoJsActivity.main.showGame(g);
						GoJsActivity.main.changeState(guistate.play);
					}
				})
				.setNeutralButton("Remove", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						sgffile.delete();
						ConfirmDialogFragment.this.getDialog().cancel();
						GUI.getGUI().showHome();
					}
				});
				builder.setTitle("Choice");
				TextView tv = (TextView)v.findViewById(R.id.errormsg);
				tv.setText("What do you want to do ?");
				return builder.create();
			}
		}
		ConfirmDialogFragment confirmDialog = new ConfirmDialogFragment();
		confirmDialog.show(GoJsActivity.main.getSupportFragmentManager(),"SavedGameChoice");
	}
	
	public static void showListSaved() {
		Thread forumthread = new Thread(new Runnable() {
			@Override
			public void run() {
				showListSaved2();
			}
		});
        forumthread.start();
	}
	private static void showListSaved2() {
		final File d = GoJsActivity.main.eidogodir;//+"/mygame"+gid+".sgf";
		final File[] savedGames = d.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.startsWith("mygame") && arg1.endsWith(".sgf");
			}
		});
		if (savedGames==null||savedGames.length==0) {
			GoJsActivity.main.showMessage("no game saved");
			return;
		}
		GoJsActivity.main.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				GoJsActivity.main.setContentView(R.layout.forumcats);
				GoJsActivity.main.curstate=guistate.forums;
				final String[] c = new String[savedGames.length+1];
				c[0]="remove all";
				for (int i=0;i<c.length-1;i++) {
					String s=savedGames[i].getName().substring(6).replace(".sgf", "");
					c[i+1]=s;
				}
		        ArrayAdapter<String> adapter = new ArrayAdapter<String>(GoJsActivity.main, R.layout.detlistitem, c);
				final ListView listFrameview = (ListView)GoJsActivity.main.findViewById(R.id.forumCatsList);
				listFrameview.setAdapter(adapter);
				listFrameview.setOnItemClickListener(new OnItemClickListener() {
		            @Override
		            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		            	if (position==0) {
		            		for (File f: savedGames) f.delete();
		            		System.out.println("all files deleted");
							GUI.getGUI().showHome();
		            		return;
		            	}
		            	final int n=position-1;
		            	Thread gameselthread = new Thread(new Runnable() {
							@Override
							public void run() {
				            	Game.savedGameChosen(savedGames[n],Integer.parseInt(c[n+1]));
							}
						});
		            	gameselthread.start();
		            }
		        });
			}
		});
	}
	
	public void addSgfData(String sgfdata) {
		if (sgf==null) sgf = new ArrayList<String>();
		sgf.add(sgfdata);
	}
	
	public static void parseJSONStatusGames(JSONObject o) {
		if (o==null) return;
		try {
			games2play.clear();
			int ngames = o.getInt("list_size");
			if (ngames>0) {
				JSONArray headers = o.getJSONArray("list_header");
				int gid_jsonidx=-1, movejsoni=-1, moveidjson=-1;
				for (int i=0;i<headers.length();i++) {
					String h = headers.getString(i);
					System.out.println("jsonheader "+i+" "+h);
					if (h.equals("id")) gid_jsonidx=i;
					else if (h.equals("move_last")) movejsoni=i;
					else if (h.equals("move_id")) moveidjson=i;
				}
				JSONArray jsongames = o.getJSONArray("list_result");
				for (int i=0;i<jsongames.length();i++) {
					JSONArray jsongame = jsongames.getJSONArray(i);
					int gameid = jsongame.getInt(gid_jsonidx);
					Game g = new Game(jsongame, gameid);
					if (movejsoni>=0&&moveidjson>=0) g.setOppMove(jsongame.getString(movejsoni),jsongame.getInt(moveidjson));
					games2play.add(g);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadStatusGames(final ServerConnection server) {
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener f = new EventManager.EventListener() {
			@Override
			public synchronized void reactToEvent() {
				JSONObject o = server.o;
				parseJSONStatusGames(o);
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
		try {
			if (gameinfo==null||gameinfo.length()<5) return "";
            return gameinfo.getString(4);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
	}

	public int getGameID() {return gid;}

	public void showGame() {
	    System.out.println("writing game sgf to example.html");
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
		System.out.println("example.html up to date");
	}
	/**
	 * Warning: once a first player has marked stone and send his agreement to the server, the dead stones are stored in the server
	 * and the new game status is SCORE2: this should be the correct way to detect this stage.
	 * And we cannot any more send new dead stones to know the score (?)
	 */
	private void checkIfDeadStonesMarked() {
		// start from the end, because there may be several sets of dead stones marked
		for (int i=sgf.size()-1;i>=0;i--) {
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

	public boolean isInScoring() {
	    if (getGameStatus().startsWith("SCORE")) {
	        return true;
	    }
	    return false;
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

	private void saveSGFLocally() {
		try {
			PrintWriter fout = new PrintWriter(new FileWriter(GoJsActivity.main.eidogodir+"/mygame"+gid+".sgf"));
			for (int i=0;i<sgf.size();i++) fout.println(sgf.get(i));
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public int countMovesInSgf() {
		int n=0;
		for (int i=0;i<sgf.size();i++) {
			if (sgf.get(i).length()>0&&sgf.get(i).charAt(0)==';') n++;
		}
		return n;
	}
	public boolean loadSGFLocally(String fname) {
		File f0 = new File(fname);
		if (!f0.exists()) return false;
		try {
			BufferedReader f = new BufferedReader(new FileReader(f0));
			sgf = new ArrayList<String>();
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				sgf.add(s);
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	private boolean loadSGFLocally() {
		final String fname = GoJsActivity.main.eidogodir+"/mygame"+gid+".sgf";
		return loadSGFLocally(fname);
	}
	void addResignToSGF() {
		// TODO
	}
	// it is always called just after addmove or addresign
	void addMessageToSGF(String msg) {
		sgf.add(sgf.size()-1, "C["+msg.trim()+"]");
		saveSGFLocally();
	}
	void addMoveToSGF(String move) {
		char oppColor = sgf.get(sgf.size()-2).charAt(1);
		char myColor = oppColor=='W'?'B':'W';
		sgf.add(sgf.size()-1, ";"+myColor+"["+move+"]");
		saveSGFLocally();
	}
	void setOppMove(String move, int mid) {
		oppMove=move; newMoveId=mid;
	}
	
	void prepareGame() {
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
		final EventManager em = EventManager.getEventManager();
		em.sendEvent(eventType.GameOK);
	}
	
	// est-ce qu'il faut garder le meme httpclient qu'avant pour pouvoir beneficier du proxy ? ==> OUI
	public void downloadGame(final ServerConnection server) {
		final EventManager em = EventManager.getEventManager();
		if (loadSGFLocally()) {
			em.sendEvent(eventType.downloadGameStarted);
			if (oppMove!=null) {
				// This game has already been created, for instance by sownloading tatusGame from DGS
				/*
				 * It can happen that the user plays a move, so the local sgf is updated (but not the moveid !),
				 * but the move is not sent correctly to the server. Then, next time status games are downloaded,
				 * the newMoveId will actually be late by 2 moves, as compared to the *actual* nb of moves in the sgf.
				 * Then, we must warn the user, and propose him to resend his move or rethink about it.
				 */
				int nActualMovesInSgf = countMovesInSgf();
				if (newMoveId==nActualMovesInSgf-2) {
					// the user last move has not been received by the DGS server
					GoJsActivity.main.showMessage("last move not received by server: you may resend it");
				} else if (newMoveId==nActualMovesInSgf) {
					// we got a new move from the server
					addMoveToSGF(oppMove); // and save
					GoJsActivity.main.showMessage("detected a new move from server");
				} else if (newMoveId==nActualMovesInSgf-1) {
					GoJsActivity.main.showMessage("No new move from server");
					// we didn't got any new move from the server
				} else {
					GoJsActivity.main.showMessage("ERROR nmoves");
					System.out.println("ERROR: strange nmoves "+getGameID()+" "+nActualMovesInSgf+" "+newMoveId);
					// TODO: reload the sgf from the server
				}
				
				if (false) {
					// another check
					int nmoves=0;
					for (int i=0;i<sgf.size();i++) {
						if (sgf.get(i).startsWith("XM[")) {
							String xx =sgf.get(i).substring(3).replace(']', ' ').trim();
							nmoves = Integer.parseInt(xx);
							sgf.set(i,"XM["+newMoveId+"]");
							break;
						}
					}
					/*
					 * nmoves is the nb of moves indicated in the sgf file:
					 * when playing a move and sending it, this nb is not updated in the SGF file;
					 * so the next time status games are downloaded, the newMoveId will be increased by 2 moves compared to this nb in the sgf file.
					 * If this is so, then we just update the sgf file with the newly downloaded move and newMoveId.
					 * Hence, if this game is just skipped, next time status games are downloaded, we can detect that the sgf
					 * file should not be updated because this nb will match the one downloaded.
					 */
					if (nmoves==newMoveId) { // games already download and updated, but not played; just keep it like that
					} else if (newMoveId==nmoves+2) {
						addMoveToSGF(oppMove); // and save
					} else {
						System.out.println("ERROR: nmoves newMoveId "+nmoves+" "+newMoveId+" "+sgf.size());
					}
				}
			}
			em.sendEvent(eventType.downloadGameEnd);
			prepareGame();
			GoJsActivity.main.showMessage("game loaded locally");
			return;
		}
		EventManager.EventListener f = new EventManager.EventListener() {
			@Override
			public synchronized void reactToEvent() {
				em.unregisterListener(eventType.downloadGameEnd, this);
				sgf = new ArrayList<String>();
				for (String s : server.sgf) sgf.add(""+s);
				saveSGFLocally();
				prepareGame();
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
		System.out.println("acceptScore deadstones "+deadstProposal);
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
		if (server==null) {
			if (!GoJsActivity.main.initServer()) return;
			sendMove2server(move, GoJsActivity.main.server);
			return;
		}
		if (move.toLowerCase().startsWith("tt")) move="pass";
		System.out.println("move "+move);
		final String finmove = move;
		
		// ask for confirmation before sending
		class ConfirmDialogFragment extends DialogFragment {
			String cmdSentBeforeNetErr;
			eventType eventTobesent;
			GoJsActivity main;
			public void setArguments(String s, eventType e, GoJsActivity m) {
				cmdSentBeforeNetErr = s;
				eventTobesent = e;
				main=m;
			}
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout
				View v = inflater.inflate(R.layout.error, null);
				// Add action buttons
				builder.setView(v).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						ConfirmDialogFragment.this.getDialog().cancel();
					}
				})
				.setPositiveButton("OK, send !", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						addMoveToSGF(finmove);
						String cmd = "quick_do.php?obj=game&cmd=move&gid="+getGameID()+"&move_id="+newMoveId+"&move="+finmove;
						if (msg!=null) {
							addMessageToSGF(msg.toString());
						    cmd+="&msg="+URLEncoder.encode(msg.toString());
						}
						// TODO: check that server exists
						server.sendCmdToServer(cmd,eventType.moveSentStart,eventType.moveSentEnd);
						ConfirmDialogFragment.this.getDialog().cancel();
					}
				});
				builder.setTitle("Confirmation");
				TextView tv = (TextView)v.findViewById(R.id.errormsg);
				// transform internal move rep to user-type moves:
				String usermove = finmove;
				if (finmove.length()==2) {
					char col=Character.toUpperCase(finmove.charAt(0));
					if (Character.toLowerCase(finmove.charAt(0))>='i') {
						col=(char)((int)col+1);
					}
					int row = boardsize-((int)Character.toUpperCase(finmove.charAt(1))-(int)'A');
					usermove=""+col;
					usermove+=row;
				}
				tv.setText("Confirm move "+usermove+" ?");
				return builder.create();
			}
		}
		ConfirmDialogFragment confirmDialog = new ConfirmDialogFragment();
		confirmDialog.show(GoJsActivity.main.getSupportFragmentManager(),"confirmBeforeSend");
	}
}
