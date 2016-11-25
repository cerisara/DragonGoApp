package fr.xtof54.dragongoapp;

import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Game {
    public int id=-1;
    public boolean isdgs = false;
    public boolean gotNewMove=false;
    ArrayList<String> sgf = new ArrayList<String>();
    int myid, blackid, whiteid;
    private JSONArray json=null;
    String oppmove;
    int oppmoveid;

    public void setSGF(ArrayList<String> sgf0) {
        sgf=sgf0;
    }

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

    public Game() {
        sgf.add("(;FF[4]KM[0]SZ[19])");
        id=-1;
    }
    public Game(JSONArray json, int gameid) {
        id=gameid;
        this.json=json;
    }

    public void save() {
        // TODO save in f+id.sgf in case of skip game
    }

	public void write2html() {
	    System.out.println("writing game sgf to example.html");
		if (sgf==null) {
			System.out.println("ERROR impossible to show game ");
			return;
		}
		try {
			PrintWriter fout = new PrintWriter(new FileWriter(DragonGoAct.main.eidogodir+"/example.html"));
			for (String s : exampleFileHtmlHeader) fout.println(s);
			for (int i=0;i<sgf.size();i++) fout.println(sgf.get(i));
			for (String s : htmlend) fout.println(s);
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static ArrayList<Game> loadJSONStatus(String s) {
        System.out.println("LOADJSONNNNNNNNNNNNNNNNN "+s);
        ArrayList<Game> games = new ArrayList<Game>();
		try {
            JSONObject o = new JSONObject(s);
            if (o==null) return null;
			int ngames = o.getInt("list_size");
            System.out.println("LOADJSONNNNNNNNNNNNNNNNN ngames "+ngames);
			if (ngames>0) {
				JSONArray headers = o.getJSONArray("list_header");
				int gid_jsonidx=-1, movejsoni=-1, moveidjson=-1;
				int myidjson=-1, whiteidjson=-1, blackidjson=-1;
				for (int i=0;i<headers.length();i++) {
					String h = headers.getString(i);
					System.out.println("jsonheader "+i+" "+h);
					if (h.equals("id")) gid_jsonidx=i;
					else if (h.equals("move_last")) movejsoni=i;
					else if (h.equals("move_id")) moveidjson=i;
					else if (h.equals("my_id")) myidjson=i;
					else if (h.equals("white_user.id")) whiteidjson=i;
					else if (h.equals("black_user.id")) blackidjson=i;
				}
				JSONArray jsongames = o.getJSONArray("list_result");
				int[] users = {-1,-1,-1};
				for (int i=0;i<jsongames.length();i++) {
					JSONArray jsongame = jsongames.getJSONArray(i);
					int gameid = jsongame.getInt(gid_jsonidx);
					Game g = new Game(jsongame, gameid);
					if (movejsoni>=0&&moveidjson>=0) {
                        g.oppmove = jsongame.getString(movejsoni);
                        g.oppmoveid = jsongame.getInt(moveidjson);
                    }
					if (myidjson>=0)    users[0]=jsongame.getInt(myidjson);
					if (blackidjson>=0) users[1]=jsongame.getInt(blackidjson);
					if (whiteidjson>=0) users[2]=jsongame.getInt(whiteidjson);
                    g.myid = users[0];
                    g.blackid = users[1];
                    g.whiteid = users[2];
					games.add(g);
				}
                System.out.println("games in the list "+games.size());
			} else return games;
		} catch (JSONException e) {
			e.printStackTrace();
		}
        return null;
    }
}

