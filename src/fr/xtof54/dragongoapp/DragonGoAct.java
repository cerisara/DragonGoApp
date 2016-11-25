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
import android.content.DialogInterface.OnDismissListener;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.view.Menu;
import android.view.View;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Gravity;
import android.text.InputType;

public class DragonGoAct extends Activity
{
    public static DragonGoAct main;
	enum guistate {nogame, play, message, forums};
	guistate curstate = guistate.nogame;
	WebView wv=null;
    DGSConnect dgs=null;
	public File eidogodir=null;
    ArrayList<Game> games = new ArrayList<Game>();
    int gameShown = 0;
    boolean forcecopy=false;
    public String login, password;

    public boolean checkDGSconnect() {
        // WARNING: must be called only from within a Runnable passed to showConnectWindow() !!
		String loginkey = PrefUtils.PREFS_LOGIN_USERNAME_KEY;
		String pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD_KEY;
		login = PrefUtils.getFromPrefs(this, loginkey ,null);
		password = PrefUtils.getFromPrefs(this, pwdkey ,null);
		if (login==null||password==null) {
			showMessage("Please enter your credentials first via menu Settings");
			return false;
		}

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

    public void playNextGame() {
        // WARNING: must be called only from within a Runnable passed to showConnectWindow() !!
        gameShown++;
        if (gameShown>=games.size()) gameShown=0;
        // download SGF + show the game
        ArrayList<String> sgf = dgs.downloadSGF(games.get(gameShown).id);
        if (sgf==null) showMessage("download SGF error "+dgs.error);
        else {
            games.get(gameShown).setSGF(sgf);
            games.get(gameShown).save();
            showGame();
        }
    }

    public void downloadGamesList() {
        // WARNING: must be called only from within a Runnable passed to showConnectWindow() !!
        if (checkDGSconnect()) {
            ArrayList<Game> gameheaders = dgs.downloadGamesList();
            if (gameheaders==null) showMessage("ERROR getting messages from DGS");
            else {
                if (gameheaders.size()==0) showMessage("no new game from DGS");
                else {
                    games.clear();
                    games.addAll(gameheaders);
                    gameShown=-1;
                    playNextGame();
                }
            }
        }
    }

    public void sendMove(final String move) {
        if (gameShown>=0&&gameShown<games.size()) {
            if (games.get(gameShown).isdgs) {
                showConnectWindow("sending move to DGS...", new Runnable() {
                    @Override
                    public void run() {
                        if (checkDGSconnect()) {
                            if (dgs.sendmove(games.get(gameShown),move)) {
                                // success
                                if (gameShown==games.size()-1) {
                                    showMessage("No more games locally");
                                    changeState(guistate.nogame);
                                } else {
                                    downloadGamesList();
                                }
                            } else {
                                showMessage("failed to send move "+dgs.error);
                            }
                        }
                    }
                });
            } else showMessage("no way to send to DGS: game is local");
        } else showMessage("no way to send to DGS: no game exists");
    }
    
/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        main=this;
        // always init with a local unsaved empty board
        games.add(new Game());
        initEidogo();
		initGUI();
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

    void showConnectWindow(final String msg, final Runnable job) {
        class TT extends AsyncTask<String,Void,Boolean> {
            ProgressDialog progressDoalog = new ProgressDialog(DragonGoAct.this);
            @Override
            protected void onPreExecute() {
                progressDoalog.setIndeterminate(true);
                progressDoalog.setMessage(msg);
                progressDoalog.setTitle("Connecting...");
                progressDoalog.show();
                super.onPreExecute();
            }
            @Override
            protected Boolean doInBackground(final String... parms) {
                job.run();
                return true;
            }
            @Override
            protected void onPostExecute(final Boolean success) {
                progressDoalog.dismiss();
                super.onPostExecute(success);
            }
        }
        TT t = new TT();
        t.execute();
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

		{
			final Button button = (Button)findViewById(R.id.but6);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press button6 on state "+curstate);
                    showGame();
				}
			});
		}
        {
			final Button button = (Button)findViewById(R.id.but1);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press button1 on state "+curstate);
                    switch(curstate) {
                        case nogame:
                            downloadGamesList();
                            break;
                        case play: // send the move
                            // ask eidogo to send last move; it will be captured by the web listener
                            System.out.println("DEBUG SENDING MOVE TO SERVER");
                            wv.loadUrl("javascript:eidogo.autoPlayers[0].detsonSend()");
                            break;
                    }
				}
			});
		}

        {
			final Button button = (Button)findViewById(R.id.but2);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press button2 on state "+curstate);
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
					System.out.println("press button3 on state "+curstate);
                    wv.zoomOut();
                    wv.invalidate();
				}
			});
		}
        {
			final Button button = (Button)findViewById(R.id.morebutts);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					System.out.println("press morebutts on state "+curstate);
                    popupmenu();
/*
                    PopupMenu popup=new PopupMenu(DragonGoAct.this,button);
                    popup.getMenuInflater().inflate(R.menu.popup_menu,popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            String it=item.getTitle();
                            if (it.equals("Set login/password")) ask4credentials();
                            else if (it.equals("Del Eidogo")) cleanAllLocalData();
                            else if (it.equals("Set Eidogo")) initEidogo();
                            else System.out.println("ERROR last button menu unk "+it);
                            return true;
                        }
                    });
                    popup.show();
*/
				}
			});
		}


		wv = (WebView)findViewById(R.id.web1);
		wv.setWebViewClient(new myWebViewClient());
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setSupportZoom(true);
        showGame();
    }

    void initEidogo() {
        File d = getFilesDir();
        eidogodir = new File(d, "eidogo");
        if (forcecopy||!eidogodir.exists()) {
            eidogodir.mkdirs();
            showConnectWindow("preparing Eidogo onto SD-card...", new Runnable() {
                @Override
                public void run() {
                    copyEidogo("eidogo",eidogodir);
                    System.out.println("eidogo copy finished");
                }});
        } else {
            showMessage("eidogo already on disk");
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
			writeInLabel("[Games] to download game from DGS");
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
		case R.id.cleanEidogo:
			cleanAllLocalData();
			return true;
		case R.id.installEidogo:
			initEidogo();
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
            showConnectWindow("deleting eidogo dir...", new Runnable() {
                @Override
                public void run() {
                    recursiveDelete(eidogodir);
                }});
        }
    }

    private void ask4credentials() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter DGS login name");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
		final Context c = getApplicationContext();
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
            @Override
            public void onClick(DialogInterface dialog, int which) {
                login = input.getText().toString();
                System.out.println("set user login "+login);

                AlertDialog.Builder builder2 = new AlertDialog.Builder(DragonGoAct.this);
                builder2.setTitle("Enter DGS password");
                final EditText input2 = new EditText(DragonGoAct.this);
                input2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder2.setView(input2);
                builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        password = input2.getText().toString();
                        System.out.println("set password"+password);
                        String userkey = PrefUtils.PREFS_LOGIN_USERNAME_KEY;
                        String pwdkey = PrefUtils.PREFS_LOGIN_PASSWORD_KEY;
                        PrefUtils.saveToPrefs(c, userkey, login);
                        PrefUtils.saveToPrefs(c, pwdkey, password);
                        showMessage("DGS Credentials saved");
                    }
                });
                builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder2.show();
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

	void showGame() {
        games.get(gameShown).write2html();
		String f=eidogodir+"/example.html";
		System.out.println("debugloadurl file://"+f);
		System.out.println("just before loading the url: ");
		wv.loadUrl("file://"+f);

        wv.invalidate();
    }

    private void popupmenu() {
        LayoutInflater inflater = (LayoutInflater)DragonGoAct.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Display display = getWindowManager().getDefaultDisplay();

        int width = display.getWidth()/2;
        int height = display.getHeight()/2;

        View pop = inflater.inflate(R.layout.popup_menu,null,false);
        pop.measure(View.MeasureSpec.UNSPECIFIED,View.MeasureSpec.UNSPECIFIED);
        height = pop.getMeasuredHeight();
        width = pop.getMeasuredWidth()+200;
        final PopupWindow pu = new PopupWindow(pop,width,height,true);
        pu.showAtLocation(findViewById(R.id.but1),Gravity.CENTER,1,1);

        Button bcancel = (Button)pu.getContentView().findViewById(R.id.cancelm);
        Button bcreds = (Button)pu.getContentView().findViewById(R.id.credsm);
        Button bclear = (Button)pu.getContentView().findViewById(R.id.clearm);
        Button beidogo= (Button)pu.getContentView().findViewById(R.id.installm);

        bcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pu.dismiss();
            }
        });
        bcreds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pu.dismiss();
                ask4credentials();
            }
        });
        bclear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pu.dismiss();
                cleanAllLocalData();
            }
        });
        beidogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pu.dismiss();
                initEidogo();
            }
        });
    }

}

