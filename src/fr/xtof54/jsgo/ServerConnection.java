package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

/**
 * This class is designed to be as much independent on the other
 * libraries than possible. In particular, it does not depend on the
 * android library, and can thus be used in desktop-oriented application.
 * This is also why it defines 2 interfaces.
 * 
 * Its role is to manage all communication during the game with one Dragon go net server.
 * Note that you can create several objects of this class to support several servers.
 * 
 * @author xtof
 *
 */
public class ServerConnection {
	final static String cmdGetListOfGames = "quick_do.php?obj=game&cmd=list&view=status";
	
	final String netErrMsg = "Connection errors or timeout, you may retry";
	
	private String u,p,server;
	private HttpClient httpclient=null;
	
	/**
	 * We define this interface so that all logging info can be simply displayed on the console,
	 * or shown as Android Toasts !
	 * Note that this interface really aims at showing routine messages to the user, and is thus quite different
	 * from the traditional logging facilities that rather aims at errors, warnings...
	 * @author xtof
	 *
	 */
	public interface DetLogger {
		public void showMsg(String s);
	}
	private DetLogger logger = new DetLogger() {
		@Override
		public void showMsg(String s) {
			System.out.println(s);
		}
	};
	public void setLogget(DetLogger l) {logger=l;}

	/**
	 * We define this interface so that it can be deployed on an Android unlimited progress dialog,
	 * as well as on a simple independent thread.
	 * 
	 * Warning: the object that implements this interface must be synchronized and wait for the runnable
	 * to have finished its job !
	 * 
	 * @author xtof
	 *
	 */
	public interface DetThreadRunner {
		public void runInThread(Runnable r);
	}
	DetThreadRunner threadRunner = new DetThreadRunner() {
		@Override
		public synchronized void runInThread(final Runnable r) {
			// wrap by another runnable to wait for its end
			Runnable rr = new Runnable() {
				@Override
				public void run() {
					System.out.println("start run");
					r.run();
					System.out.println("run done");
					synchronized (threadRunner) {
						System.out.println("notify");
						threadRunner.notify();
					}
				}
			};
			Thread t = new Thread(rr);
			t.start();
			try {
				threadRunner.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	final String[] serverNames = {
			"http://www.dragongoserver.net/",
			"http://dragongoserver.sourceforge.net/"
	};
	
	public void closeConnection() {
		if (httpclient!=null)
			httpclient.getConnectionManager().shutdown();
		httpclient=null;
	}
	
	/**
	 * creates a connection to a specific server;
	 * determines the correct credentials
	 * @param num
	 */
	public ServerConnection(int num, String userlogin, String userpwd) {
		server=serverNames[num];
// TODO: this must be handled outside of this class
//		String tu = PrefUtils.getFromPrefs(GoJsActivity.main, PrefUtils.PREFS_LOGIN_USERNAME_KEY,null);
//	    String tp = PrefUtils.getFromPrefs(GoJsActivity.main, PrefUtils.PREFS_LOGIN_PASSWORD_KEY,null);
//	    if (tu==null||tp==null) {
//	    	logger.showMsg("Please enter your credentials first via menu Settings");
//	        return;
//	    }
//		if (GoJsActivity.main.debugdevel==0) {
//			GoJsActivity.main.debugdevel=1;
//		} else if (GoJsActivity.main.debugdevel==1) {
//			tu = PrefUtils.getFromPrefs(GoJsActivity.main, PrefUtils.PREFS_LOGIN_USERNAME2_KEY,null);
//			tp = PrefUtils.getFromPrefs(GoJsActivity.main, PrefUtils.PREFS_LOGIN_PASSWORD2_KEY,null);
//			GoJsActivity.main.debugdevel=0;
//		}
		u=userlogin; p=userpwd;
	}
	
	/**
	 * Login to this server
	 */
    public boolean login() {
    	class MyRunnable implements Runnable {
    		boolean loginok=false;
			@Override
			public void run() {
				System.out.println("start login run");
				HttpParams httpparms = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
				HttpConnectionParams.setSoTimeout(httpparms, 6000);
				httpclient = new DefaultHttpClient(httpparms);
				try {
					String cmd = server+"login.php?quick_mode=1&userid="+u+"&passwd="+p;
				    System.out.println("debug login cmd "+cmd);
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
							if (s.contains("#Error"))
								logger.showMsg("Error login; check credentials");
						}
						fin.close();
					}
					loginok=true;
				} catch (Exception e) {
					e.printStackTrace();
//				    if (s.contains("#Error")) logger.showMsg("Error login; check credentials");
					logger.showMsg(netErrMsg);
					loginok=false;
				}
				System.out.println("end login run");
			}
    	};
    	MyRunnable r = new MyRunnable();
    	threadRunner.runInThread(r);
    	return r.loginok;
    }
    
	JSONObject o=null;
    /**
     * send a command to the server and gets back a JSon object with the answer
     * @param cmd
     * @return
     */
    public JSONObject sendCmdToServer(final String cmd) {
		System.out.println("begin send command, httpclient="+httpclient);
		if (httpclient==null) {
			boolean loginok = login();
			System.out.println("login success: "+loginok);
			if (!loginok) {
				return null;
			}
		}
		System.out.println("now httpclient="+httpclient);
    	Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					 System.out.println("debug send cmd "+server+cmd);
						HttpGet httpget = new HttpGet(server+cmd);
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
								System.out.println("cmdlog "+s);
								s=s.trim();
								if (s.length()>0 && s.charAt(0)=='{') {
								    o = new JSONObject(s);
								    break;
								}
							}
							fin.close();
						}
				} catch (Exception e) {
					e.printStackTrace();
					logger.showMsg(netErrMsg);
				}
			}
		};
		o=null;
		threadRunner.runInThread(r);
//    	if (GoJsActivity.main!=null) {
//    		Thread cmdthread = GoJsActivity.main.runInWaitingThread(r);
//    	} else {
//    		Thread cmdthread = new Thread(r);
//    		cmdthread.start();
//    	}
		return o;
    }
    
//    public void dowbloadListOfGames() {
//		GoJsActivity.main.runInWaitingThread(new Runnable() {
//			@Override
//			public void run() {
//				HttpParams httpparms = new BasicHttpParams();
//				HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
//				HttpConnectionParams.setSoTimeout(httpparms, 6000);
//				HttpClient httpclient = new DefaultHttpClient(httpparms);
//				try {
//					String cmd = server+"login.php?quick_mode=1&userid="+u+"&passwd="+p;
//				    System.out.println("debug cmd "+cmd);
//					HttpGet httpget = new HttpGet(cmd);
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
//							System.out.println("LOGINlog "+s);
//							s=s.trim();
//							if (s.length()>0 && s.charAt(0)=='{') {
//							    JSONObject o = new JSONObject(s);
//							}
//						}
//						fin.close();
//					}
//
////					Message.handleMessages(GoJsActivity.main);
//					
//					// get list of games to play
////                    httpget = new HttpGet(server+"quick_status.php?order=0");
//                    httpget = new HttpGet(server+"quick_do.php?obj=game&cmd=list&view=status");
//					response = httpclient.execute(httpget);
//					heds = response.getAllHeaders();
//					for (Header s : heds)
//						System.out.println("[HEADER] "+s);
//					entity = response.getEntity();
//					games2play.clear();
//					if (entity != null) {
//						InputStream instream = entity.getContent();
//						BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("UTF-8")));
//						for (;;) {
//							String s = fin.readLine();
//							if (s==null) break;
//							s=s.trim();
//							if (s.length()>0 && s.charAt(0)=='{') {
//							    JSONObject o = new JSONObject(s);
//							    System.out.println("debugjson "+o.getInt("list_size"));
//							    if (o.getInt("list_size")>0) {
//	                                JSONArray headers = o.getJSONArray("list_header");
//	                                int gid_jsonidx = -1;
//	                                for (int i=0;i<headers.length();i++) {
//	                                    String h = headers.getString(i);
//	                                    System.out.println("jsonheader "+i+" "+h);
//	                                    if (h.equals("id")) gid_jsonidx=i;
//	                                }
//	                                JSONArray jsongames = o.getJSONArray("list_result");
//	                                for (int i=0;i<jsongames.length();i++) {
//	                                    JSONArray jsongame = jsongames.getJSONArray(i);
//	                                    int gameid = jsongame.getInt(gid_jsonidx);
//	                                    Game g = new Game(gameid);
//	                                    games2play.add(g);
//	                                }
//							    }
//							}
//							System.out.println("LOGINstatus "+s);
//						}
//						fin.close();
//					}
//					System.out.println("NBGAMES "+games2play.size());
//					showMsg("Nb games to play: "+games2play.size());
//
//					if (games2play.size()>0) {
//						curgidx2play=0;
//						downloadAndShowGame();
//						// download detects if 2 passes and auto change state to markdeadstones
//						if (curstate!=guistate.markDeadStones) changeState(guistate.play);
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//					showMsg(netErrMsg);
    //				}
    //			}

    static String[] loadCredsFromFile(String file) {
    	String u=null, p=null;
    	try {
    		BufferedReader f=new BufferedReader(new FileReader(file));
    		u=f.readLine();
    		p=f.readLine();
    		f.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	String[] res = {u,p};
    	return res;
    }
    
    public static void main(String args[]) throws Exception {
    	String[] c = loadCredsFromFile("creds.txt");
    	ServerConnection server = new ServerConnection(0,c[0],c[1]);
    	JSONObject o = server.sendCmdToServer(cmdGetListOfGames);
    	System.out.println("answer: "+o);
    	server.closeConnection();
    }
}