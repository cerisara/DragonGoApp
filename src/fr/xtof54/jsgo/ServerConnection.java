package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.util.Log;

import fr.xtof54.jsgo.EventManager.eventType;

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
    private HttpClient httpclientdirect=null;

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
     * WARNING: asynchronous / non-blocking !!
     */
    public boolean loginok=false;
    public void startLogin() {
        final EventManager em = EventManager.getEventManager();
        em.sendEvent(eventType.loginStarted);

        class MyRunnable implements Runnable {
            @Override
            public void run() {
                System.out.println("start login run");
                HttpParams httpparms = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
                HttpConnectionParams.setSoTimeout(httpparms, 6000);
                httpclient = new DefaultHttpClient(httpparms);
                try {
                	try {
                		// for now, just an anonymous ping for usage stats
                		// TODO: use it to check for updates + if enough stats, direct connection with the opponent app for live game
                        String cmd = "http://talc1.loria.fr/users/cerisara/DGSping.php?v="+GoJsActivity.dgappversion;
                        Log.i("login", cmd);
                        HttpGet httpget = new HttpGet(cmd);
                        httpclient.execute(httpget);
                	} catch (Exception e) {}
                	// login
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
                em.sendEvent(eventType.loginEnd);
                GoJsActivity.main.updateTraffic();
            }
        };
        MyRunnable r = new MyRunnable();
        Thread loginthread = new Thread(r);
        loginthread.start();
    }

    public JSONObject o=null;
    /**
     * send a command to the server and gets back a JSon object with the answer
     * 
     * WARNING: asynchronous / non-blocking !!
     * @param cmd
     * @return
     */
    public void sendCmdToServer(final String cmd, final eventType startEvent, final eventType endEvent) {
        o=null;
        System.out.println("begin send command, httpclient="+httpclient);
        final EventManager em = EventManager.getEventManager();
        if (startEvent!=null) em.sendEvent(startEvent);
        if (httpclient==null) {
            System.out.println("in sendcmd: no httpclient, trying login...");
            em.registerListener(eventType.loginEnd, new EventManager.EventListener() {
                @Override
                public String getName() {return "sendCmdToServer";}
                @Override
                public void reactToEvent() {
                    em.unregisterListener(eventType.loginEnd, this);
                    if (loginok) sendCmdToServer(cmd,null,endEvent);
                    else if (endEvent!=null) em.sendEvent(endEvent);
                }
            });
            startLogin();
            return;
        }
        System.out.println("now httpclient="+httpclient);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                boolean hasError = false;
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
                        String errmsg = o.getString("error");
                        if (errmsg!=null&&errmsg.length()>0) {
                            hasError=true;
                            handleNetError(errmsg,cmd,endEvent);
                        }
                    } else {
                    	// entity==null
                        hasError=true;
                        handleNetError("no server reply", cmd,endEvent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hasError=true;
                    handleNetError(e.toString(),cmd,endEvent);
                }
                System.out.println("server runnable terminated");
                GoJsActivity.main.updateTraffic();
                if (!hasError&&endEvent!=null) em.sendEvent(endEvent);
            }
        };
        Thread t = new Thread(r);
        t.start();
        //    	if (GoJsActivity.main!=null) {
        //    		Thread cmdthread = GoJsActivity.main.runInWaitingThread(r);
        //    	} else {
        //    		Thread cmdthread = new Thread(r);
        //    		cmdthread.start();
        //    	}
    }

    private void handleNetError(String cmd, final eventType endEvent) {
    	handleNetError("unknown error", cmd, endEvent);
    }
    private void handleNetError(String err, String cmd, final eventType endEvent) {
    	if (err.contains("not_logged_in")) {
    		// restart connection session, so that login is redone next time
    		closeConnection();
    	}
        logger.showMsg("Net error|"+cmd+"|"+endEvent);
    }

    public List<String> sgf = null;
    /**
     * because download sgf does not return a JSON object, we have to use a dedicated function to do it
     * @param gameid
     * @param sendEvent=true when called normally
     * @return
     */
    public void downloadSgf(final int gameid, boolean sendEvent) {
        final EventManager em = EventManager.getEventManager();
        if (sendEvent) em.sendEvent(eventType.downloadGameStarted);
        if (httpclient==null) {
            System.out.println("in getsgf: no httpclient, trying login...");
            em.registerListener(eventType.loginEnd, new EventManager.EventListener() {
                @Override
                public String getName() {return "downloadSgf";}
                @Override
                public void reactToEvent() {
                    em.unregisterListener(eventType.loginEnd, this);
                    if (loginok) downloadSgf(gameid, false);
                    else em.sendEvent(eventType.downloadGameEnd);
                }
            });
            startLogin();
            return;
        }
        sgf = new ArrayList<String>();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    final String cmd = server+"sgf.php?gid="+gameid+"&owned_comments=1&quick_mode=1";
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
                            s=s.trim();
                            if (s.length()>0&&s.charAt(0)!='[') {
                                sgf.add(s);
                            }
                            System.out.println("SGFdownload "+s);
                        }
                        fin.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.showMsg(netErrMsg);
                }
                GoJsActivity.main.updateTraffic();
                em.sendEvent(eventType.downloadGameEnd);
            }
        };
        Thread t = new Thread(r);
        t.start();
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
    private boolean isAlreadyDirectLogged = false;

    private String directConnectExecute(HttpPost post, HttpGet get) {
        if (httpclientdirect==null) {
            HttpParams httpparms = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
            HttpConnectionParams.setSoTimeout(httpparms, 6000);
            httpclientdirect = new DefaultHttpClient(httpparms);
        }
        HttpResponse response;
        String res="";
        try {
            if (post!=null)
                response = httpclientdirect.execute(post);
            else
                response = httpclientdirect.execute(get);
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
                    res+=s;
                }
                fin.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        GoJsActivity.main.updateTraffic();
        return res;
    }

    private String directLogin() {
        try {
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("userid", u));
            formparams.add(new BasicNameValuePair("passwd", p));
            formparams.add(new BasicNameValuePair("login", "Log+In"));
            UrlEncodedFormEntity entity;
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            HttpPost httppost = new HttpPost(getUrl()+"login.php");
            httppost.setEntity(entity);
            String answ = directConnectExecute(httppost, null);
            System.out.println("direct login anws: "+answ);
            // TODO: check if login succeeded
            isAlreadyDirectLogged=true;
            GoJsActivity.main.updateTraffic();
            return answ;
        } catch (Exception e) {
            e.printStackTrace();
            GoJsActivity.main.updateTraffic();
        }
        return "";
    }
    
    public String directInvite(String user, String msg) {
        if (!isAlreadyDirectLogged) directLogin();
        
        // TODO: do we need senderid ? Shall we URLencode ?
        // no, it looks like we don't need to URLEncode, and we don't need to fill-in all slots !
//      =&=&=0&senderid=180&&=&=
        
        try {
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("ruleset", "JAPANESE"));
            formparams.add(new BasicNameValuePair("size", "19"));
            formparams.add(new BasicNameValuePair("cat_htype", "manual"));
            formparams.add(new BasicNameValuePair("color_m", "nigiri"));
            formparams.add(new BasicNameValuePair("handicap_m", "0"));
            formparams.add(new BasicNameValuePair("komi_m", "6.5"));
            formparams.add(new BasicNameValuePair("fk_htype", "auko_opn"));
            formparams.add(new BasicNameValuePair("stdhandicap", "Y"));
            formparams.add(new BasicNameValuePair("adj_handicap", "0"));
            formparams.add(new BasicNameValuePair("min_handicap", "0"));
            formparams.add(new BasicNameValuePair("max_handicap", "-1"));
            formparams.add(new BasicNameValuePair("adj_komi", "0"));
            formparams.add(new BasicNameValuePair("jigo_mode", "KEEP_KOMI"));
            formparams.add(new BasicNameValuePair("timevalue", "30"));
            formparams.add(new BasicNameValuePair("timeunit", "days"));
            formparams.add(new BasicNameValuePair("byotimevalue_jap", "1"));
            formparams.add(new BasicNameValuePair("timeunit_jap", "days"));
            formparams.add(new BasicNameValuePair("byoperiods_jap", "10"));
            formparams.add(new BasicNameValuePair("byotimevalue_can", "15"));
            formparams.add(new BasicNameValuePair("timeunit_can", "days"));
            formparams.add(new BasicNameValuePair("byoperiods_can", "15"));
            formparams.add(new BasicNameValuePair("byoyomitype", "FIS"));
            formparams.add(new BasicNameValuePair("byotimevalue_fis", "1"));
            formparams.add(new BasicNameValuePair("timeunit_fis", "days"));
            formparams.add(new BasicNameValuePair("weekendclock", "Y"));
            formparams.add(new BasicNameValuePair("to", user));
            formparams.add(new BasicNameValuePair("message", msg));
            formparams.add(new BasicNameValuePair("send_message", "Send+Invitation"));
            formparams.add(new BasicNameValuePair("mode", "Invite"));
//            formparams.add(new BasicNameValuePair("mid", "0"));
            formparams.add(new BasicNameValuePair("view", "0"));
            formparams.add(new BasicNameValuePair("gsc", "1"));
            formparams.add(new BasicNameValuePair("subject", "Game+invitation"));
            formparams.add(new BasicNameValuePair("type", "INVITATION"));
            
            UrlEncodedFormEntity entity;
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            HttpPost httppost = new HttpPost(getUrl()+"message.php");
            httppost.setEntity(entity);
            String answ = directConnectExecute(httppost, null);
            GoJsActivity.main.updateTraffic();
            // TODO: check if success
            return answ;
        } catch (Exception e) {
            e.printStackTrace();
            GoJsActivity.main.updateTraffic();
        }
        return "";
    }
    
    // ====================================================

    private static void test1() throws Exception {
        final String[] c = loadCredsFromFile("creds.txt");
        ServerConnection server = new ServerConnection(0,c[0],c[1]);
        final EventManager em = EventManager.getEventManager();
        em.registerListener(eventType.downloadListEnd, new EventManager.EventListener() {
            @Override
            public String getName() {return "maintest";}
            @Override
            public void reactToEvent() {
                synchronized (c) {
                    c.notifyAll();
                }
            }
        });
        server.sendCmdToServer(cmdGetListOfGames,eventType.downloadListStarted, eventType.downloadListEnd);
        synchronized (c) {
            c.wait();
        }
        JSONObject o = server.o;
        System.out.println("answer: "+o);
        server.closeConnection();
    }
    private static void test2() throws Exception {
        final String[] c = loadCredsFromFile("creds.txt");
        final ServerConnection server = new ServerConnection(0,c[0],c[1]);
        String ans = server.directLogin();
        System.out.println("login answer: "+ans);
        System.out.println();

//        ans = server.directInvite("xtof54","");
//        System.out.println("invite answer");
        
        server.closeConnection();
    }
    public static void main(String args[]) throws Exception {
        //		test1();
        test2();
    }
}
