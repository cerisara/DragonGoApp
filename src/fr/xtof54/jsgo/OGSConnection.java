package fr.xtof54.jsgo;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xtof2 on 04/06/15.
 */
public class OGSConnection {
    static String atoken=null, rtoken=null;
    static HttpContext httpctxt = new BasicHttpContext();
    static HttpClient httpclient = null;
    static List<String> games2play;

    // to be run on a separate thread
    public static String getWithHttpGet(String cmd) {
        System.out.println("login to OGS server");
        initHttp();
        String res=null;
        try {
            HttpGet httpget = new HttpGet(cmd);
            HttpResponse response = httpclient.execute(httpget);
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            for (; ; ) {
                String s = fin.readLine();
                if (s == null) break;
                System.out.println("talc1log " + s);
                s = s.trim();
                int i=s.indexOf("detinfo=");
                if (i >= 0) {
                    res=s.substring(i+8);
                    break;
                }
            }
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private static void initHttp() {
        if (httpclient==null) {
            HttpParams httpparms = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
            HttpConnectionParams.setSoTimeout(httpparms, 6000);
            httpclient = new DefaultHttpClient(httpparms);
        }
    }

    public static List<String> getNotifications0() {
        ArrayList<String> newgames2play = new ArrayList<String>();
        if (atoken==null) return null;
        try {
            initHttp();
            final String cmd = "https://online-go.com/api/v1/me/notifications";
            HttpGet httpget = new HttpGet(cmd);
            String header = "Bearer "+atoken;
            System.out.println("ogs getnotifications header "+header);
            httpget.addHeader("Authorization",header);
            HttpResponse response = httpclient.execute(httpget,httpctxt);

            // retrieve the notifications
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            for (; ; ) {
                String s = fin.readLine();
                System.out.println("ogsnotlog " + s);
                if (s == null) break;
                if (s.indexOf("Authentication credentials were not provided")>=0) {
                    GoJsActivity.main.showMessage("Auth. failed no credentials");
                    return null;
                }
                // TODO: do a real JSON parsing
                // look for games where it's my move
                for (int deb=0;;) {
                    int i = s.indexOf("\"type\": \"yourMove\"", deb);
                    if (i<0) break;
                    deb=i+8;
                    int j=s.lastIndexOf("game_id",i);
                    if (j<0) {
                        GoJsActivity.main.showMessage("ERROR in JSON got from OGS");
                        return newgames2play;
                    }
                    i=s.indexOf(':',j);
                    if (i<0) {
                        GoJsActivity.main.showMessage("ERROR in JSON got from OGS");
                        return newgames2play;
                    } else i+=2;
                    j=s.indexOf(',',i);
                    if (j<0) {
                        GoJsActivity.main.showMessage("ERROR in JSON got from OGS");
                        return newgames2play;
                    }
                    String gameid = s.substring(i,j);
                    newgames2play.add(gameid);
                    System.out.println("ogs notifications found gameid "+gameid);
                }
            }
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newgames2play;
    }

    public static void loadGame(String gameid) {
        // now download the sgf of this game
        // TODO: try and download ONLY the last opponent move
    }

    public static boolean nextGame2play() {
        if (games2play==null||games2play.size()==0) {
            GoJsActivity.main.showMessage("no more OGS game to play");
            GoJsActivity.main.changeState(GoJsActivity.guistate.nogame);
            return false;
        }
        initHttp();
        String gid = games2play.get(0);
        if (downloadGame(gid)) {
            games2play.remove(0);
            return true;
        } else {
            GoJsActivity.main.showMessage("Error downloading OGS game");
            GoJsActivity.main.changeState(GoJsActivity.guistate.nogame);
            return false;
        }
    }

    public static boolean sendMove(int gameid, String move) {
        initHttp();
        boolean sendMoveOK=true;
        try {
            final String cmd = "https://online-go.com/api/v1/games/"+gameid+"/move/";
            HttpPost httppost = new HttpPost(cmd);
            String header = "Bearer "+atoken;
            httppost.addHeader("Authorization",header);

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("move", move));
            System.out.println("ogs sendmove "+move);
            UrlEncodedFormEntity entity;
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse response = httpclient.execute(httppost,httpctxt);

            // retrieve the response
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            for (; ; ) {
                String s = fin.readLine();
                if (s == null) break;
                System.out.println("ogssendmovelog " + s);
                if (s.indexOf("error")>=0) sendMoveOK=false;
            }
            fin.close();
            if (sendMoveOK) nextGame2play();
        } catch (Exception e) {
            e.printStackTrace();
            sendMoveOK=false;
        }
        return sendMoveOK;
    }

    // return true iff download succeeded
    private static boolean downloadGame(String gid) {
        try {
            /*
            dans api/v1/games/gameid, il y a dans:
            - moves = tous les coups joues
            - XXX: le temps restant
            - last_move: un move ID ??
            ...
             */
            final String cmd = "https://online-go.com/api/v1/games/"+gid+"/sgf/";
            HttpGet httpget = new HttpGet(cmd);
            String header = "Bearer " + atoken;
            httpget.addHeader("Authorization", header);
            HttpResponse response = httpclient.execute(httpget, httpctxt);

            // retrieve the game details
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            int gamid = Integer.parseInt(gid);
            // j'utilise un - pour differencier games de OGS de DGS
            Game g = new Game(null,-gamid);
            for (; ; ) {
                String s = fin.readLine();
                // on n'a ici QUE des lignes SGF
                System.out.println("ogsgamelog " + s);
                if (s == null) break;
                if (s.indexOf("Authentication credentials were not provided") >= 0) {
                    GoJsActivity.main.showMessage("Auth. failed no credentials");
                    return false;
                }
                // TODO: rendre Game moins dependant de DGS
                s=s.trim();
                if (s.length()>0) {
                    // this method does not trigger a local save
                    // but when we'll add the new single user move, this will save the sgf on disk
                    g.addSgfData(s);
                }
            }
            fin.close();
            // on a charge tout le SGF
            GoJsActivity.main.changeState(GoJsActivity.guistate.play);
            GoJsActivity.main.showMessage("OGS game: your turn !");
            GoJsActivity.main.showGame(g);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void login() {
        Thread push = new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO: dont log every time
                if (atoken == null) {
                    System.out.println("ogs no access token, trying login...");
                    login0();
                }
                if (atoken == null) {
                    System.out.println("ogs no access after login. Stopping");
                } else {
                    games2play = getNotifications0();
                    nextGame2play();
                }
            }
        });
        push.start();
    }

    public static void login0() {
        String OGSCLIENTID = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTID, null);
        if (OGSCLIENTID==null) {
            OGSCLIENTID = getWithHttpGet("http://talc1.loria.fr/users/cerisara/ogsooid.txt");
            PrefUtils.saveToPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTID, OGSCLIENTID);
        }
        if (OGSCLIENTID==null) {
            GoJsActivity.main.showMessage("Error connections A1");
            return;
        }
        String OGSCLIENTSECRET = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTSECRET, null);
        if (OGSCLIENTSECRET==null) {
            OGSCLIENTSECRET = getWithHttpGet("http://talc1.loria.fr/users/cerisara/ogsoose.txt");
            PrefUtils.saveToPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTSECRET, OGSCLIENTSECRET);
        }
        if (OGSCLIENTSECRET==null) {
            GoJsActivity.main.showMessage("Error connections A2");
            return;
        }

        final String pwd = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(),PrefUtils.PREFS_LOGIN_OGS_PASSWD,null);
        if (pwd==null) GoJsActivity.main.showMessage("You must first create an application-specific password on online-go.com," +
                "and enter this password in this app Settings menu");
        else {
            final String user = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(),PrefUtils.PREFS_LOGIN_OGS_USERNAME,null);
            if (user == null)
                GoJsActivity.main.showMessage("You must first enter your OGS credentials via the Settings menu");
            else {
                System.out.println("all info is here:");
                System.out.println("OGSuser " + user);
                System.out.println("OGSpwd " + pwd);
                System.out.println("login to OGS server");
                httpctxt.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
                initHttp();
                try {
                    String cmd = "https://online-go.com/oauth2/access_token";

                    List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                    formparams.add(new BasicNameValuePair("client_id", OGSCLIENTID));
                    formparams.add(new BasicNameValuePair("client_secret", OGSCLIENTSECRET));
                    formparams.add(new BasicNameValuePair("grant_type", "password"));
                    formparams.add(new BasicNameValuePair("username", user));
                    formparams.add(new BasicNameValuePair("password", pwd));
                    UrlEncodedFormEntity entity;
                    entity = new UrlEncodedFormEntity(formparams, "UTF-8");
                    HttpPost httppost = new HttpPost(cmd);
                    httppost.setEntity(entity);
                    HttpResponse response = httpclient.execute(httppost,httpctxt);

                    // retrieve the access token from the response
                    BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
                    for (; ; ) {
                        String s = fin.readLine();
                        if (s == null) break;
                        /*
                        {"access_token": "56e18706ffeaee8424e8a10dd6885d5cee1beb79", "scope": "read", "expires_in": 31535999, "refresh_token": "6e2863f2619a2e1bed773ba0a93b737724969eff"}
                         */
                        System.out.println("ogslog " + s);
                        s = s.trim();
                        if (s.indexOf("error") >= 0) {
                            GoJsActivity.main.showMessage("ERROR login OGS " + s);
                        } else {
                            // retrieve the token
                            int i=s.indexOf("access_token");
                            if (i<0) {
                                GoJsActivity.main.showMessage("ERROR login OGS " + s);
                            } else {
                                int j=s.indexOf(':',i);
                                i=s.indexOf('"',j)+1;
                                j=s.indexOf('"',i);
                                atoken = s.substring(i,j);
                                i=s.indexOf("refresh_token");
                                if (i>=0) {
                                    j = s.indexOf(':', i);
                                    i = s.indexOf('"', j) + 1;
                                    j = s.indexOf('"', i);
                                    rtoken = s.substring(i, j);
                                } else rtoken=null;
                            }
                        }
                    }
                    fin.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
