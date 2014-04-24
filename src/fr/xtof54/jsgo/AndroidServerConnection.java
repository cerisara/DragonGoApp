package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;

import fr.xtof54.jsgo.EventManager.eventType;

/**
 * This class is the "new" version of the ServerConnection, but specific to Android.
 * It shall ultimately completely replace ServerConnection.
 * 
 * 
 * @author xtof
 *
 */
public class AndroidServerConnection {
	private String u,p,server;
	private AndroidHttpClient httpclientdirect=null;
	private HttpContext httpctxt=null;

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
	public void setLogger(DetLogger l) {logger=l;}


	final String[] serverNames = {
			"http://www.dragongoserver.net/",
			"http://dragongoserver.sourceforge.net/"
	};

	private void initHttp() {
		if (httpclientdirect==null) {
			httpclientdirect = AndroidHttpClient.newInstance(null);
			httpctxt = new BasicHttpContext();
			httpctxt.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
			// login
			try {
				List<NameValuePair> formparams = new ArrayList<NameValuePair>();
				formparams.add(new BasicNameValuePair("userid", u));
				formparams.add(new BasicNameValuePair("passwd", p));
				formparams.add(new BasicNameValuePair("login", "Log+In"));
				UrlEncodedFormEntity entity;
				entity = new UrlEncodedFormEntity(formparams, "UTF-8");
				HttpPost httppost = new HttpPost(getUrl()+"login.php");
				httppost.setEntity(entity);
				directConnectExecute(httppost,null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void closeConnection() {
		if (httpclientdirect!=null)
			httpclientdirect.getConnectionManager().shutdown();
		httpclientdirect=null;
	}

	/**
	 * creates a connection to a specific server;
	 * determines the correct credentials
	 * @param num
	 */
	public AndroidServerConnection(int num, String userlogin, String userpwd) {
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

	public String getUrl() {
		return server;
	}
	public String getLogin() {
		return u;
	}
	public String getPwd() {
		return p;
	}

	// ====================================================================
	/*
	 * you'll find below direct connection to the DGS server, without using the quicksuite !
	 */

	Ladder ladd = new Ladder();
	String res;
	
	/**
	 * In the case of long strings, StringBuilder can crash because of OutOfMemory.
	 * So the caller must saveInFile, and then get the string back from this file.
	 * 
	 * @param req
	 * @param saveInFile
	 * @return
	 */
	private String directConnectExecute(HttpUriRequest req, final String saveInFile) {
		res=null;
		try {
			// is it synchronous or not ?? Yes, it seems that it is synchronous
			httpclientdirect.execute(req,new ResponseHandler<String>() {
				@Override
				public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
					StatusLine status = response.getStatusLine();
					System.out.println("httpclient execute status "+status.toString());
					HttpEntity entity = response.getEntity();
					// no because I got a status>=300 but login succeeds...
//					if (status.getStatusCode() >= 300) {
//						throw new HttpResponseException(status.getStatusCode(),status.getReasonPhrase());
//					}
					if (entity == null) {
						throw new ClientProtocolException("Response contains no content");
					}
					InputStream instream = entity.getContent();
					BufferedReader fin = new BufferedReader(new InputStreamReader(instream, Charset.forName("ISO-8859-1")));
					if (saveInFile==null) {
						StringBuilder sb = new StringBuilder();
						for (;;) {
							String s = fin.readLine();
							if (s==null) break;
							//						System.out.println("request answer "+s);
							sb.append(s);
						}
						res = sb.toString();
					} else {
						System.out.println("saving res in file "+saveInFile);
						PrintWriter fout = new PrintWriter(new FileWriter(saveInFile));
						for (;;) {
							String s = fin.readLine();
							if (s==null) break;
							fout.print(s);
						}
						fout.close();
						res=null;
					}
					fin.close();
					return res;
				}
			},httpctxt);
			System.out.println("just after execute...");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public void ladderChallenge(final String rid, final int pos) {
        EventManager.getEventManager().sendEvent(eventType.ladderChallengeStart);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                initHttp();
                System.out.println("challengeladder - getlogin passed");
                
                HttpGet get = new HttpGet(getUrl()+"tournaments/ladder/challenge.php?tid=3&rid="+rid);
                String res = directConnectExecute(get,null);
                System.out.println("challengeladder - got server answer");
                
                // check if challenge possible
                int i=res.indexOf("Defender is not");
                if (i>=0) {
                    EventManager.getEventManager().sendEvent(eventType.ladderChallengeEnd);
                    EventManager.getEventManager().sendEvent(eventType.showMessage,"Defender is not any more challengeable");
                    return;
                }
                i=res.indexOf("Please confirm if you want to challenge this user");
                if (i<0) {
                    EventManager.getEventManager().sendEvent(eventType.ladderChallengeEnd);
                    EventManager.getEventManager().sendEvent(eventType.showMessage,"Error when trying to challenge user");
                    return;
                }
                // challenge is possible.
                {
                    HttpGet cget = new HttpGet(getUrl()+"tournaments/ladder/challenge.php?tl_challenge=Confirm+Challenge&tid=3&rid="+rid+"&confirm=1");
                    String cres = directConnectExecute(cget,null);
                    System.out.println("challengeladder - got server answer to challenge confirm");
                    System.out.println(cres);
                    EventManager.getEventManager().sendEvent(eventType.showMessage,"Challenge "+ladd.userList[pos]+" ok");
                }
                
                // warning: displaying this String may not be done completely, why ?
//                System.out.println(res);
                EventManager.getEventManager().sendEvent(eventType.ladderChallengeEnd);
            }
        });
        t.start();
	}
	
	/**
	 * @return the players that you can challenge in the ladder, and only them !
	 */
	public void startLadderView(final File dir) {
		EventManager.getEventManager().sendEvent(eventType.ladderStart);
		if (ladd.isLadderCached()) {
			EventManager.getEventManager().sendEvent(eventType.ladderEnd);
			return;
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				initHttp();
				System.out.println("getladder - getlogin passed");
				
				HttpGet get = new HttpGet(getUrl()+"tournaments/ladder/view.php?tid=3");
				final String cacheFile = "ladderHtmlString";
				directConnectExecute(get, dir.getAbsolutePath()+"/"+cacheFile);
				ladd.loadHTML(dir.getAbsolutePath()+"/"+cacheFile);
				System.out.println("getladder - got server answer");
				EventManager.getEventManager().sendEvent(eventType.ladderEnd);
			}
		});
		t.start();
	}
}
