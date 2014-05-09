package fr.xtof54.jsgo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import fr.xtof54.dragonGoApp.R;
import fr.xtof54.jsgo.EventManager.eventType;
import fr.xtof54.jsgo.ServerConnection.DetLogger;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
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
	ServerConnection server=null;
	AndroidServerConnection androidServer = null;
	int chosenServer=0, chosenLogin=0;

	//    String server;
	enum guistate {nogame, play, markDeadStones, checkScore, message, review};
	guistate curstate = guistate.nogame;

	File eidogodir;
	final boolean forcecopy=false;
	WebView wv;
	ArrayList<Game> games2play = new ArrayList<Game>();
	int curgidx2play=0,moveid=0;
	final String netErrMsg = "Connection errors or timeout, you may retry";
	static GoJsActivity main;

	//	private static void copyFile(InputStream in, OutputStream out) throws IOException {
	//		byte[] buffer = new byte[1024];
	//		int read;
	//		while((read = in.read(buffer)) != -1){
	//			out.write(buffer, 0, read);
	//		}
	//	}

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
	void changeState(guistate newstate) {
		if (curstate==guistate.review && newstate!=guistate.review)
			GoJsActivity.main.wv.loadUrl("javascript:eidogo.autoPlayers[0].detMoveNumber()");
		if (curstate==guistate.markDeadStones && newstate!=guistate.markDeadStones)
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detmarkp()");
		System.out.println("inchangestate "+curstate+" .. "+newstate);
		switch (newstate) {
		case nogame:
			// we allow clicking just in case the user wants to play locally, disconnected
			writeInLabel("Getgame: download game from DGS");
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
			setButtons("Getgame","Zoom+","Zoom-","Msg"); break;
		case play:
			writeInLabel("click on the board to play");
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
			setButtons("Send","Zoom+","Zoom-","Reset"); break;
		case markDeadStones:
			writeInLabel("click on the board to mark dead stones");
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
			showMessage("Scoring phase: put one X marker on each dead group and click SCORE to check score (you can still change after)");
			// just in case the board is already rendered...
			// normally, detmarkx() is called right after the board is displayed,
			// but here, the board is displayed long ago, so we have to call it manually
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detmarkx()");
			setButtons("Score","Zoom+","Zoom-","Play"); break;
		case checkScore: 
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detforbidClicking()");
			setButtons("Accept","Zoom+","Zoom-","Refuse"); break;
		case message:
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detforbidClicking()");
			lastGameState=curstate;
			setButtons("GetMsg","Invite","SendMsg","Back2game"); break;
		case review:
			setButtons("ReprintMsg","Zoom+","Zoom-","ListG");break;
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
				wv.loadUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TB\",\""+coords+"\")");
			}
			bt = o.getString("white_dead").trim();
			for (int i=0;i<bt.length();i+=2) {
				String coords = bt.substring(i, i+2);
				wv.loadUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TB\",\""+coords+"\")");
			}
			String wt = o.getString("white_territory").trim();
			for (int i=0;i<wt.length();i+=2) {
				String coords = wt.substring(i, i+2);
				wv.loadUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TW\",\""+coords+"\")");
			}
			wt = o.getString("black_dead").trim();
			for (int i=0;i<wt.length();i+=2) {
				String coords = wt.substring(i, i+2);
				wv.loadUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TW\",\""+coords+"\")");
			}

			wv.loadUrl("javascript:eidogo.autoPlayers[0].refresh()");
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
		wv.loadUrl("javascript:eidogo.autoPlayers[0].detsoncleanT()");
		Game g = Game.gameShown;
		String serverMarkedStones = g.deadstInSgf;
		System.out.println("clean territory: serverMarkedStones: "+serverMarkedStones);
		for (int i=0;i<serverMarkedStones.length();i+=2) {
			String coord = serverMarkedStones.substring(i,i+2);
			wv.loadUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"MA\", \""+coord+"\")");
		}
		wv.loadUrl("javascript:eidogo.autoPlayers[0].refresh()");
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
				view.loadUrl("javascript:eidogo.autoPlayers[0].last()");
			else if (!Reviews.isNotReviewStage) {
			    Reviews.advance();
			}
			if (curstate==guistate.markDeadStones)
				view.loadUrl("javascript:eidogo.autoPlayers[0].detmarkx()");
			final EventManager em = EventManager.getEventManager();
			em.sendEvent(eventType.gobanReady);
			
			// ask for comments to display them in big
			System.out.println("page finished call detComments");
			view.loadUrl("javascript:eidogo.autoPlayers[0].detComments()");
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			System.out.println("mywebclient detecting command from javascript: "+url);
			int i=url.indexOf("androidcall01");
			if (i>=0) {
				if (url.substring(i).startsWith("androidcall01|C|")) {
				    System.out.println("comment command initreview "+Reviews.isNotReviewStage);
					if (!Reviews.isNotReviewStage) return true;
					Reviews.setComment(url.substring(i+16));
					if (Reviews.comment.length()>0) longToast(Reviews.comment, 5);
					return true;
				}
				if (url.substring(i).startsWith("androidcall01|M|")) {
					// this is triggered when the player gets out of review mode
					Integer mn = Integer.parseInt(url.substring(i+16));
					Reviews.curmove=mn;
					Reviews.saveCurReview();
					return true;
				}
				int j=url.lastIndexOf('|')+1;
				String lastMove = url.substring(j);

				// this is trigerred when the user clicks the SEND button or by the sgf downloader when detecting a SCORE2 phase
				final Game g = Game.gameShown;
				if (curstate==guistate.markDeadStones) {
					String sgfdata = url.substring(i+14, j);
					String deadstones=getMarkedStones(sgfdata);
					final EventManager em = EventManager.getEventManager();
					EventManager.EventListener f = new EventManager.EventListener() {
						@Override
						public String getName() {return "mywebclient";}
						@Override
						public synchronized void reactToEvent() {
							em.unregisterListener(eventType.moveSentEnd, this);
							JSONObject o = server.o;
							if (o==null) {
							    // error: do nothing
							    return;
							}
							String err;
							try {
							    err = o.getString("error");
							    if (err!=null&&err.length()>0) {
							        // error: do nothing
							        return;
							    }
							} catch (JSONException e) {
							    // TODO Auto-generated catch block
							    e.printStackTrace();
							}
							// show territories
							String sc = showCounting(o);
							showMessage("dead stones sent; score="+sc);
							writeInLabel("score: "+sc);
							changeState(guistate.checkScore);
						}
					};
					em.registerListener(eventType.moveSentEnd, f);
					g.sendDeadstonesToServer(deadstones, server, true);
				} else {
					final EventManager em = EventManager.getEventManager();
					EventManager.EventListener f = new EventManager.EventListener() {
						@Override
						public String getName() {return "mywebclient";}
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
					g.sendMove2server(lastMove,server);
				}
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
					wv.loadUrl(server.getUrl()+"game.php?gid="+g.getGameID());
				}
				@Override
				public String getName() {
					return "SCORE_phase";
				}
			});
			changeState(guistate.nogame);
			wv.loadUrl(server.getUrl()+"login.php?userid="+server.getLogin()+"&passwd="+server.getPwd());

			//            skipGame();
			return;
		}

		g.showGame();
		// detect if the other player has already agreed on dead stones
		//        if (g.getGameStatus().equals("SCORE2")) {
		//            System.out.println("Check score phase detected !");
		//            final EventManager em = EventManager.getEventManager();
		//            // this listener is trigerred when the goban has finished displayed
		//            final EventManager.EventListener drawTerritory = new EventManager.EventListener() {
		//                @Override
		//                public void reactToEvent() {
		//                    em.unregisterListener(eventType.gobanReady, this);
		//                    try {
		//						Thread.sleep(4000);
		//					} catch (InterruptedException e) {
		//						e.printStackTrace();
		//					}
		//                    JSONObject o = server.o;
		//                    if (o==null) {
		//                        // error: do nothing
		//                        return;
		//                    }
		//                    String err = o.getString("error");
		//                    if (err!=null&&err.length()>0) {
		//                        // error: do nothing
		//                        return;
		//                    }
		//                    // show territories
		//                    String sc = showCounting(o);
		//                    showMessage("dead stones sent; score="+sc);
		//                    writeInLabel("score: "+sc);
		//                    changeState(guistate.checkScore);
		//                }
		//                @Override
		//                public String getName() {return "drawTerritory";}
		//            };
		//            em.registerListener(eventType.moveSentEnd, new EventManager.EventListener() {
		//            	// this listener is triggered when the server sends back the score and dead stones
		//                @Override
		//                public void reactToEvent() {
		//                    em.unregisterListener(eventType.moveSentEnd, this);
		//                    curstate=guistate.markDeadStones;
		//                    // show the board game
		//                    String f=eidogodir+"/example.html";
		//                    System.out.println("debugloadurl file://"+f);
		//                    em.registerListener(eventType.gobanReady, drawTerritory);
		//                    // we then ask the goban to show the game in lark deadstone mode, but the second listener
		//                    // will be immediately trigerred once the goban is shown
		//                    wv.loadUrl("file://"+f);
		//                }
		//                @Override
		//                public String getName() {return "showgamedeadstones";}
		//            });
		//            // send NO stones to server to get the final score
		//            g.sendDeadstonesToServer("", server, false);
		//        } else {
		//            // detect if in scoring phase
		//            // TODO: replace this by checking the game STATUS !
		//            if (g.isTwoPasses()) {
		//                System.out.println("scoring phase detected !");
		//                changeState(guistate.markDeadStones);
		//            }
		//        }

		// show the board game
		String f=eidogodir+"/example.html";
		System.out.println("debugloadurl file://"+f);
		System.out.println("just before loading the URL: ");
		wv.loadUrl("file://"+f);
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
		System.out.println("showing game "+curgidx2play);

		final Game g = Game.getGames().get(curgidx2play);
		g.downloadGame(server);
		final EventManager em = EventManager.getEventManager();
		em.registerListener(eventType.GameOK, new EventManager.EventListener() {
			@Override
			public String getName() {return "downloadAndShowGame";}
			@Override
			public synchronized void reactToEvent() {
				em.unregisterListener(eventType.GameOK, this);
				showGame(g);
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
					System.out.println("press button1 on state "+curstate);
					switch(curstate) {
					case nogame: // download games
						downloadListOfGames();
						break;
					case play: // send the move
						// ask eidogo to send last move; it will be captured by the web listener
						wv.loadUrl("javascript:eidogo.autoPlayers[0].detsonSend()");
						break;
					case markDeadStones: // send a request to the server to compute the score
						// ask eidogo to give sgf, which shall contain X
						wv.loadUrl("javascript:eidogo.autoPlayers[0].detsonSend()");
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
					case nogame: // send message (TODO)
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
		main = this;

		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener waitDialogShower = new EventManager.EventListener() {
			@Override
			public String getName() {return "onStartShowWaitDialog";}
			@Override
			public synchronized void reactToEvent() {
				try {
					if (!isWaitingDialogShown) {
						waitdialog = new WaitDialogFragment();
						waitdialog.show(getSupportFragmentManager(),"waiting");
						isWaitingDialogShown=true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
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
						waitdialog.dismiss();
						isWaitingDialogShown=false;
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

	/*
	 * This method must not be blocking, otherwise the waiting dialog window is never displayed.
	 */
	public Thread runInWaitingThread(final Runnable method) {
		final Thread computingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// this is blocking
				method.run();
				try {
					// just in case, wait for the waiting dialog to be visible
					for (;;) {
						if (waitdialog!=null && waitdialog.getDialog()!=null) break;
						Thread.sleep(500);
					}
					// then dismisses it
					waitdialog.getDialog().cancel();
				} catch (Exception e) {
					e.printStackTrace();
				}
				// I notify potential threads that wait for computing to be finished
				synchronized (waiterComputingFinished) {
					waiterComputingFinished.notifyAll();
				}
			}
		});
		computingThread.start();
		return computingThread;
	}
	private Boolean waiterComputingFinished=true;

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

	private boolean initServer() {
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
	private boolean initAndroidServer() {
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
		if (!initServer()) return;
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener l = new EventManager.EventListener() {
			@Override
			public String getName() {return "downloadListOfGames";}
			@Override
			public void reactToEvent() {
				em.unregisterListener(eventType.downloadListGamesEnd, this);
				int ngames = Game.getGames().size();
				System.out.println("in downloadList listener "+ngames);
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

	public static class WaitDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			// Get the layout inflater
			LayoutInflater inflater = getActivity().getLayoutInflater();

			// Inflate and set the layout for the dialog
			// Pass null as the parent view because its going in the dialog layout
			builder.setView(inflater.inflate(R.layout.waiting, null))
			// Add action buttons
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					WaitDialogFragment.this.getDialog().cancel();
					// TODO stop thread
				}
			});
			return builder.create();
		}
	}

	private WaitDialogFragment waitdialog;
	private int numEventsReceived = 0;
	boolean isWaitingDialogShown = false;

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
			ask4credentials();
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

	private void ask4credentials() {
		System.out.println("calling settings");
		final Context c = this;
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
				.setPositiveButton(R.string.signin, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						// sign in the user ...
						TextView username = (TextView)LoginDialogFragment.this.getDialog().findViewById(R.id.username);
						TextView pwd = (TextView)LoginDialogFragment.this.getDialog().findViewById(R.id.password);
						String userkey = PrefUtils.PREFS_LOGIN_USERNAME_KEY;
						String pwdkey  = PrefUtils.PREFS_LOGIN_PASSWORD_KEY;
						if (chosenLogin==1) {
							System.out.println("saving creds 1");
							userkey = PrefUtils.PREFS_LOGIN_USERNAME2_KEY;
							pwdkey  = PrefUtils.PREFS_LOGIN_PASSWORD2_KEY;
						} else
							System.out.println("saving creds 0");
						PrefUtils.saveToPrefs(c, userkey, username.getText().toString());
						PrefUtils.saveToPrefs(c, pwdkey, (String)pwd.getText().toString());
						showMessage("Credentials saved");
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						LoginDialogFragment.this.getDialog().cancel();
					}
				});
				return builder.create();
			}
		}
		LoginDialogFragment dialog = new LoginDialogFragment();
		dialog.show(getSupportFragmentManager(),"dgs signin");
	}

	private void skipGame() {
		if (Game.getGames().size()<=1) {
			showMessage("No more games downloaded; retry GetGames ?");
			changeState(guistate.nogame);
			return;
		}
		if (++curgidx2play>=Game.getGames().size()) curgidx2play=0;
		downloadAndShowGame();
	}
	// TODO: move this method into Game !
	private void resignGame() {
		String cmd = "quick_do.php?obj=game&cmd=resign&gid="+Game.gameShown.getGameID()+"&move_id="+Game.gameShown.moveid;
		if (Game.gameShown.getMessage()!=null)
		    cmd+="&msg="+URLEncoder.encode(Game.gameShown.getMessage().toString());
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

	private void loadSgf() {
		System.out.println("eidogodir: "+eidogodir);
		String f=eidogodir+"/example.html";
		wv.loadUrl("file://"+f);
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
					showMessage("Probleme: are your registered in the ladder ? can you still challenge ?");
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
	                String s = ss[toastline];
	                for (int i=toastline+1;i<toastline+4&&i<ss.length;i++)
	                    s+="\n"+ss[i];
	                Toast.makeText(getBaseContext(), s, Toast.LENGTH_LONG).show();
	            }
	            public void onFinish() {}
	        }.start();
	    } else {
	        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
	    }
	}
}
