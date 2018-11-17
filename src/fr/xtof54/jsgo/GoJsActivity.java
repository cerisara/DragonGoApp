package fr.xtof54.jsgo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

//import com.google.android.gms.gcm.GoogleCloudMessaging;

import fr.xtof54.jsgo.R;
import fr.xtof54.jsgo.EventManager.eventType;
import fr.xtof54.jsgo.ServerConnection.DetLogger;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

/**
 * TODO:
 * - quicksuite: use status to get in a single command both the games and messages
 * - support new msgs in forums
 * - don't download the sgf at every move, but rather cache the sgf and only use the last move given by status (what about if a comment is made on the last move ?)
 * - better graphics / pictures
 * - init goban zoom from physical screen size
 * 
 * @author xtof
 *
 */
public class GoJsActivity extends FragmentActivity {
	public static final String dgappversion = "1.8";

	private long rx=0, tx=0, rx0=-7, tx0=-7;
	private int uid=-1;
	private Handler mHandler = new Handler();
	private boolean quitall = false;
	private final Runnable mRunnable = new Runnable() {
		public void run() {
			updateTraffic();
			if (!quitall) mHandler.postDelayed(mRunnable, 3000);
		}
	};

	ServerConnection server=null;
	AndroidServerConnection androidServer = null;
	int chosenServer=0, chosenLogin=0;

	//    String server;
	enum guistate {nogame, play, markDeadStones, checkScore, message, review, forums};
	guistate curstate = guistate.nogame;

	public File eidogodir;
	final boolean forcecopy=false;
	WebView wv;
	ArrayList<Game> games2play = new ArrayList<Game>();
	int curgidx2play=0,moveid=0;
	final String netErrMsg = "Connection errors or timeout, you may retry";
	public static GoJsActivity main;
	private int numEventsReceived = 0;

	public boolean getGamesFromDGS=true, getGamesFromOGS=false;

	//	private static void copyFile(InputStream in, OutputStream out) throws IOException {
	//		byte[] buffer = new byte[1024];
	//		int read;
	//		while((read = in.read(buffer)) != -1){
	//			out.write(buffer, 0, read);
	//		}
	//	}

	public static void viewUrl(final String url) {
		viewURL(url);
	}
	private static Thread viewer = null;
	private static ArrayBlockingQueue<String> viewerq = new ArrayBlockingQueue<String>(100);
	public static void viewURL(final String url) {
		try {
			viewerq.put(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (viewer==null) {
			viewer = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						for (;;) {
							final String surl = viewerq.take();
							System.out.println("DGSAPP viewurl "+surl+" "+viewerq.size());
							GoJsActivity.main.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									GoJsActivity.main.wv.loadUrl(surl);
								}
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			viewer.start();
		}
	}

	private void setButtons(final String b1, final String b2, final String b3, final String b4, final String b5) {
		setButtons(b1, b2, b3, b4);
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Button but5 = (Button)findViewById(R.id.but5);
				but5.setText(b5);
			}
		});
	}

	private void setButtons(final String b1, final String b2, final String b3, final String b4) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Button but1 = (Button)findViewById(R.id.but1);
				but1.setText(b1);
				Button but2 = (Button)findViewById(R.id.but2);
				but2.setText(b2);
				Button but3 = (Button)findViewById(R.id.but3);
				but3.setText(b3);
				Button but4 = (Button)findViewById(R.id.but4);
				but4.setText(b4);
			}
		});
	}
	private guistate lastGameState;
	// TODO: because this method calls a WebView method (loadUrl()), all of these calls must be made
	// by the same thread (should it be the main android/app UI loop ?)
	// So, we should apply the same principle as for showMessage
	void changeState(guistate newstate) {
		if (curstate==guistate.review && newstate!=guistate.review)
			GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detMoveNumber()");
		if (curstate==guistate.markDeadStones && newstate!=guistate.markDeadStones)
			GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detmarkp()");
		System.out.println("inchangestate "+curstate+" .. "+newstate);
		switch (newstate) {
			case nogame:
				// we allow clicking just in case the user wants to play locally, disconnected
				writeInLabel("Getgame: download game from DGS");
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
				setButtons("Games","Zm+","Zm-","Msg"); break;
			case play:
				writeInLabel("click on the board to play");
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
				setButtons("Send","Zm+","Zm-","Reset","Bck"); break;
			case markDeadStones:
				writeInLabel("click on the board to mark dead stones");
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
				showMessage("Scoring phase: put one X marker on each dead group and click SCORE to check score (you can still change after)");
				// just in case the board is already rendered...
				// normally, detmarkx() is called right after the board is displayed,
				// but here, the board is displayed long ago, so we have to call it manually
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detmarkx()");
				setButtons("Score","Zm+","Zm-","Play"); break;
			case checkScore: 
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detforbidClicking()");
				setButtons("Accept","Zm+","Zm-","Refuse"); break;
			case message:
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detforbidClicking()");
				lastGameState=curstate;
				setButtons("GetMsg","Invite","SendMsg","Back2game"); break;
			case review:
				setButtons("LastCmt","Zm+","Zm-","ListG","Fwd");break;
			default:
		}
		curstate=newstate;
	}

	public String showCounting(JSONObject o) {
		String sc="";
		try {
			sc = o.getString("score");
			String bt = o.getString("black_territory").trim();
			for (int i=0;i<bt.length();i+=2) {
				String coords = bt.substring(i, i+2);
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TB\",\""+coords+"\")");
			}
			bt = o.getString("white_dead").trim();
			for (int i=0;i<bt.length();i+=2) {
				String coords = bt.substring(i, i+2);
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TB\",\""+coords+"\")");
			}
			String wt = o.getString("white_territory").trim();
			for (int i=0;i<wt.length();i+=2) {
				String coords = wt.substring(i, i+2);
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TW\",\""+coords+"\")");
			}
			wt = o.getString("black_dead").trim();
			for (int i=0;i<wt.length();i+=2) {
				String coords = wt.substring(i, i+2);
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TW\",\""+coords+"\")");
			}

			GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].refresh()");
		} catch (JSONException e) {
			e.printStackTrace();
			showMessage("warning: error counting");
		}
		return sc;
	}

	/**
	 * we remove all territories and X in the goban but not in the sgf,
	 * because we'll need them to compute the "toggled dead stones" to estimate the score
	 * we then add back the X from the server onto the goban, so that the user knows which stones were
	 * marked dead and he can modify them. It should also make the toggle computing easier
	 */
	void cleanTerritory() {
		GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detsoncleanT()");
		Game g = Game.gameShown;
		String serverMarkedStones = g.deadstInSgf;
		System.out.println("clean territory: serverMarkedStones: "+serverMarkedStones);
		for (int i=0;i<serverMarkedStones.length();i+=2) {
			String coord = serverMarkedStones.substring(i,i+2);
			GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"MA\", \""+coord+"\")");
		}
		GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].refresh()");
	}

	void copyEidogo(final String edir, final File odir) {
		EventManager.getEventManager().sendEvent(eventType.copyEidogoStart);
		AssetManager mgr = getResources().getAssets();
		try {
			String[] fs = mgr.list(edir);
			for (String s : fs) {
				try {
					InputStream i = mgr.open(edir+"/"+s);
					// this is a file
					File f0 = new File(odir,s);
					FileOutputStream f = new FileOutputStream(f0);
					for (;;) {
						int d = i.read();
						if (d<0) break;
						f.write(d);
					}
					f.close();
					i.close();
				} catch (FileNotFoundException e) {
					// this is a directory and not a file
					File f = new File(odir,s);
					f.mkdirs();
					copyEidogo(edir+"/"+s,f);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			showMessage("DISK ERROR: "+e.toString());
		}
		System.out.println("endof copy");
		EventManager.getEventManager().sendEvent(eventType.copyEidogoEnd);
	}

	/*
	 * lifecycle:
	 * onCreate() ... onDestroy()
	 * visible:
	 * onStart() ... onStop() : can be called multiple times
	 * in front of all others:
	 * onResume() ... onPause() : interacting with user; called frequently
	 */

	@Override
	public void onDestroy() {
		super.onDestroy();
		quitall=true;
		if (server!=null) server.closeConnection();
		if (androidServer!=null) androidServer.closeConnection();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		loadSgf();
	}

	public void updateTraffic() {
		long newrx = TrafficStats.getTotalRxBytes();
		long newtx = TrafficStats.getTotalTxBytes();
		if (rx!=newrx||tx!=newtx) {
			rx=newrx; tx=newtx;
			writeTraffix(rx+tx);
		}
	}

	private void writeTraffix(final long nbytes) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final TextView label = (TextView)findViewById(R.id.textView1);
				if (label!=null) {
					CharSequence a = label.getText();
					if (a==null) a="";
					String s = a.toString();
					if (s.endsWith("kB")) {
						int i=s.lastIndexOf(' ');
						s=s.substring(0, i);
					}
					int kb = (int)((nbytes - rx0 - tx0) / (long)1024);
					label.setText(s+" "+kb+"kB");
					label.invalidate();
				}
			}
		});
	}

	public void writeInLabel(final String s) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final TextView label = (TextView)findViewById(R.id.textView1);
				label.setText(s);
				label.invalidate();
			}
		});
	}

	void initFinished() {
		System.out.println("init finished");
		writeInLabel("init done. You can play !");
		final Button button1 = (Button)findViewById(R.id.but1);
		button1.setClickable(true);
		button1.setEnabled(true);
		final Button button2 = (Button)findViewById(R.id.but2);
		button2.setClickable(true);
		button2.setEnabled(true);
		final Button button3 = (Button)findViewById(R.id.but3);
		button3.setClickable(true);
		button3.setEnabled(true);
		button1.invalidate();
		button2.invalidate();
		button3.invalidate();
	}

	private String getMarkedStones(String sgf) {
		String res = "";
		int i=0;
		for (;;) {
			System.out.println("debug "+sgf.substring(i));
			int j=sgf.indexOf("MA[",i);
			if (j<0) {
				return res;
			} else {
				i=j+2;
				while (i<sgf.length()) {
					if (sgf.charAt(i)!='[') break;
					j=sgf.indexOf(']',i);
					if (j<0) break;
					String stone = sgf.substring(i+1,j);
					res+=stone;
					i=j+1;
				}
			}
		}
	}

	private class myWebViewClient extends WebViewClient {
		@Override
		public void onPageFinished(final WebView view, String url) {
			System.out.println("page finished loading");
			if (curstate!=guistate.review)
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].last()");
			else if (!Reviews.isNotReviewStage) {
				Reviews.advance();
			}
			if (curstate==guistate.markDeadStones)
				GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detmarkx()");
			final EventManager em = EventManager.getEventManager();
			em.sendEvent(eventType.gobanReady);

			// ask for comments to display them in big
			System.out.println("page finished call detComments");
			GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detComments()");
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			System.out.println("mywebclient detecting command from javascript: "+url);
			int i=url.indexOf("androidcall01");
			if (i>=0) {
				// received the comment associated with the current move
				if (url.substring(i).startsWith("androidcall01C")) {
					System.out.println("comment command initreview "+Reviews.isNotReviewStage);
					if (!Reviews.isNotReviewStage) return true;
					Reviews.setComment(url.substring(i+14));
					if (Reviews.comment.length()>0) longToast(Reviews.comment, 5);
					return true;
				}
				if (url.substring(i).startsWith("androidcall01M")) {
					// this is triggered when the player gets out of review mode
					Integer mn = Integer.parseInt(url.substring(i+14));
					Reviews.curmove=mn;
					Reviews.saveCurReview();
					return true;
				}
				if (url.substring(i).startsWith("androidcall01S")) {
					// this is trigerred when the user clicks the SEND button or by the sgf downloader when detecting a SCORE2 phase
					// TODO: check that we can never have a 'Z' in a move definition
					int j = url.lastIndexOf('Z') + 1;
					String lastMove = url.substring(j);

					final Game g = Game.gameShown;
					if (curstate == guistate.markDeadStones) {
						// TODO: update the local SGF
						String sgfdata = url.substring(i + 14, j);
						String deadstones = getMarkedStones(sgfdata);
						final EventManager em = EventManager.getEventManager();
						EventManager.EventListener f = new EventManager.EventListener() {
							@Override
							public String getName() {
								return "mywebclient";
							}

							@Override
							public synchronized void reactToEvent() {
								em.unregisterListener(eventType.moveSentEnd, this);
								JSONObject o = server.o;
								if (o == null) {
									// error: do nothing
									return;
								}
								String err;
								try {
									err = o.getString("error");
									if (err != null && err.length() > 0) {
										// error: do nothing
										return;
									}
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								// show territories
								String sc = showCounting(o);
								showMessage("dead stones sent; score=" + sc);
								writeInLabel("score: " + sc);
								changeState(guistate.checkScore);
							}
						};
						em.registerListener(eventType.moveSentEnd, f);
						g.sendDeadstonesToServer(deadstones, server, true);
					} else {
						// we are in a "normal" (not scoring) state
						final EventManager em = EventManager.getEventManager();
						EventManager.EventListener f = new EventManager.EventListener() {
							@Override
							public String getName() {
								return "mywebclient";
							}

							@Override
							public synchronized void reactToEvent() {
								em.unregisterListener(eventType.moveSentEnd, this);
								// switch to next game if sendmove successful
								if (!g.finishedWithThisGame()) {
									main.showMessage("Problem sending move");
								} else {
									if (Game.getGames().size() == 0) {
										showMessage("No more games locally");
										changeState(guistate.nogame);
									} else
										downloadAndShowGame();
								}
							}
						};
						em.registerListener(eventType.moveSentEnd, f);
						g.sendMove2server(lastMove, server);
					}
					return true;
				}
				System.out.println("WARNING: unknown androidcall01 from javascript !");
				return true;
			} else {
				// its not an android call back 
				// let the browser navigate normally
				return false;
			}
		}   
	}  

	void showGame(final Game g) {
		if (g.getGameStatus().startsWith("SCORE")) {
			System.out.println("scoring phase detected in showgame");
			// TODO: I tried, but it's really difficult to handle correctly the scoring phase;
			// so for now, I just call the browser to resolve this stage !
			//            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(server.getUrl()+"game.php?gid="+g.getGameID()));
			//            startActivity(browserIntent);

			EventManager.getEventManager().registerListener(eventType.gobanReady, new EventManager.EventListener() {
				@Override
				public void reactToEvent() {
					EventManager.getEventManager().unregisterListener(eventType.gobanReady, this);
					GoJsActivity.viewUrl(server.getUrl()+"game.php?gid="+g.getGameID());
				}
				@Override
				public String getName() {
					return "SCORE_phase";
				}
			});
			changeState(guistate.nogame);
			GoJsActivity.viewUrl(server.getUrl()+"login.php?userid="+server.getLogin()+"&passwd="+server.getPwd());

			//            skipGame();
			return;
		}

		g.showGame();

		// show the board game
		String f=eidogodir+"/example.html";
		System.out.println("debugloadurl file://"+f);
		System.out.println("just before loading the URL: ");
		GoJsActivity.viewUrl("file://"+f);
	}

	void downloadAndShowGame() {
		int ngames = Game.getGames().size();
		if (curgidx2play>=ngames) {
			if (ngames==0) {
				showMessage("No game to show");
				return;
			} else {
				curgidx2play=0;
			}
		}
		final Game g = Game.getGames().get(curgidx2play);
		System.out.println("showing game "+curgidx2play+" "+g.getGameID());

		if (g.getGameID()>=0) {
			// DGS game
			final EventManager em = EventManager.getEventManager();
			em.registerListener(eventType.GameOK, new EventManager.EventListener() {
				@Override
				public String getName() {
					return "downloadAndShowGame";
				}

				@Override
				public synchronized void reactToEvent() {
					em.unregisterListener(eventType.GameOK, this);
					showGame(g);
				}
			});
			g.downloadGame(server);
		} else {
			// OGS game
			// OGSConnection.nextGame2play(g);
		}
	}

	// warning; this requires API 5 (> v2.0)
	@Override
	public void onBackPressed() {
		System.out.println("back pressed");
		if (curstate==guistate.nogame) {
			super.onBackPressed();
		} else if (curstate==guistate.forums) {
			if (Forums.back()) GUI.getGUI().showHome();
		} else
			GUI.getGUI().showHome();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		main = this;
		initGUI();
	}
	void initGUI() {
		// ====================================
		setContentView(R.layout.activity_main);

		wv = (WebView)findViewById(R.id.web1);

		wv.setWebViewClient(new myWebViewClient());
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setSupportZoom(true);

		//		String myHTML  = "<html><body><a href='androidcall01|123456'>Call Back to Android 01 with param 123456</a>my Content<p>my Content<p>my Content<p><a href='androidcall02|7890123'>Call Back to Android 01 with param 7890123</a></body></html>";
		//		wv.loadDataWithBaseURL("about:blank", myHTML, "text/html", "utf-8", "");
		//		wv.addJavascriptInterface(new WebAppInterface(this), "Android");

		{
			final Button button = (Button)findViewById(R.id.morebutts);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press last button on state "+curstate);
					showMoreButtons();
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.but1);
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					System.out.println("DGSAPP press button1 on state "+curstate);
					switch(curstate) {
						case nogame: // download games
							downloadListOfGames();
							break;
						case play: // send the move
							// ask eidogo to send last move; it will be captured by the web listener
							System.out.println("DEBUG SENDING MOVE TO SERVER");
							GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].detsonSend()");
							break;
						case markDeadStones: // send a request to the server to compute the score
							// ask eidogo to give sgf, which shall contain X
							// for now, I forbid scoring mode from the app
							break;
						case checkScore: // accept the current score evaluation
							acceptScore();
							break;
						case message: // get messages
							if (!initServer()) return;
							Message.downloadMessages(server,main);
							break;
						case review: // reprint comment
							longToast(Reviews.comment, 5);
							break;
					}

					//	              {
					//	                  Intent intent = new Intent(Intent.ACTION_VIEW);
					//	                  intent.addCategory(Intent.CATEGORY_BROWSABLE);
					//	                  intent.setDataAndType(Uri.fromFile(ff), "application/x-webarchive-xml");
					//	                  //                  intent.setDataAndType(Uri.fromFile(ff), "text/html");
					//	                  intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
					//	                  //                  intent.setClassName("Lnu.tommie.inbrowser", "com.android.browser.BrowserActivity");
					//	                  startActivity(intent);
					//	                  // to launch a browser:
					//	                  //                  Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///mnt/sdcard/"));
					//	              }
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.but2);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press button2 on state "+curstate);
					switch(curstate) {
						case nogame:
						case play:
						case markDeadStones:
						case checkScore:
						case review:
							wv.zoomIn();
							wv.invalidate();
							break;
						case message: // send invitation
							System.out.println("send invitation");
							if (!initServer()) return;
							Message.invite(server, main);
							break;
					}
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.but3);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press button3 on state "+curstate);
					switch(curstate) {
						case nogame:
						case play:
						case markDeadStones:
						case review:
						case checkScore:
							wv.zoomOut();
							wv.invalidate();
							break;
						case message: // send message
							if (!initServer()) {
								showMessage("Connection problem");
							} else
								Message.send(server,main);
							break;
					}
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.but4);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press button4 on state "+curstate);
					switch(curstate) {
						case nogame: // send message
							changeState(guistate.message); break;
						case play: // reset to the original download SGF
							showGame(Game.gameShown);
							break;
						case markDeadStones: // cancels marking stones and comes back to playing
							changeState(guistate.play);
							break;
						case checkScore: // refuse score and continues to mark stones
							refuseScore();
							break;
						case message: // go back to last game mode
							changeState(lastGameState);
							break;
						case review:
							Reviews.showList();
							break;
					}
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.but5);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press button5 on state "+curstate);
					CharSequence t = button.getText();
					if (t.equals("Fwd"))
						GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].fwd()");
					else if (t.equals("Bck"))
						GoJsActivity.viewUrl("javascript:eidogo.autoPlayers[0].backward()");
					else Log.w("button5", "unknow text "+t);
				}
			});
		}

		// ====================================
		// copy the eidogo dir into the external sdcard
		// only copy if it does not exist already
		// this takes time, so do it in a thread and show a message for the user to wait
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if (mExternalStorageAvailable&&mExternalStorageWriteable) {
			File d = getExternalCacheDir();
			eidogodir = new File(d, "eidogo");
			if (forcecopy||!eidogodir.exists()) {
				eidogodir.mkdirs();
				final Button button3 = (Button)findViewById(R.id.but3);
				button3.setClickable(false);
				button3.setEnabled(false);
				final Button button2= (Button)findViewById(R.id.but2);
				button2.setClickable(false);
				button2.setEnabled(false);
				final Button button1= (Button)findViewById(R.id.but1);
				button1.setClickable(false);
				button1.setEnabled(false);
				button1.invalidate();
				button2.invalidate();
				button3.invalidate();
				new CopyEidogoTask().execute("noparms");
			} else {
				showMessage("eidogo already on disk");
				initFinished();
			}
		} else {
			showMessage("R/W ERROR sdcard");
		}


		// manage events to show/hide the waiting dialog
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener waitDialogShower = new EventManager.EventListener() {
			@Override
			public String getName() {return "onStartShowWaitDialog";}
			@Override
			public synchronized void reactToEvent() {
				GUI.showWaitingWin();
				numEventsReceived++;
			}
		};
		// we put here all events that should trigger the "waiting" dialog
		em.registerListener(eventType.downloadGameStarted, waitDialogShower);
		em.registerListener(eventType.downloadListStarted, waitDialogShower);
		em.registerListener(eventType.loginStarted, waitDialogShower);
		em.registerListener(eventType.moveSentStart, waitDialogShower);
		em.registerListener(eventType.ladderStart, waitDialogShower);
		em.registerListener(eventType.ladderChallengeStart, waitDialogShower);
		em.registerListener(eventType.copyEidogoStart, waitDialogShower);

		EventManager.EventListener waitDialogHider = new EventManager.EventListener() {
			@Override
			public String getName() {return "onStartHideWaitDialog";}
			@Override
			public synchronized void reactToEvent() {
				try {
					--numEventsReceived;
					if (numEventsReceived<0) {
						System.out.println("ERROR events stream...");
						return;
					}
					if (numEventsReceived==0) {
						GUI.hideWaitingWin();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		em.registerListener(eventType.downloadGameEnd, waitDialogHider);
		em.registerListener(eventType.downloadListEnd, waitDialogHider);
		em.registerListener(eventType.loginEnd, waitDialogHider);
		em.registerListener(eventType.moveSentEnd, waitDialogHider);
		em.registerListener(eventType.ladderEnd, waitDialogHider);
		em.registerListener(eventType.ladderChallengeEnd, waitDialogHider);
		em.registerListener(eventType.copyEidogoEnd, waitDialogHider);

		// to show message
		em.registerListener(eventType.showMessage, new EventManager.EventListener() {
			@Override
			public void reactToEvent() {
				showMessage(EventManager.getEventManager().message);
			}
			@Override
			public String getName() {return "showMessage";}
		});

		// initialize guistate
		changeState(guistate.nogame);

		{
			// initialize active Go servers
			int valInConfig = PrefUtils.getFromPrefs(getApplicationContext(),PrefUtils.PREFS_DGSON,1);
			getGamesFromDGS = (valInConfig==1?true:false);
		}

		// initialize traffic stats
		ActivityManager mgr = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		// strange: the initial bandwidth of this uid is not 0 ?!
		uid = Process.myUid();
		rx=TrafficStats.getTotalRxBytes();
		tx=TrafficStats.getTotalTxBytes();
		if (rx0==-7) {
			rx0=rx; tx0=tx;
		}
		if (rx == TrafficStats.UNSUPPORTED || tx == TrafficStats.UNSUPPORTED) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Uh Oh!");
			alert.setMessage("Your device does not support traffic stat monitoring.");
			alert.show();
		} else {
			mHandler.postDelayed(mRunnable, 3000);
		}
		writeTraffix(rx+tx);

		if (false) {
			String f=eidogodir+"/example.html";
			GoJsActivity.viewUrl("file://"+f);
		}

	}

	private void acceptScore() {
		final Game g = Game.gameShown;
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener f = new EventManager.EventListener() {
			@Override
			public String getName() {return "acceptScore";}
			@Override
			public synchronized void reactToEvent() {
				em.unregisterListener(eventType.moveSentEnd, this);
				JSONObject o = server.o;
				if (o==null) {
					// error: don't switch game
					return;
				}
				String err;
				try {
					err = o.getString("error");
					if (err!=null&&err.length()>0) {
						// error: don't switch game
						return;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// switch to next game
				g.finishedWithThisGame();
				if (Game.getGames().size()==0) {
					showMessage("No more games locally");
					changeState(guistate.nogame);
				} else
					downloadAndShowGame();
			}
		};
		em.registerListener(eventType.moveSentEnd, f);
		g.acceptScore(server);
	}
	private void refuseScore() {
		cleanTerritory();
		changeState(guistate.markDeadStones);
	}

	public static class ErrDialogFragment extends DialogFragment {
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
			builder.setView(inflater.inflate(R.layout.error, null))
				// Add action buttons
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						EventManager.getEventManager().sendEvent(eventTobesent);
						// TODO: maybe the action reacting to the eventTobesent will change the state after the following state...
						Thread.yield();
						switch (main.curstate) {
							default: main.changeState(guistate.nogame);
						}
						main.changeState(guistate.nogame);
						ErrDialogFragment.this.getDialog().cancel();
					}
				})
			.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					main.server.sendCmdToServer(cmdSentBeforeNetErr, null, eventTobesent);
					ErrDialogFragment.this.getDialog().cancel();
				}
			});
			return builder.create();
		}
	}
	private ErrDialogFragment errdialog = null;

	boolean initServer() {
		System.out.println("call initserver "+server);
		if (server!=null) return true;
		String loginkey = PrefUtils.PREFS_LOGIN_USERNAME_KEY;
		String pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD_KEY;
		if (chosenLogin==1) {
			loginkey = PrefUtils.PREFS_LOGIN_USERNAME2_KEY;
			pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD2_KEY;
		}
		String u = PrefUtils.getFromPrefs(this, loginkey ,null);
		String p = PrefUtils.getFromPrefs(this, pwdkey ,null);
		System.out.println("credsdebug "+u+" "+p+" "+chosenLogin);
		if (u==null||p==null) {
			showMessage("Please enter your credentials first via menu Settings");
			return false;
		}
		{
			int i=PrefUtils.getFromPrefs(getApplicationContext(), PrefUtils.PREFS_BADNWIDTH_MODE, Game.ALWAYS_DOWNLOAD_SGF);
			Game.bandwidthMode=i;
			if (i==Game.PREFER_LOCAL_SGF)
				System.out.println("Set prefer local sgf from saved preferences");
		}

		final GoJsActivity m = this;
		System.out.println("credentials passed to server "+u+" "+p);
		if (server==null) {
			server = new ServerConnection(chosenServer, u, p);
			DetLogger l = new DetLogger() {
				@Override
				public void showMsg(String s) {
					if (s.startsWith("Net error|")) {
						int i=s.lastIndexOf('|');
						String cmdSentBeforeNetErr = s.substring(10, i);
						eventType eventTobesent = eventType.valueOf(s.substring(i+1));
						errdialog = new ErrDialogFragment();
						errdialog.setArguments(cmdSentBeforeNetErr, eventTobesent, m);
						errdialog.show(getSupportFragmentManager(),"Net_error");
					} else 
						showMessage(s);
				}
			};
			server.setLogger(l);
		}
		return true;
	}
	boolean initAndroidServer() {
		if (androidServer!=null) return true;
		String loginkey = PrefUtils.PREFS_LOGIN_USERNAME_KEY;
		String pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD_KEY;
		if (chosenLogin==1) {
			loginkey = PrefUtils.PREFS_LOGIN_USERNAME2_KEY;
			pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD2_KEY;
		}
		String u = PrefUtils.getFromPrefs(this, loginkey ,null);
		String p = PrefUtils.getFromPrefs(this, pwdkey ,null);
		System.out.println("credsdebug "+u+" "+p+" "+chosenLogin);
		if (u==null||p==null) {
			showMessage("Please enter your credentials first via menu Settings");
			return false;
		}
		System.out.println("credentials passed to server "+u+" "+p);
		if (androidServer==null) {
			androidServer = new AndroidServerConnection(chosenServer, u, p);
		}
		return true;
	}

	private void downloadListOfGames() {
		if (getGamesFromDGS&&!initServer()) return;
		final EventManager em = EventManager.getEventManager();
		// this listener will be called when the list of games will be downloaded
		EventManager.EventListener l = new EventManager.EventListener() {
			@Override
			public String getName() {return "downloadListOfGames";}
			@Override
			public void reactToEvent() {
				em.unregisterListener(eventType.downloadListGamesEnd, this);
				int ngames = Game.getGames().size();
				System.out.println("DGSAPP in downloadList listener "+ngames);
				if (ngames>=0)
					showMessage("Nb games to play: "+ngames);
				if (ngames>0) {
					curgidx2play=0;
					downloadAndShowGame();
					// download detects if 2 passes and auto change state to markdeadstones
					if (curstate!=guistate.markDeadStones) changeState(guistate.play);
				} else return;
			}
		};
		em.registerListener(eventType.downloadListGamesEnd, l);
		Game.loadStatusGames(server);
	}

	private class CopyEidogoTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... parms) {
			copyEidogo("eidogo",eidogodir);
			return "init done";
		}
		protected void onPostExecute(String res) {
			System.out.println("eidogo copy finished");
			initFinished();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				if (curstate==guistate.forums && Forums.inList>0) {
					// TODO: this keycode is weird; remove it
					Forums.switchShowNew();
				} else
					ask4credentials();
				return true;
			case R.id.bandwidth:
				ask4bandwidth();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	void showMessage(final String txt) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getBaseContext(), txt, Toast.LENGTH_LONG).show();
			}
		});
	}

	private void ask4bandwidth() {
		class LoginDialogFragment extends DialogFragment {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				LayoutInflater inflater = getActivity().getLayoutInflater();
				View v = inflater.inflate(R.layout.error, null);
				builder.setView(v)
					.setPositiveButton("Prefer Local SGF", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							Game.bandwidthMode = Game.PREFER_LOCAL_SGF;
							PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_BADNWIDTH_MODE, Game.PREFER_LOCAL_SGF);
							LoginDialogFragment.this.getDialog().cancel();
						}
					})
				.setNegativeButton("Always download", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Game.bandwidthMode = Game.ALWAYS_DOWNLOAD_SGF;
						PrefUtils.saveToPrefs(getApplicationContext(), PrefUtils.PREFS_BADNWIDTH_MODE, Game.ALWAYS_DOWNLOAD_SGF);
						LoginDialogFragment.this.getDialog().cancel();
					}
				});
				builder.setTitle("Bandwidth");
				TextView tv = (TextView)v.findViewById(R.id.errormsg);
				tv.setText("Choose bandwidth mode:");
				return builder.create();
			}
		}
		LoginDialogFragment dialog = new LoginDialogFragment();
		dialog.show(getSupportFragmentManager(),"bandwidth");
	}

	private void ask4credentials() {
		System.out.println("calling settings");
		final Context c = getApplicationContext();
		class LoginDialogFragment extends DialogFragment {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout
				builder.setView(inflater.inflate(R.layout.dialog_signin, null))
					// Add action buttons
					.setPositiveButton(R.string.signinDGS, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							// sign in the user ...
							TextView username = (TextView) LoginDialogFragment.this.getDialog().findViewById(R.id.username);
							TextView pwd = (TextView) LoginDialogFragment.this.getDialog().findViewById(R.id.password);
							String userkey = PrefUtils.PREFS_LOGIN_USERNAME_KEY;
							String pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD_KEY;
							if (chosenLogin == 1) {
								System.out.println("saving creds 1");
								userkey = PrefUtils.PREFS_LOGIN_USERNAME2_KEY;
								pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD2_KEY;
							} else
								System.out.println("saving creds 0");
							PrefUtils.saveToPrefs(c, userkey, username.getText().toString());
							PrefUtils.saveToPrefs(c, pwdkey, (String) pwd.getText().toString());
							showMessage("DGS Credentials saved");
						}
					})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						LoginDialogFragment.this.getDialog().cancel();
					}
				})
				.setNeutralButton(R.string.signinOGS, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						TextView username = (TextView) LoginDialogFragment.this.getDialog().findViewById(R.id.username);
						TextView pwd = (TextView) LoginDialogFragment.this.getDialog().findViewById(R.id.password);
						String userkey = PrefUtils.PREFS_LOGIN_OGS_USERNAME;
						String pwdkey = PrefUtils.PREFS_LOGIN_OGS_PASSWD;
						System.out.println("saving creds OGS");
						PrefUtils.saveToPrefs(c, userkey, username.getText().toString());
						PrefUtils.saveToPrefs(c, pwdkey, (String) pwd.getText().toString());
						System.out.println("OGS creds saved");
						showMessage("OGS Credentials saved");

						// immediately try to login
						// OGSConnection.login();
					}
				});

				return builder.create();
			}
		}
		LoginDialogFragment dialog = new LoginDialogFragment();
		dialog.show(getSupportFragmentManager(),"dgs signin");
	}

	// TODO: fix to play first all DGS games, and then all OGS games
	private void skipGame() {
		if (Game.gameShown != null) {
			// consider the same way both DGS and OGS games
			if (Game.getGames().size() <= 1) {
				// go to OGS games
				showMessage("No more games downloaded; retry GetGames ?");
				changeState(guistate.nogame);
				return;
			}
			if (++curgidx2play >= Game.getGames().size()) curgidx2play = 0;
			downloadAndShowGame();
		}
	}

	// TODO: move this method into Game !
	private void resignGame() {
		Game.gameShown.addResignToSGF();
		String cmd = "quick_do.php?obj=game&cmd=resign&gid="+Game.gameShown.getGameID()+"&move_id="+Game.gameShown.moveid;
		if (Game.gameShown.getMessage()!=null) {
			cmd+="&msg="+URLEncoder.encode(Game.gameShown.getMessage().toString());
			Game.gameShown.addMessageToSGF(Game.gameShown.getMessage().toString());
		}
		EventManager.getEventManager().registerListener(eventType.moveSentEnd, new EventManager.EventListener() {
			@Override
			public void reactToEvent() {
				EventManager.getEventManager().unregisterListener(eventType.moveSentEnd,this);
				// switch to next game
				Game.gameShown.finishedWithThisGame();
				if (Game.getGames().size()==0) {
					showMessage("No more games locally");
					changeState(guistate.nogame);
				} else
					downloadAndShowGame();
			}
			@Override
			public String getName() {return "resign";}
		});
		server.sendCmdToServer(cmd, eventType.moveSentStart, eventType.moveSentEnd);
	}

	/*


	   From kitkat onwards use evaluateJavascript method instead loadUrl to call the javascript functions like below

	   if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
	   webView.evaluateJavascript("enable();", null);
	   } else {
	   webView.loadUrl("javascript:enable();");
	   }





	   public void run(final String scriptSrc) { 
	   webView.post(new Runnable() {
	   @Override
	   public void run() { 
	   webView.loadUrl("javascript:" + scriptSrc); 
	   }
	   }); 
	   }

	   and if this doesn't work, do new Thread(new Runnaable(){..<like above>..}).start() 

*/

	private void loadSgf() {
		System.out.println("eidogodir: "+eidogodir);
		String f=eidogodir+"/example.html";
		GoJsActivity.viewUrl("file://"+f);
	}

	private void addGameMessage() {
		if (Game.gameShown==null) {
			showMessage("you have no game to attach a message to");
			return;
		}
		class GameMessageDialogFragment extends DialogFragment {
			GoJsActivity main;
			public void setArguments(GoJsActivity m) {
				main=m;
			}
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout
				final View v = inflater.inflate(R.layout.gamemsg, null);
				builder.setView(v)
					// Add action buttons
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							GameMessageDialogFragment.this.getDialog().cancel();
						}
					})
				.setPositiveButton("Add message", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						RadioButton r = (RadioButton)v.findViewById(R.id.introMsg);
						if (r.isChecked()) {
							CharSequence msg = r.getText();
							Game.gameShown.setMessage(msg);
						} else {
							r = (RadioButton)v.findViewById(R.id.endMsg);
							if (r.isChecked()) {
								CharSequence msg = r.getText();
								Game.gameShown.setMessage(msg);
							} else {
								r = (RadioButton)v.findViewById(R.id.otherMsg);
								if (r.isChecked()) {
									EditText tt = (EditText)v.findViewById(R.id.textOtherMsg);
									CharSequence msg = tt.getText();
									if (msg.toString().trim().length()==0)
										Game.gameShown.setMessage(null);
									else
										Game.gameShown.setMessage(msg);
								} else {
									System.out.println("gamemsg nothing selected !!");
								} 
							}
						}
						GameMessageDialogFragment.this.getDialog().cancel();
					}
				});
				if (Game.gameShown.getMessage()!=null) {
					RadioButton r = (RadioButton)v.findViewById(R.id.otherMsg);
					r.setSelected(true);
					TextView msg = (TextView)v.findViewById(R.id.textOtherMsg);
					msg.setText(Game.gameShown.getMessage());
				}
				return builder.create();
			}
		}
		GameMessageDialogFragment gameMsgDialog = new GameMessageDialogFragment();
		gameMsgDialog.setArguments(this);
		gameMsgDialog.show(getSupportFragmentManager(),"game messages");
	}

	private void viewLadder(final int ladid) {
		if (!initAndroidServer()) return;
		androidServer.ladd=new Ladder(ladid);
		EventManager.getEventManager().registerListener(eventType.ladderEnd, new EventManager.EventListener() {
			@Override
			public void reactToEvent() {
				EventManager.getEventManager().unregisterListener(eventType.ladderEnd,this);
				if (androidServer.ladd==null||androidServer.ladd.getCachedLadder()==null||androidServer.ladd.getCachedLadder().length==0) {
					showMessage("Problem: are your registered in the ladder ? can you still challenge ?");
					return;
				}
				class DetListDialogFragment extends DialogFragment {
					@Override
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						// Get the layout inflater
						LayoutInflater inflater = getActivity().getLayoutInflater();
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(main, R.layout.detlistitem, androidServer.ladd.getCachedLadder());
						// Inflate and set the layout for the dialog
						// Pass null as the parent view because it's going in the dialog layout
						View listFrameview = inflater.inflate(R.layout.ladder, null);
						{
							TextView ladderlab = (TextView)listFrameview.findViewById(R.id.ladderlab);
							String s = "on "+androidServer.ladd.getCacheTime()+" your rk: "+androidServer.ladd.userRank;
							ladderlab.setText(s);
						}
						final ListView ladder = (ListView)listFrameview.findViewById(R.id.ladderList);
						ladder.setAdapter(adapter);
						ladder.setOnItemClickListener(new OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
								androidServer.ladd.lastClicked = position;
							}
						});
						builder.setView(listFrameview);

						builder.setPositiveButton("Challenge", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								int i = androidServer.ladd.lastClicked;
								//                                showMessage("selected item "+i);
								if (i>=0) {
									ladderChallenge(i);
								}
								DetListDialogFragment.this.getDialog().dismiss();
							}
						})
						.setNeutralButton("Reload", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								androidServer.ladd.resetCache();
								androidServer.startLadderView(eidogodir);
								DetListDialogFragment.this.getDialog().dismiss();
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								DetListDialogFragment.this.getDialog().dismiss();
							}
						});
						return builder.create();
					}
				}
				final DetListDialogFragment msgdialog = new DetListDialogFragment();
				msgdialog.show(main.getSupportFragmentManager(),"message");
			}
			@Override
			public String getName() {return "ladder";}
		});
		androidServer.ladd.checkCache(eidogodir);
		androidServer.startLadderView(eidogodir);
	}

	private void ladderChallenge(int pos) {
		if (pos<0||pos>=androidServer.ladd.ridList.length) {
			showMessage("Problem with item at pos "+pos);
			return;
		}
		String rid = androidServer.ladd.ridList[pos];
		System.out.println("challenging "+rid);
		androidServer.ladderChallenge(rid,pos);
	}

	private void showMoreButtons() {
		System.out.println("showing more buttons");
		class MoreButtonsDialogFragment extends DialogFragment {
			private final MoreButtonsDialogFragment dialog = this;
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				// Get the layout inflater
				LayoutInflater inflater = getActivity().getLayoutInflater();

				// Inflate and set the layout for the dialog
				// Pass null as the parent view because its going in the dialog layout
				View v = inflater.inflate(R.layout.other_buttons, null);

				Button bdebug = (Button)v.findViewById(R.id.loadSgf);
				bdebug.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("loading sgf");
						loadSgf();
						changeState(guistate.play);
						Game.createDebugGame();
						dialog.dismiss();
					}
				});

				Button bgamemsg = (Button)v.findViewById(R.id.gamemsg);
				bgamemsg.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						addGameMessage();
						dialog.dismiss();
					}
				});

				Button bladder19 = (Button)v.findViewById(R.id.ladder19);
				bladder19.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						viewLadder(Ladder.LADDER19x19);
						dialog.dismiss();
					}
				});

				Button bladder9 = (Button)v.findViewById(R.id.ladder9);
				bladder9.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						viewLadder(Ladder.LADDER9x9);
						dialog.dismiss();
					}
				});

				RadioButton bserver1 = (RadioButton)v.findViewById(R.id.dgs);
				bserver1.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						chosenServer=0;
						if (server!=null) server.closeConnection();
						server=null;
						dialog.dismiss();
					}
				});
				RadioButton bserver2 = (RadioButton)v.findViewById(R.id.devdgs);
				if (chosenServer==1) bserver2.setChecked(true);
				bserver2.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						chosenServer=1;
						if (server!=null) server.closeConnection();
						server=null;
						dialog.dismiss();
					}
				});
				RadioButton blogin1 = (RadioButton)v.findViewById(R.id.log1);
				blogin1.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						chosenLogin=0;
						if (server!=null) server.closeConnection();
						server=null;
						dialog.dismiss();
					}
				});
				RadioButton blogin2 = (RadioButton)v.findViewById(R.id.log2);
				if (chosenLogin==1) blogin2.setChecked(true);
				blogin2.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						chosenLogin=1;
						if (server!=null) server.closeConnection();
						server=null;
						dialog.dismiss();
					}
				});

				Button breviews = (Button)v.findViewById(R.id.reviews);
				breviews.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("game reviews");
						Reviews.contReviews();
						dialog.dismiss();
					}
				});
				Button beidogo = (Button)v.findViewById(R.id.copyEidogo);
				beidogo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("copy eidogo");
						new CopyEidogoTask().execute("noparms");
						dialog.dismiss();
					}
				});
				Button bcycle = (Button)v.findViewById(R.id.cycleStates);
				bcycle.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						changeState(guistate.nogame);
						dialog.dismiss();
					}
				});
				Button bskip = (Button)v.findViewById(R.id.skipGame);
				bskip.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("skip game");
						skipGame();
						dialog.dismiss();
					}
				});
				Button bresign= (Button)v.findViewById(R.id.resign);
				bresign.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("resign game");
						resignGame();
						dialog.dismiss();
					}
				});
				Button bsettings = (Button)v.findViewById(R.id.baction_settings);
				bsettings .setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("DGSAPP setting");
						dialog.dismiss();
						GoJsActivity.main.ask4credentials();
					}
				});
	
				Button bforums= (Button)v.findViewById(R.id.forums);
				bforums.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("Forums");
						Forums.show();
						dialog.dismiss();
					}
				});
				Button bsaved= (Button)v.findViewById(R.id.savedGames);
				bsaved.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						System.out.println("List saved games");
						Game.showListSaved();
						dialog.dismiss();
					}
				});

				final CheckBox connectDGS = (CheckBox)v.findViewById(R.id.checkBoxDGS);
				{
					int prefval=PrefUtils.getFromPrefs(getApplicationContext(),PrefUtils.PREFS_DGSON,1);
					connectDGS.setChecked(prefval==1?true:false);
				}
				connectDGS.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View vv) {
						boolean curval = connectDGS.isChecked();
						getGamesFromDGS=curval;
						int curvali = curval?1:0;
						PrefUtils.saveToPrefs(getApplicationContext(),PrefUtils.PREFS_DGSON,curvali);
					}
				});

				builder.setView(v);
				return builder.create();
			}
		}
		MoreButtonsDialogFragment dialog = new MoreButtonsDialogFragment();
		dialog.show(getSupportFragmentManager(),"more actions");
	}

	int toastline = 0;
	public void longToast(String msg, int secs) {
		final String[] ss = msg.split("\n");
		if (ss.length>4) {
			int ntics = ss.length/4;
			if (ntics*4<ss.length) ++ntics;
			toastline=0;
			String s = ss[toastline];
			for (int i=toastline+1;i<toastline+4&&i<ss.length;i++)
				s+="\n"+ss[i];
			Toast.makeText(getBaseContext(), s, Toast.LENGTH_LONG).show();
			new CountDownTimer(1000*ntics, 1000) {
				public void onTick(long millisUntilFinished) {
					toastline+=4;
					if (toastline<ss.length) {
						String s = ss[toastline];
						for (int i = toastline + 1; i < toastline + 4 && i < ss.length; i++)
							s += "\n" + ss[i];
						Toast.makeText(getBaseContext(), s, Toast.LENGTH_LONG).show();
					}
				}
				public void onFinish() {}
			}.start();
		} else {
			Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
		}
	}
}
