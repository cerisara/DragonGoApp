package fr.xtof54.dragongoapp;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Environment;
import android.content.res.AssetManager;
import android.content.DialogInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.view.Menu;
import android.view.MenuItem;
import android.text.InputType;

public class DragonGoAct extends Activity
{
	enum guistate {nogame, play, message, review, forums};
	guistate curstate = guistate.nogame;
	WebView wv=null;
    DGSConnect dgs=null;
    ProgressDialog progressDoalog=null;
	public File eidogodir=null;
    ArrayList<Game> games = new ArrayList<Game>();
    int gameShown = 0;
    boolean forcecopy=false;
    String login, password;

    public boolean checkDGSconnect() {
        if (dgs==null) dgs = new DGSConnect();
        if (dgs==null) {
            showMessage("connection error "+dgs.error);
        } else {
            if (!dgs.connect()) {
                showMessage("connection error "+dgs.error);
            } else return true;
        }
        return false;
     }

    public void downloadGamesList() {
        if (checkDGSconnect()) {
            ArrayList<Game> gameheaders = dgs.downloadGamesList();
            for (int j=0;j<gameheaders.size();j++) {
                int i=0;
                for (i=0;i<games.size();i++) {
                    if (games.get(i).id == gameheaders.get(j).id) {
                        games.get(i).gotNewMove=true;
                        break;
                    }
                }
                if (i>=games.size()) {
                    games.add(gameheaders.get(j));
                }
            }
        }
    }

    public void sendMove(final String move) {
        if (gameShown>=0&&gameShown<games.size()) {
            if (games.get(gameShown).isdgs) {
                showConnectWindow("sending move to DGS...");
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!checkDGSconnect()) hideConnectWindow();
                        else {
                            if (dgs.sendmove(games.get(gameShown),move)) {
                                // success
                                if (gameShown==games.size()-1) {
                                    hideConnectWindow();
                                    showMessage("No more games locally");
                                    changeState(guistate.nogame);
                                } else {
                                    downloadGamesList();
                                    hideConnectWindow();
                                }
                            } else {
                                hideConnectWindow();
                                showMessage("failed to send move "+dgs.error);
                            }
                        }
                    }
                });
                t.start();
            } else showMessage("no way to send to DGS: game is local");
        } else showMessage("no way to send to DGS: no game exists");
    }
    
/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		initGUI();
        // always init with a local unsaved empty board
        games.add(new Game());
        initEidogo();
    }

	private class myWebViewClient extends WebViewClient {
		@Override
		public void onPageFinished(final WebView view, String url) {
		    System.out.println("page finished loading");
            view.loadUrl("javascript:eidogo.autoPlayers[0].last()");
			// ask for comments to display them in big
			System.out.println("page finished call detComments");
			view.loadUrl("javascript:eidogo.autoPlayers[0].detComments()");
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			System.out.println("mywebclient detecting command from javascript: "+url);
			int i=url.indexOf("androidcall01");
			if (i>=0) {
                if (url.substring(i).startsWith("androidcall01S")) {
                    // this is trigerred when the user clicks the SEND button
                    // TODO: check that we can never have a 'Z' in a move definition
                    int j = url.lastIndexOf('Z') + 1;
                    String lastMove = url.substring(j);
                    sendMove(lastMove);
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

    void showConnectWindow(String msg) {
        progressDoalog = new ProgressDialog(DragonGoAct.this);
        progressDoalog.setIndeterminate(true);
        progressDoalog.setMessage(msg);
        progressDoalog.setTitle("Connecting...");
    }
    void hideConnectWindow() {
        try {
            if (progressDoalog != null && progressDoalog.isShowing()) {
                 progressDoalog.dismiss();
            }
        } catch (Exception ex) {
            System.out.println("ERROR in hideConnectWindow "+ex.getMessage());
        }
    }

	public void writeInLabel(final String s) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
//				final TextView label = (TextView)findViewById(R.id.textView1);
//				label.setText(s);
//				label.invalidate();
			}
		});
	}

	void showMessage(final String txt) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getBaseContext(), txt, Toast.LENGTH_LONG).show();
			}
		});
	}

	void initGUI() {
        setContentView(R.layout.main);
		wv = (WebView)findViewById(R.id.web1);
		wv.setWebViewClient(new myWebViewClient());
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setSupportZoom(true);
    }

    void initEidogo() {
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
                showConnectWindow("preparing Eidogo onto SD-card...");
				new CopyEidogoTask().execute("noparms");
			} else {
				showMessage("eidogo already on disk");
			}
		} else {
			showMessage("R/W ERROR sdcard "+mExternalStorageAvailable+" "+mExternalStorageWriteable);
		}
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
                    System.out.println("copying eidogo "+odir+"/"+s);
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

    // TODO: because this method calls a WebView method (loadUrl()), all of these calls must be made
    // by the same thread (should it be the main android/app UI loop ?)
    // So, we should apply the same principle as for showMessage
	void changeState(guistate newstate) {
		System.out.println("inchangestate "+curstate+" .. "+newstate);
		switch (newstate) {
		case nogame:
			// we allow clicking just in case the user wants to play locally, disconnected
			writeInLabel("Getgame: download game from DGS");
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
			setButtons("Games","Zm+","Zm-","Msg"); break;
		case play:
			writeInLabel("click on the board to play");
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detallowClicking()");
			setButtons("Send","Zm+","Zm-","Reset","Bck"); break;
		case message:
			wv.loadUrl("javascript:eidogo.autoPlayers[0].detforbidClicking()");
			setButtons("GetMsg","Invite","SendMsg","Back2game"); break;
		default:
		}
		curstate=newstate;
	}

	private class CopyEidogoTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... parms) {
			copyEidogo("eidogo",eidogodir);
			return "init done";
		}
		protected void onPostExecute(String res) {
			System.out.println("eidogo copy finished");
            hideConnectWindow();
		}
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
		case R.id.bandwidth:
			cleanAllLocalData();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    static void recursiveDelete(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                recursiveDelete(f);
            }
        }
        file.delete();
        System.out.println("Deleted file/folder: "+file.getAbsolutePath());
    }

    void cleanAllLocalData() {
        // TODO: ask for confirmation
        if (eidogodir!=null && eidogodir.exists()) {
            showConnectWindow("deleting eidogo dir...");
            recursiveDelete(eidogodir);
            hideConnectWindow();
        }
    }

	private void ask4credentials() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter DGS login name");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                login = input.getText().toString();
                System.out.println("set user login "+login);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
