package fr.xtof54.jsgo;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xtof2 on 04/06/15.
 */
public class OGSConnection {
    final static String ioserver = "http://ggs.online-go.com";

    static String atoken = null, rtoken = null;
    static HttpContext httpctxt = new BasicHttpContext();
    static HttpClient httpclient = null;
    static boolean sendMoveIsSuccess = true;
    static BasicCookieStore mycookiestore = new BasicCookieStore();

    // to be run on a separate thread
    public static String getClientIDFromPrivateConfigfile() {
        String res = null;
        try {
            ClassLoader cl = ((Object) GoJsActivity.main).getClass().getClassLoader();
            InputStream is = cl.getResourceAsStream("assets/privateconfig.txt");
            BufferedReader b = new BufferedReader(new InputStreamReader(is));
            for (; ; ) {
                String s = b.readLine();
                if (s == null) break;
                if (s.startsWith("CLIENTID=")) {
                    res = s.substring(9);
                    break;
                }
            }
            is.close();
        } catch (Exception e) {
            return null;
        }
        return res;
    }

    public static String getClientSecretFromPrivateConfigfile() {
        String res = null;
        try {
            ClassLoader cl = ((Object) GoJsActivity.main).getClass().getClassLoader();
            InputStream is = cl.getResourceAsStream("assets/privateconfig.txt");
            BufferedReader b = new BufferedReader(new InputStreamReader(is));
            for (; ; ) {
                String s = b.readLine();
                if (s == null) break;
                if (s.startsWith("CLIENTSECRET=")) {
                    res = s.substring(13);
                    break;
                }
            }
            is.close();
        } catch (Exception e) {
            return null;
        }
        return res;
    }

    private static void initHttp() {
        if (httpclient == null) {
            HttpParams httpparms = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpparms, 6000);
            HttpConnectionParams.setSoTimeout(httpparms, 6000);
            httpclient = new DefaultHttpClient(httpparms);
        }
    }

    static void acceptChallenge(String challid) {
        initHttp();
        try {
            final String cmd = "https://online-go.com/api/v1/me/challenges/" + challid + "/accept/";
            HttpPost httppost = new HttpPost(cmd);
            String header = "Bearer " + atoken;
            httppost.addHeader("Authorization", header);
            HttpResponse response = httpclient.execute(httppost, httpctxt);
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            for (; ; ) {
                String s = fin.readLine();
                if (s == null) break;
                System.out.println("ogsacceptchallenge " + s);
            }
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getNotifications0() {
        ArrayList<String> newgames2play = new ArrayList<String>();
        if (atoken == null) return null;
        try {
            initHttp();
            final String cmd = "https://online-go.com/api/v1/me/notifications";
            HttpGet httpget = new HttpGet(cmd);
            String header = "Bearer " + atoken;
            System.out.println("ogs getnotifications header " + header);
            httpget.addHeader("Authorization", header);
            HttpResponse response = httpclient.execute(httpget, httpctxt);

            // retrieve the notifications
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            for (; ; ) {
                String s = fin.readLine();
                System.out.println("ogsnotlog " + s);
                if (s == null) break;
                if (s.indexOf("Authentication credentials were not provided") >= 0) {
                    GoJsActivity.main.showMessage("Auth. failed no credentials");
                    return null;
                }
                // TODO: do a real JSON parsing
                // look for new challenges
                for (int deb = 0; ; ) {
                    int i = s.indexOf("\"type\": \"challenge\"", deb);
                    if (i < 0) break;
                    deb = i + 8;
                    String challid = null, challenger = null;
                    int i0 = i;
                    {
                        int j = s.lastIndexOf("challenge_id", i);
                        if (j < 0) continue;
                        i = s.indexOf(':', j);
                        if (i < 0) continue;
                        else i += 2;
                        j = s.indexOf(',', i);
                        if (j < 0) continue;
                        challid = s.substring(i, j);
                    }
                    {
                        int j = s.lastIndexOf("username", i0);
                        if (j < 0) continue;
                        i = s.indexOf(':', j);
                        if (i < 0) continue;
                        else i += 3;
                        j = s.indexOf('"', i);
                        if (j < 0) continue;
                        challenger = s.substring(i, j);
                    }
                    System.out.println("ogs notifications found challengeid " + challid + " " + challenger);
                    if (challenger != null && challid != null) {
                        final String chid = challid;
                        GUI.askUser("Challenge from " + challenger + ": accept ?", new GUI.FunctionOK() {
                            @Override
                            public void isOK() {
                                acceptChallenge(chid);
                            }
                        });
                    }
                }

                // look for games where it's my move
                for (int deb = 0; ; ) {
                    int i = s.indexOf("\"type\": \"yourMove\"", deb);
                    if (i < 0) break;
                    deb = i + 8;
                    int j = s.lastIndexOf("game_id", i);
                    if (j < 0) {
                        GoJsActivity.main.showMessage("ERROR in JSON got from OGS");
                        return newgames2play;
                    }
                    i = s.indexOf(':', j);
                    if (i < 0) {
                        GoJsActivity.main.showMessage("ERROR in JSON got from OGS");
                        return newgames2play;
                    } else i += 2;
                    j = s.indexOf(',', i);
                    if (j < 0) {
                        GoJsActivity.main.showMessage("ERROR in JSON got from OGS");
                        return newgames2play;
                    }
                    String gameid = s.substring(i, j);
                    newgames2play.add(gameid);
                    System.out.println("ogs notifications found gameid " + gameid);
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

    public static boolean nextGame2play(Game g) {
        initHttp();
        int i = -g.getGameID();
        String gid = "" + i;
        if (downloadGame(gid)) {
            return true;
        } else {
            GoJsActivity.main.showMessage("Error downloading OGS game");
            GoJsActivity.main.changeState(GoJsActivity.guistate.nogame);
            return false;
        }
    }

    public static boolean sendMove(int gameid, String move) {
        // faut-il mettre tout ca dans un thread ?
        initHttp();
        sendMoveIsSuccess = true;
        try {
            final String cmd = "https://online-go.com/api/v1/games/" + gameid + "/move/";
            HttpPost httppost = new HttpPost(cmd);
            String header = "Bearer " + atoken;
            httppost.addHeader("Authorization", header);

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("move", move));
            System.out.println("ogs sendmove " + move);
            UrlEncodedFormEntity entity;
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse response = httpclient.execute(httppost, httpctxt);

            // retrieve the response
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            for (; ; ) {
                String s = fin.readLine();
                if (s == null) break;
                System.out.println("ogssendmovelog " + s);
                if (s.indexOf("error") >= 0) sendMoveIsSuccess = false;
            }
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
            sendMoveIsSuccess = false;
        }
        return sendMoveIsSuccess;
    }

    private static String downoadGameReview(String gameid) {
        try {
            /*
            dans api/v1/games/gameid, il y a dans:
            - moves = tous les coups joues
            - XXX: le temps restant
            - last_move: un move ID ??
            ...
             */
            final String cmd = "https://online-go.com/api/v1/games/" + gameid + "/reviews/";
            HttpGet httpget = new HttpGet(cmd);
            String header = "Bearer " + atoken;
            httpget.addHeader("Authorization", header);
            HttpResponse response = httpclient.execute(httpget, httpctxt);

            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            for (; ; ) {
                String s = fin.readLine();
                System.out.println("ogsreviewlog " + s);
                if (s == null) break;
                if (s.indexOf("Authentication credentials were not provided") >= 0) {
                    GoJsActivity.main.showMessage("Auth. failed no credentials");
                    return null;
                }
                s = s.trim();
                if (s.length() > 0) {
                    // TODO: add the review as sgf comments ?
                }
            }
            fin.close();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // return true iff download succeeded
    private static boolean downloadGame(String gid) {
        downoadGameReview(gid);
        try {
            /*
            dans api/v1/games/gameid, il y a dans:
            - moves = tous les coups joues
            - XXX: le temps restant
            - last_move: un move ID ??
            ...
             */
            final String cmd = "https://online-go.com/api/v1/games/" + gid + "/sgf/";
            HttpGet httpget = new HttpGet(cmd);
            String header = "Bearer " + atoken;
            httpget.addHeader("Authorization", header);
            HttpResponse response = httpclient.execute(httpget, httpctxt);

            // retrieve the game details
            BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
            int gamid = Integer.parseInt(gid);
            // j'utilise un - pour differencier games de OGS de DGS
            Game g = new Game(null, -gamid);
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
                s = s.trim();
                if (s.length() > 0) {
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

            // get chat data
            connectChat(gamid);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void login() {
        GUI.showWaitingWin();
        if (atoken == null) {
            System.out.println("ogs no access token, trying login...");
            login0();
        }
        if (atoken == null) {
            System.out.println("ogs no access after login. Stopping");
        } else {
            List<String> games2play = getNotifications0();
            {
                // add these games into the main games list
                List<Game> mainGameList = Game.getGames();
                for (String newgame : games2play) {
                    int newgid = Integer.parseInt(newgame);
                    boolean isAlreadyHere = false;
                    for (Game g : mainGameList) {
                        if (g.getGameID() == -newgid) {
                            isAlreadyHere = true;
                            break;
                        }
                    }
                    if (!isAlreadyHere) {
                        Game newg = new Game(null, -newgid);
                        mainGameList.add(newg);
                    }
                }
            }
//                    nextGame2play();
        }
        GUI.hideWaitingWin();
    }

    public static boolean login0() {
        System.out.println("ogs connectchat");
        connectChat(123);
        System.out.println("login0");
        String OGSCLIENTID = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTID, null);
        if (OGSCLIENTID == null) {
            OGSCLIENTID = getClientIDFromPrivateConfigfile();
            System.out.println("ogs found client " + OGSCLIENTID);
            if (OGSCLIENTID == null) {
                GoJsActivity.main.showMessage("You cannot connect to OGS with the app coming from F-Droid ! Please rather install: http://talc1.loria.fr/users/cerisara/DragonGoApp.apk");
                return false;
            } else
                PrefUtils.saveToPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTID, OGSCLIENTID);
        }

        String OGSCLIENTSECRET = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTSECRET, null);
        if (OGSCLIENTSECRET == null) {
            OGSCLIENTSECRET = getClientSecretFromPrivateConfigfile();
            System.out.println("ogs found secret " + OGSCLIENTID);
            if (OGSCLIENTID == null) {
                GoJsActivity.main.showMessage("You cannot connect to OGS with the app coming from F-Droid ! Please rather install: http://talc1.loria.fr/users/cerisara/DragonGoApp.apk");
                return false;
            } else
                PrefUtils.saveToPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_CLIENTSECRET, OGSCLIENTSECRET);
        }

        final String pwd = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_PASSWD, null);
        if (pwd == null)
            GoJsActivity.main.showMessage("You must first create an application-specific password on online-go.com," +
                    "and enter this password in this app Settings menu");
        else {
            final String user = PrefUtils.getFromPrefs(GoJsActivity.main.getApplicationContext(), PrefUtils.PREFS_LOGIN_OGS_USERNAME, null);
            if (user == null)
                GoJsActivity.main.showMessage("You must first enter your OGS credentials via the Settings menu");
            else {
                System.out.println("all info is here:");
                System.out.println("OGSuser " + user);
                System.out.println("OGSpwd " + pwd);
                System.out.println("login to OGS server");
                httpctxt.setAttribute(ClientContext.COOKIE_STORE, mycookiestore);
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
                    HttpResponse response = httpclient.execute(httppost, httpctxt);

                    // retrieve the access token from the response
                    BufferedReader fin = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Charset.forName("UTF-8")));
                    for (; ; ) {
                        String s = fin.readLine();
                        if (s == null) break;
                        System.out.println("ogslog " + s);
                        s = s.trim();
                        if (s.indexOf("error") >= 0) {
                            GoJsActivity.main.showMessage("ERROR login OGS " + s);
                        } else {
                            // retrieve the token
                            int i = s.indexOf("access_token");
                            if (i < 0) {
                                GoJsActivity.main.showMessage("ERROR login OGS " + s);
                            } else {
                                int j = s.indexOf(':', i);
                                i = s.indexOf('"', j) + 1;
                                j = s.indexOf('"', i);
                                atoken = s.substring(i, j);
                                i = s.indexOf("refresh_token");
                                if (i >= 0) {
                                    j = s.indexOf(':', i);
                                    i = s.indexOf('"', j) + 1;
                                    j = s.indexOf('"', i);
                                    rtoken = s.substring(i, j);
                                } else rtoken = null;
                                return true;
                            }
                        }
                    }
                    fin.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    static void connectChat(final int gameid) {
        try {
            final Socket socket = IO.socket(ioserver);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("PASS HERE");
                    try {
                        JSONObject parms = new JSONObject();
                        parms.put("game_id", gameid);
                        parms.put("player_id", 1);
                        parms.put("chat", true);
                        socket.emit("game/connect", parms);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }).on("game/"+gameid+"/gamedata", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    JSONObject obj = (JSONObject) args[0];
                    System.out.println("PASS HEREZ " + obj);
                    socket.disconnect();
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("PASS HEREGG");
                }

            });
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        System.out.println("dbug");
    }
}
