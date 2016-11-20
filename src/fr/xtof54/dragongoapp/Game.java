package fr.xtof54.dragongoapp;

import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

public class Game {
    public int id=-1;
    public boolean isdgs = false;
    public boolean gotNewMove=false;
    ArrayList<String> sgf = new ArrayList<String>();

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

}

