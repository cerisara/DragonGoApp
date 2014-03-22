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

import com.example.testbrowser.R;
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
	int nc=0;
	File eidogodir;
	final boolean forcecopy=false;
	Thread uithread;
	HttpClient httpclient=null;
	WebView wv;
	ArrayList<String> games2play = new ArrayList<String>();
	int curgidx2play=0,moveid=0;
	String gameid="";
	boolean isSelectingDeadStones = false;

//	private static void copyFile(InputStream in, OutputStream out) throws IOException {
//		byte[] buffer = new byte[1024];
//		int read;
//		while((read = in.read(buffer)) != -1){
//			out.write(buffer, 0, read);
//		}
//	}

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
			showMsg("DISK ERROR: "+e.toString());
		}
		System.out.println("endof copy");
	}

	void initFinished() {
		System.out.println("init finished");
		final TextView wv = (TextView)findViewById(R.id.textView1);
		wv.setText("init done. You can play !");
		final Button button1 = (Button)findViewById(R.id.button1);
		button1.setClickable(true);
		button1.setEnabled(true);
		final Button button2 = (Button)findViewById(R.id.button2);
		button2.setClickable(true);
		button2.setEnabled(true);
		final Button button3 = (Button)findViewById(R.id.button3);
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
					else res+=","+stone;
					i=j+1;
				}
			}
		}
	}
	
	private class myWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			int i=url.indexOf("androidcall01");
			if (i>=0) {
				int j=url.lastIndexOf('|')+1;
				String cmd="", move="??";
				if (isSelectingDeadStones) {
					String sgfdata = url.substring(i+14, j);
					String deadstones=getMarkedStones(sgfdata);
					System.out.println("deadstones "+deadstones);
					if (deadstones==null)
						cmd = "quick_do.php?obj=game&cmd=status_score&gid="+gameid;
					else
						cmd = "quick_do.php?obj=game&cmd=status_score&gid="+gameid+"&toggle=uniq&move="+deadstones;
				} else {
					move = url.substring(j);
					System.out.println("move "+move);
					cmd = "quick_do.php?obj=game&cmd=move&gid="+gameid+"&move_id="+moveid+"&move="+move;
					if (move.toLowerCase().startsWith("tt")) {
						// pass move
						cmd = "quick_do.php?obj=game&cmd=move&gid="+gameid+"&move_id="+moveid+"&move=pass";
					}
				}
				HttpGet httpget = new HttpGet("http://www.dragongoserver.net/"+cmd);
				try {
					HttpResponse response = httpclient.execute(httpget);
					// TODO: check if move has correctly been sent
					if (isSelectingDeadStones) {
						showMsg("dead stones sent !");
						isSelectingDeadStones=false;
					} else {
						showMsg("move "+move+" sent !");
					}
					moveid=0;
					games2play.remove(curgidx2play);
					showGame();
				} catch (Exception e) {
					e.printStackTrace();
					showMsg("net ERROR sending move");
				}
				return true;
			} else {
				// its not an android call back 
				// let the browser navigate normally
				return false;
			}
		}   
	}  

	void showGame() {
		if (curgidx2play>=games2play.size()) {
			if (games2play.size()==0) {
				showMsg("No game to show");
				return;
			} else {
				curgidx2play=0;
			}
		}
		System.out.println("showing game "+curgidx2play);
		System.out.println(" ... "+games2play.get(curgidx2play));
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

		// load the sgf and saves it in example.html
		String[] ss = games2play.get(curgidx2play).split(",");
		gameid = ss[1].replaceAll("\\D", "");
		HttpGet httpget = new HttpGet("http://www.dragongoserver.net/sgf.php?gid="+gameid+"&owned_comments=1&quick_mode=1");
		try {
			HttpResponse response = httpclient.execute(httpget);
			Header[] heds = response.getAllHeaders();
			for (Header s : heds)
				System.out.println("[HEADER] "+s);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				PrintWriter fout = new PrintWriter(new FileWriter(eidogodir+"/example.html"));
				for (String s : exampleFileHtmlHeader) fout.println(s);
				InputStream instream = entity.getContent();
				BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
				for (;;) {
					String s = fin.readLine();
					if (s==null) break;
					s=s.trim();
					if (s.length()>0&&s.charAt(0)!='[') {
						// look for move_id
						int i=s.indexOf("XM[");
						if (i>=0) {
							int j=s.indexOf(']',i+3);
							moveid = Integer.parseInt(s.substring(i+3, j));
						}
					}
					System.out.println("LOGINstatus "+s);
					fout.println(s);
				}
				fin.close();
				for (String s : htmlend) fout.println(s);
				fout.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// show the board game
		String f=eidogodir+"/example.html";
		wv.loadUrl("file://"+f);
}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		uithread = Thread.currentThread();
		
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
			final Button button = (Button)findViewById(R.id.button2);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					wv.zoomIn();
					wv.invalidate();
				}
			});
		}
		{
			final Button button = (Button)findViewById(R.id.button3);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					wv.zoomOut();
					wv.invalidate();
				}
			});
		}
		final Button button = (Button)findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				++nc;
                dowbloadListOfGames();

//				{
//					Intent intent = new Intent(Intent.ACTION_VIEW);
//					intent.addCategory(Intent.CATEGORY_BROWSABLE);
//					intent.setDataAndType(Uri.fromFile(ff), "application/x-webarchive-xml");
//					//	                intent.setDataAndType(Uri.fromFile(ff), "text/html");
//					intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
//					//	                intent.setClassName("Lnu.tommie.inbrowser", "com.android.browser.BrowserActivity");
//					startActivity(intent);
//					// to launch a browser:
//					//	                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("file:///mnt/sdcard/"));
//				}
			}
		});

		// ====================================
		// copy the eidogo dir into the external sdcard
		// TODO: only copy if it does not exist already
		// TODO: this takes time, so do it in a thread and show a message for the user to wait
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
				final Button button3 = (Button)findViewById(R.id.button3);
				button3.setClickable(false);
				button3.setEnabled(false);
				final Button button2= (Button)findViewById(R.id.button2);
				button2.setClickable(false);
				button2.setEnabled(false);
				final Button button1= (Button)findViewById(R.id.button1);
				button1.setClickable(false);
				button1.setEnabled(false);
				button1.invalidate();
				button2.invalidate();
				button3.invalidate();
				new CopyEidogoTask().execute("noparms");
			} else {
			    showMsg("eidogo already on disk");
				initFinished();
			}
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
	
	private void dowbloadListOfGames() {
	    final String u = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_USERNAME_KEY,null);
	    final String p = PrefUtils.getFromPrefs(this, PrefUtils.PREFS_LOGIN_PASSWORD_KEY,null);
	    if (u==null||p==null) {
	        showMsg("Please enter your credentials first via menu Settings");
	        return;
	    }
	    
		class WaitDialogFragment extends DialogFragment {
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
						// TODO: stop downloading
					}
				});
				return builder.create();
			}
		}
		final WaitDialogFragment waitdialog = new WaitDialogFragment();
		waitdialog.show(getSupportFragmentManager(),"waiting");

	    Thread downloadThread = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("CREATE DOWNLOAD THREAD "+Thread.currentThread().getId());
				if (httpclient==null) {
				    System.out.println("creating httpclient in getsgf");
				    httpclient = new DefaultHttpClient();
				}
				try {
				    String cmd = "http://www.dragongoserver.net/login.php?quick_mode=1&userid="+u+"&passwd="+p;
				    System.out.println("debug cmd "+cmd);
					HttpGet httpget = new HttpGet(cmd);
					HttpResponse response = httpclient.execute(httpget);
					Header[] heds = response.getAllHeaders();
					for (Header s : heds)
						System.out.println("[HEADER] "+s);
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						InputStream instream = entity.getContent();
						BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
						for (;;) {
							String s = fin.readLine();
							if (s==null) break;
							System.out.println("LOGINlog "+s);
							if (s.contains("#Error")) showMsg("Error login; check credentials");
						}
						fin.close();
					}

					// get list of games to play
					httpget = new HttpGet("http://www.dragongoserver.net/quick_status.php?order=0");
					response = httpclient.execute(httpget);
					heds = response.getAllHeaders();
					for (Header s : heds)
						System.out.println("[HEADER] "+s);
					entity = response.getEntity();
					games2play.clear();
					if (entity != null) {
						InputStream instream = entity.getContent();
						BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
						for (;;) {
							String s = fin.readLine();
							if (s==null) break;
							s=s.trim();
							if (s.length()>0&&s.charAt(0)!='[')
								games2play.add(s);
							System.out.println("LOGINstatus "+s);
						}
						fin.close();
					}
					System.out.println("NBGAMES "+games2play.size());
					showMsg("Nb games to play: "+games2play.size());

					if (games2play.size()>0) {
						curgidx2play=0;
						showGame();
					}
				} catch (Exception e) {
					e.printStackTrace();
					showMsg("Connection or network problems");
				}
				
				try {
					for (;;) {
						if (waitdialog!=null && waitdialog.getDialog()!=null) break;
						Thread.sleep(500);
					}
					waitdialog.getDialog().cancel();
				} catch (Exception e) {
					e.printStackTrace();
				}
				// TODO: where to close the connection ?
//				httpclient.getConnectionManager().shutdown();
			}
		});
	    downloadThread.start();
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
			ask4credentials();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	void showMsg(final String txt) {
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
                        showMsg("Credentials saved");
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
			showMsg("No more games downloaded; retry GetGames ?");
			return;
		}
		if (++curgidx2play>=games2play.size()) curgidx2play=0;
		showGame();
	}
	private void resignGame() {
		String cmd = "quick_do.php?obj=game&cmd=resign&gid="+gameid+"&move_id="+moveid;
		HttpGet httpget = new HttpGet("http://www.dragongoserver.net/"+cmd);
		try {
			HttpResponse response = httpclient.execute(httpget);
			// TODO: check if move has correctly been sent
			showMsg("resign sent !");
			moveid=0;
			games2play.remove(curgidx2play);
			showGame();
		} catch (Exception e) {
			e.printStackTrace();
			showMsg("net ERROR sending move");
		}

	}
	
	private void loadSgf() {
		String f=eidogodir+"/example.html";
		wv.loadUrl("file://"+f);
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
				{
					Button button = (Button)v.findViewById(R.id.deadStones);
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							showMsg("Put one X marker on each dead group and click SEND");
							isSelectingDeadStones=true;
							dialog.dismiss();
						}
					});
				}
				
				builder.setView(v);
				return builder.create();
			}
		}
		MoreButtonsDialogFragment dialog = new MoreButtonsDialogFragment();
		dialog.show(getSupportFragmentManager(),"more actions");
	}
}
