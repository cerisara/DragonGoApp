package fr.xtof54.jsgo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
import org.json.JSONException;
import org.json.JSONObject;

import com.example.testbrowser.R;

import fr.xtof54.jsgo.EventManager.eventType;
import fr.xtof54.jsgo.ServerConnection.DetLogger;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

/**
 * TODO:
 * - create a dir in externalStorage - done
 * - copy in there the eidogo dir - done
 * - load games from DGS - done
 * - create an example.html file with the sgf inline - done
 * - init goban zoom from physical screen size
 * 
 * @author xtof
 *
 */
public class GoJsActivity extends FragmentActivity {
	private ServerConnection server=null;
	
	static int debugdevel=-1;
//    String server;
    enum guistate {nogame, play, markDeadStones, checkScore};
    guistate curstate = guistate.nogame;
    
	File eidogodir;
	final boolean forcecopy=false;
	HttpClient httpclient=null;
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
	private void changeState(guistate newstate) {
		if (curstate==guistate.markDeadStones && newstate!=guistate.markDeadStones)
	    	wv.loadUrl("javascript:eidogo.autoPlayers[0].detmarkp()");
		System.out.println("inchangestate "+curstate+" .. "+newstate);
	    switch (newstate) {
	    case nogame: setButtons("Getgame","Zoom+","Zoom-","Msg"); break;
	    case play: setButtons("Send","Zoom+","Zoom-","Est.Score"); break;
	    case markDeadStones:
	    	// just in case the board is already rendered...
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detmarkx()");
	    	setButtons("Score","Zoom+","Zoom-","Play"); break;
	    case checkScore: setButtons("Accept","Zoom+","Zoom-","Refuse"); break;
	    default:
	    }
	    curstate=newstate;
	}
	
    public String showCounting(String json) {
        JSONObject o = new JSONObject(json);
        String sc="";
        try {
            sc = o.getString("score");
            String bt = o.getString("black_territory").trim();
            for (int i=0;i<bt.length();i+=2) {
                String coords = bt.substring(i, i+2);
                wv.loadUrl("javascript:eidogo.autoPlayers[0].cursor.node.pushProperty(\"TB\",\""+coords+"\")");
            }
            String wt = o.getString("white_territory").trim();
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

	void copyEidogo(final String edir, final File odir) {
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
	}

	void initFinished() {
		System.out.println("init finished");
		final TextView wv = (TextView)findViewById(R.id.textView1);
		wv.setText("init done. You can play !");
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
		wv.invalidate();
	}
	
	private String getMarkedStones(String sgf) {
		String res = null;
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
					if (res==null) res=stone;
					else res+=stone;
					i=j+1;
				}
			}
		}
	}
	
	private boolean sendCmd2server(String cmd, String msg) {
		HttpGet httpget = new HttpGet(server+cmd);
		try {
			HttpResponse response = httpclient.execute(httpget);
			System.out.println("answer to send move:");
			Header[] heds = response.getAllHeaders();
			for (Header s : heds)
				System.out.println("[HEADER] "+s);
			HttpEntity entity = response.getEntity();
			String jsonansw=null;
			if (entity != null) {
				InputStream instream = entity.getContent();
				BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
				for (;;) {
					String s = fin.readLine();
					if (s==null) break;
					System.out.println("LOGINlog "+s);
					if (s.contains("#Error")) showMessage("Error login; check credentials");
					if (s.length()>0&&s.charAt(0)=='{') jsonansw = s;
				}
				fin.close();
			}
			if (curstate==guistate.markDeadStones) {
			    String sc = showCounting(jsonansw);
				showMessage("dead stones sent; score="+sc);
				changeState(guistate.checkScore);
			} else {
				showMessage("sent to server: "+msg);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			showMessage(netErrMsg);
			return false;
		}
	}
	
	private class myWebViewClient extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			view.loadUrl("javascript:eidogo.autoPlayers[0].last()");
			if (curstate==guistate.markDeadStones)
				view.loadUrl("javascript:eidogo.autoPlayers[0].detmarkx()");
		}
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
		    int i=url.indexOf("androidcall01");
		    if (i>=0) {
		        int j=url.lastIndexOf('|')+1;
		        String sgf = url.substring(i+14, j);
		        String lastMove = url.substring(j);

		        // this is trigerred when the user clicks the SEND button
		        Game g = Game.gameShown;
		        String cmd, msg;
		        if (curstate==guistate.markDeadStones) {
		            String sgfdata = url.substring(i+14, j);
		            String deadstones=getMarkedStones(sgfdata);
		            System.out.println("deadstones "+deadstones);
		            if (deadstones==null) {
		                cmd = "quick_do.php?obj=game&cmd=status_score&gid="+g.getGameID();
		                msg = "no deadstone";
		            } else {
		                cmd = "quick_do.php?obj=game&cmd=status_score&gid="+g.getGameID()+"&toggle=uniq&move="+deadstones;
		                msg = "deadstones "+deadstones;
		            }
		        } else {
		            g.sendMove2server(lastMove,server);
		        }
//	            if (sendCmd2server(cmd, msg) && curstate==guistate.play) {
//	              // if the move has been correctly sent to the server and it's really a move, not a score request,
//	              // then we can switch to the next game
//	                moveid=0;
//	                games2play.remove(curgidx2play);
//	                if (games2play.size()==0) {
//	                  showMessage("No more games locally");
//	                  changeState(guistate.nogame);
//	                } else
//	                  downloadAndShowGame();
//	            }
				return true;
			} else {
				// its not an android call back 
				// let the browser navigate normally
				return false;
			}
		}   
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
            public synchronized void reactToEvent() {
                em.unregisterListener(eventType.GameOK, this);
                g.showGame();
                // detect if in scoring phase
                if (g.isTwoPasses()) {
                    System.out.println("scoring phase detected !");
                    showMessage("Scoring phase: put one X marker on each dead group and click SEND to check score (you can still change after)");
                    changeState(guistate.markDeadStones);
                }

                // show the board game
                String f=eidogodir+"/example.html";
                System.out.println("debugloadurl file://"+f);
                wv.loadUrl("file://"+f);
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
					showMoreButtons();
				}
			});
		}
		{
	        final Button button = (Button)findViewById(R.id.but1);
	        button.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	                switch(curstate) {
	                case nogame: // download games
	                    downloadListOfGames();
	                    break;
	                case play: // send the move
	            		// ask eidogo to send last move; it will be captured by the web listener
	                    wv.loadUrl("javascript:eidogo.autoPlayers[0].detsonSend()");
	                    break;
	                case markDeadStones: // send a request to the server to compute the score
	                	// ask eidogo to send sgf, which shall contain X
	                    wv.loadUrl("javascript:eidogo.autoPlayers[0].detsonSend()");
	                    break;
	                case checkScore: // accept the current score evaluation
//	                    acceptScore();
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
					wv.zoomIn();
					wv.invalidate();
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.but3);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					wv.zoomOut();
					wv.invalidate();
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.but4);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
                    switch(curstate) {
                    case nogame: // send message (TODO)
                    	Message.send(); break;
                    case play: // start evaluation of score
                    	// normally, detmarkx() is called right after the board is displayed,
                    	// but here, the board is displayed long ago, so we have to call it manually
        				wv.loadUrl("javascript:eidogo.autoPlayers[0].detmarkx()");
        				changeState(guistate.markDeadStones);
        				break;
                    case markDeadStones: // cancels marking stones and comes back to playing
                    	changeState(guistate.play);
                        break;
                    case checkScore: // refuse score and continues to mark stones
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
	}

	private void downloadSgf(HttpClient httpclient, String url) {
		try {
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			System.out.println(response.getStatusLine());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
				try {
					BufferedInputStream bis = new BufferedInputStream(instream);
					String filePath = eidogodir+"/example.html";
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
					int inByte;
					while ((inByte = bis.read()) != -1 ) {
						bos.write(inByte);
					}
					bis.close();
					bos.close();
				} catch (IOException ex) {
					throw ex;
				} catch (RuntimeException ex) {
					httpget.abort();
					throw ex;
				} finally {
					instream.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static class PrefUtils {
	    public static final String PREFS_LOGIN_USERNAME_KEY = "__USERNAME__" ;
	    public static final String PREFS_LOGIN_PASSWORD_KEY = "__PASSWORD__" ;
	    public static final String PREFS_LOGIN_USERNAME2_KEY = "__USERNAME2__" ;
	    public static final String PREFS_LOGIN_PASSWORD2_KEY = "__PASSWORD2__" ;

	    /**
	     * Called to save supplied value in shared preferences against given key.
	     * @param context Context of caller activity
	     * @param key Key of value to save against
	     * @param value Value to save
	     */
	    public static void saveToPrefs(Context context, String key, String value) {
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	        final SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(key,value);
	        editor.commit();
	    }

	    /**
	     * Called to retrieve required value from shared preferences, identified by given key.
	     * Default value will be returned of no value found or error occurred.
	     * @param context Context of caller activity
	     * @param key Key to find value against
	     * @param defaultValue Value to return if no data found against given key
	     * @return Return the value found against given key, default if not found or any error occurs
	     */
	    public static String getFromPrefs(Context context, String key, String defaultValue) {
	        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	        try {
	            return sharedPrefs.getString(key, defaultValue);
	        } catch (Exception e) {
	             e.printStackTrace();
	             return defaultValue;
	        }
	    }
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
	
	private void downloadListOfGames() {
	    String tu = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_USERNAME_KEY,null);
	    String tp = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY,null);
	    if (tu==null||tp==null) {
	        showMessage("Please enter your credentials first via menu Settings");
	        return;
	    }
		if (debugdevel==0) {
			debugdevel=1;
		} else if (debugdevel==1) {
			tu = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_USERNAME2_KEY,null);
			tp = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_PASSWORD2_KEY,null);
			debugdevel=0;
		}
		final String u = tu, p=tp;
	    
		System.out.println("credentials passed to server "+u+" "+p);
		if (server==null) {
			server = new ServerConnection(0, u, p);
			DetLogger l = new DetLogger() {
				@Override
				public void showMsg(String s) {
					showMessage(s);
				}
			};
			server.setLogget(l);
		}
		
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener l = new EventManager.EventListener() {
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
	
    @Override
	public void onStart() {
        super.onStart();
//	    server = getString(R.string.server1);
	    main = this;
	    
		final EventManager em = EventManager.getEventManager();
		EventManager.EventListener waitDialogShower = new EventManager.EventListener() {
			boolean isShown = false;
			@Override
			public synchronized void reactToEvent() {
				if (!isShown) {
					waitdialog = new WaitDialogFragment();
					waitdialog.show(getSupportFragmentManager(),"waiting");
					isShown=true;
				}
				numEventsReceived++;
			}
		};
		em.registerListener(eventType.downloadGameStarted, waitDialogShower);
		em.registerListener(eventType.downloadListStarted, waitDialogShower);
		em.registerListener(eventType.loginStarted, waitDialogShower);
		
		EventManager.EventListener waitDialogHider = new EventManager.EventListener() {
			@Override
			public synchronized void reactToEvent() {
				--numEventsReceived;
				if (numEventsReceived<0) {
					System.out.println("ERROR events stream...");
					return;
				}
				if (numEventsReceived==0) waitdialog.dismiss();
			}
		};
		em.registerListener(eventType.downloadGameEnd, waitDialogHider);
		em.registerListener(eventType.downloadListEnd, waitDialogHider);
		em.registerListener(eventType.loginEnd, waitDialogHider);
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
                        PrefUtils.saveToPrefs(c, PrefUtils.PREFS_LOGIN_USERNAME_KEY, username.getText().toString());
                        PrefUtils.saveToPrefs(c, PrefUtils.PREFS_LOGIN_PASSWORD_KEY, (String)pwd.getText().toString());
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
	private void ask4credentials2() {
		System.out.println("calling debug settings");
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
                        PrefUtils.saveToPrefs(c, PrefUtils.PREFS_LOGIN_USERNAME2_KEY, username.getText().toString());
                        PrefUtils.saveToPrefs(c, PrefUtils.PREFS_LOGIN_PASSWORD2_KEY, (String)pwd.getText().toString());
                        showMessage("Credentials2 saved");
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
		if (games2play.size()<=1) {
			showMessage("No more games downloaded; retry GetGames ?");
			return;
		}
		if (++curgidx2play>=games2play.size()) curgidx2play=0;
		downloadAndShowGame();
	}
	private void resignGame() {
		String cmd = "quick_do.php?obj=game&cmd=resign&gid="+Game.gameShown.getGameID()+"&move_id="+moveid;
		HttpGet httpget = new HttpGet(server+cmd);
		try {
			HttpResponse response = httpclient.execute(httpget);
			// TODO: check if move has correctly been sent
			showMessage("resign sent !");
			moveid=0;
			games2play.remove(curgidx2play);
			downloadAndShowGame();
		} catch (Exception e) {
			e.printStackTrace();
			showMessage(netErrMsg);
		}
	}
	
	private void loadSgf() {
		System.out.println("eidogodir: "+eidogodir);
		String f=eidogodir+"/example.html";
		wv.loadUrl("file://"+f);
	}
	
	private void showMoreButtons() {
		System.out.println("showing more buttons");
		final GoJsActivity c = this;
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
                        dialog.dismiss();
                    }
                });
                Button bserver2 = (Button)v.findViewById(R.id.devServer);
                bserver2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View vv) {
                    	debugdevel=0;
                    	showMessage("debug devel mode ON");
                        System.out.println("next connections to devel server");
//                        server = getString(R.string.server2);
                	    final String u = PrefUtils.getFromPrefs(c, PrefUtils.PREFS_LOGIN_USERNAME2_KEY,null);
                	    System.out.println("debugcred2 "+u);
                        dialog.dismiss();
                	    if (u==null) ask4credentials2();
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
						switch (curstate) {
					    case nogame: changeState(guistate.play); break;
					    case play: changeState(guistate.markDeadStones); break;
					    case markDeadStones: changeState(guistate.checkScore); break;
					    case checkScore: changeState(guistate.nogame); break;
					    default:
						}
						System.out.println("cycle state "+curstate);
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
}
