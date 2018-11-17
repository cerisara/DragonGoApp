package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import android.webkit.CookieManager;

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
	String u,p,server;
	private HttpClient httpclientdirect=null;
	private HttpContext httpctxt=null;
	private HttpsURLConnection conn = null;

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
		"https://www.dragongoserver.net/",
		"https://dragongoserver.sourceforge.net/"
	};


	/*
	   private HttpsURLConnection getConnection(String url) throws Exception {
	   URL request_url = new URL(url);
	   try {
	   conn = (HttpsURLConnection) request_url.openConnection();
	   conn.setRequestMethod("POST");
	   conn.setReadTimeout(95 * 1000);
	   conn.setConnectTimeout(95 * 1000);
	   conn.setDoInput(true);
	   conn.setRequestProperty("Accept", "application/json");
	   conn.setRequestProperty("X-Environment", "android");


	   CookieManager cookieManager = CookieManager.getInstance();
	   String cookie = cookieManager.getCookie(urlConnection.getURL().toString());
	   if (cookie != null) conn.setRequestProperty("Cookie", cookie);

	   List<String> cookieList = conn.getHeaderFields().get("Set-Cookie");
	   if (cookieList != null) {
	   for (String cookieTemp : cookieList) {
	   cookieManager.setCookie(conn.getURL().toString(), cookieTemp);
	   }
	   }


	   conn.setHostnameVerifier(new HostnameVerifier() {
	   @Override
	   public boolean verify(String hostname, SSLSession session) {
	   return HttpsURLConnection.getDefaultHostnameVerifier().verify("your_domain.com", session);
	   return true;
	   }
	   });
// conn.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
conn.connect();
} catch (Exception e) {
e.printStackTrace();
}
return conn;
	   }
	   */

void initHttp() {
	// this function is not called for normal play but it is called for forums, ladders...
	System.out.println("DGSAPP inithttp");
	if (conn==null) {
		try {
			String param = "userid="+URLEncoder.encode(u,"UTF-8")+
				"&passwd="+URLEncoder.encode(p,"UTF-8")+
				"&login="+URLEncoder.encode("Log in","UTF-8");
			URL url = new URL(getUrl()+"login.php?"+param);
			conn = (HttpsURLConnection)url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);

			CookieManager cookieManager = CookieManager.getInstance();
			String cookie = cookieManager.getCookie(conn.getURL().toString());
			if (cookie != null) conn.setRequestProperty("Cookie", cookie);
			List<String> cookieList = conn.getHeaderFields().get("Set-Cookie");
			if (cookieList != null) {
				for (String cookieTemp : cookieList) {
					cookieManager.setCookie(conn.getURL().toString(), cookieTemp);
				}
			}

			System.out.println("DGSAPP PARAMS "+param);
			//PrintWriter outputPost = new PrintWriter(conn.getOutputStream());
			//outputPost.print(param);
			//outputPost.close();

			String response= "";
			Scanner inStream = new Scanner(conn.getInputStream());
			while(inStream.hasNextLine()) response+=(inStream.nextLine());

			System.out.println("DGSAPP OKHTTP "+response);
		} catch (Exception e) {
			System.out.println("DGSAPP Exception "+e);
			e.printStackTrace();
		}

		/*
		   HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl()+"login.php").newBuilder();
		   urlBuilder.addQueryParameter("userid", u);
		   urlBuilder.addQueryParameter("password", p);
		   urlBuilder.addQueryParameter("login", "Log+in");

		   Request req = new Request.Builder()
		   .url(getUrl()+"login.php")
		   .build();

		   httpclientdirect = new DefaultHttpClient();
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
		GoJsActivity.main.updateTraffic();
		} catch (Exception e) {
		e.printStackTrace();
		GoJsActivity.main.updateTraffic();
		}
		*/
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

Ladder ladd = null;
String res;
/**
 * In the case of long strings, StringBuilder can crash because of OutOfMemory.
 * So the caller must saveInFile, and then get the string back from this file.
 * 
 * @param req
 * @param saveInFile
 * @return
 */
public String directConnectExecute(HttpUriRequest req, final String saveInFile) {
	res=null;
	try {
		// is it synchronous or not ?? Yes, it seems that it is synchronous
		httpclientdirect.execute(req,new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse response0) throws ClientProtocolException, IOException {
				Header[] httpHeader = response0.getHeaders("Location");
				while (httpHeader.length > 0) {
					// TODO cannot close httpclient but we should ensure to call releaseConnection() !
					//httpclientdirect.close();
					httpclientdirect = HttpClients.createDefault();
					String url = httpHeader[0].getValue();
					System.out.println("DGSAPP redirect "+url);
					HttpGet httpGet = new HttpGet(url);
					response0 = httpclientdirect.execute(httpGet);
					httpHeader = response0.getHeaders("Location");
				}
				final HttpResponse response = response0;

				StatusLine status = response.getStatusLine();
				System.out.println("DGSAPP httpclient execute status "+status.toString());
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
					System.out.println("DGSAPP building String with answer to request");
					StringBuilder sb = new StringBuilder();
					for (;;) {
						String s = fin.readLine();
						if (s==null) break;
						System.out.println("DGSAPP request answer "+s);
						sb.append(s);
					}
					res = sb.toString();
				} else {
					System.out.println("DGSAPP saving res in file "+saveInFile);
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
				GoJsActivity.main.updateTraffic();
				return res;
			}
		},httpctxt);
		System.out.println("DGSAPP just after execute...");
	} catch (Exception e) {
		e.printStackTrace();
		GoJsActivity.main.updateTraffic();
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

			HttpGet get = new HttpGet(getUrl()+"tournaments/ladder/challenge.php?tid="+ladd.ladnum+"&rid="+rid);
			String res = directConnectExecute(get,null);
			System.out.println("challengeladder - got server answer");

			// check if challenge possible
			int i=res.indexOf("Defender is not");
			if (i>=0) {
				EventManager.getEventManager().sendEvent(eventType.ladderChallengeEnd);
				EventManager.getEventManager().sendEvent(eventType.showMessage,"Defender is not any more challengeable");
				GoJsActivity.main.updateTraffic();
				return;
			}
			i=res.indexOf("Please confirm if you want to challenge this user");
			if (i<0) {
				EventManager.getEventManager().sendEvent(eventType.ladderChallengeEnd);
				EventManager.getEventManager().sendEvent(eventType.showMessage,"Error when trying to challenge user");
				GoJsActivity.main.updateTraffic();
				return;
			}
			// challenge is possible.
			{
				HttpGet cget = new HttpGet(getUrl()+"tournaments/ladder/challenge.php?tl_challenge=Confirm+Challenge&tid="+ladd.ladnum+"&rid="+rid+"&confirm=1");
				String cres = directConnectExecute(cget,null);
				System.out.println("challengeladder - got server answer to challenge confirm");
				System.out.println(cres);
				EventManager.getEventManager().sendEvent(eventType.showMessage,"Challenge "+ladd.userList[pos]+" ok");
			}

			// warning: displaying this String may not be done completely, why ?
			//                System.out.println(res);
			EventManager.getEventManager().sendEvent(eventType.ladderChallengeEnd);
			GoJsActivity.main.updateTraffic();
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
		GoJsActivity.main.updateTraffic();
		return;
	}
	Thread t = new Thread(new Runnable() {
		@Override
		public void run() {
			initHttp();
			System.out.println("getladder - getlogin passed");

			HttpGet get = new HttpGet(getUrl()+"tournaments/ladder/view.php?tid="+ladd.ladnum);
			// this file is erased next time we load one of the ladder - it's useless to keep both ladders in such files: it could even be removed afterwards...
			final String cacheFile = "ladderHtmlString";
			directConnectExecute(get, dir.getAbsolutePath()+"/"+cacheFile);
			ladd.loadHTML(dir.getAbsolutePath()+"/"+cacheFile);
			System.out.println("getladder - got server answer");
			GoJsActivity.main.updateTraffic();
			EventManager.getEventManager().sendEvent(eventType.ladderEnd);
		}
	});
	t.start();
}
}
