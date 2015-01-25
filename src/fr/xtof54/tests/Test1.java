package fr.xtof54.tests;

import org.json.JSONObject;

import fr.xtof54.jsgo.Game;

public class Test1 {
	public static void main(String args[]) throws Exception {
		final String downloaded = "{\"version\":\"1.19.5:7\",\"error\":\"\",\"quota_count\":499,\"quota_expire\":\"2015-01-25 09:47:55\",\"list_object\":\"game\",\"list_totals\":\"7\",\"list_size\":7,\"list_offset\":0,\"list_limit\":10,\"list_has_next\":0,\"list_order\":\"time_lastmove+,id-\",\"list_header\":[\"id\",\"double_id\",\"tournament_id\",\"game_action\",\"status\",\"flags\",\"score\",\"game_type\",\"rated\",\"ruleset\",\"size\",\"komi\",\"jigo_mode\",\"handicap\",\"handicap_mode\",\"shape_id\",\"time_started\",\"time_lastmove\",\"time_weekend_clock\",\"time_mode\",\"time_limit\",\"my_id\",\"move_id\",\"move_count\",\"move_color\",\"move_uid\",\"move_opp\",\"move_last\",\"prio\",\"black_user.id\",\"black_gameinfo.prisoners\",\"black_gameinfo.remtime\",\"black_gameinfo.rating_start\",\"black_gameinfo.rating_start_elo\",\"white_user.id\",\"white_gameinfo.prisoners\",\"white_gameinfo.remtime\",\"white_gameinfo.rating_start\",\"white_gameinfo.rating_start_elo\"],\"list_result\":[[962332,0,3,2,\"PLAY\",\"\",\"\",\"GO\",1,\"JAPANESE\",19,6,\"KEEP_KOMI\",0,\"STD\",0,\"2015-01-21 12:41:50\",\"2015-01-24 10:11:50\",1,\"FIS\",\"F: 10d + 1d 4h\",53858,28,28,\"B\",53858,10305,\"nf\",0,53858,0,\"F: 9d 2h (+ 1d 4h)\",\"7k (-31%)\",\"1369.3485138599\",10305,0,\"F: 10d (+ 1d 4h)\",\"9k (+46%)\",\"1246.2026990668\"],[957285,0,11,2,\"PLAY\",\"\",\"\",\"GO\",1,\"JAPANESE\",19,6.5,\"KEEP_KOMI\",0,\"STD\",0,\"2015-01-08 18:53:23\",\"2015-01-24 11:56:25\",1,\"CAN\",\"C: 1d + 14d \\/ 14\",53858,43,43,\"W\",53858,10493,\"iq\",0,10493,0,\"C: 13d 8h \\/ 13 (14d \\/ 14)\",\"5k (-47%)\",\"1553.2495361855\",53858,0,\"C: 12d 9h \\/ 7 (14d \\/ 14)\",\"7k (-40%)\",\"1360.2530343457\"],[957288,0,11,2,\"PLAY\",\"\",\"\",\"GO\",1,\"JAPANESE\",19,6.5,\"KEEP_KOMI\",0,\"STD\",0,\"2015-01-08 18:53:23\",\"2015-01-24 16:22:27\",1,\"CAN\",\"C: 1d + 14d \\/ 14\",53858,28,28,\"B\",53858,13085,\"rc\",0,53858,0,\"C: 10d 12h \\/ 1 (14d \\/ 14)\",\"7k (-40%)\",\"1360.2530343457\",13085,0,\"C: 3d 13h \\/ 3 (14d \\/ 14)\",\"6k (-4%)\",\"1495.9158307442\"],[939396,0,3,2,\"PLAY\",\"\",\"\",\"GO\",1,\"JAPANESE\",19,6,\"KEEP_KOMI\",0,\"STD\",0,\"2014-10-31 07:33:57\",\"2015-01-24 16:35:56\",1,\"FIS\",\"F: 10d + 1d 4h\",53858,136,136,\"B\",53858,7203,\"je\",0,53858,1,\"F: 9d 8h (+ 1d 4h)\",\"7k (+42%)\",\"1442.3610072675\",7203,0,\"F: 10d (+ 1d 4h)\",\"7k (+8%)\",\"1408.4476055197\"],[957276,0,11,2,\"PLAY\",\"\",\"\",\"GO\",1,\"JAPANESE\",19,6.5,\"KEEP_KOMI\",0,\"STD\",0,\"2015-01-08 18:53:23\",\"2015-01-24 18:34:32\",1,\"CAN\",\"C: 1d + 14d \\/ 14\",53858,104,104,\"B\",53858,50523,\"fh\",0,53858,1,\"C: 12d 6h \\/ 8 (14d \\/ 14)\",\"7k (-40%)\",\"1360.2530343457\",50523,0,\"C: 14d \\/ 13 (14d\\/ 14)\",\"4k (+41%)\",\"1741.1915182805\"],[957263,0,11,2,\"PLAY\",\"\",\"\",\"GO\",1,\"JAPANESE\",19,6.5,\"KEEP_KOMI\",0,\"STD\",0,\"2015-01-08 18:53:23\",\"2015-01-24 21:52:18\",1,\"CAN\",\"C: 1d + 14d \\/ 14\",53858,42,42,\"B\",53858,20456,\"ej\",0,53858,0,\"C: 13d 5h\\/ 10 (14d\\/ 14)\",\"7k (-40%)\",\"1360.2530343457\",20456,0,\"C: 12d 7h\\/ 7 (14d\\/ 14)\",\"1k (+40%)\",\"2039.7573825759\"],[957270,0,11,2,\"PLAY\",\"\",\"\",\"GO\",1,\"JAPANESE\",19,6.5,\"KEEP_KOMI\",0,\"STD\",0,\"2015-01-08 18:53:23\",\"2015-01-24 23:36:58\",1,\"CAN\",\"C: 1d + 14d\\/ 14\",53858,48,48,\"B\",53858,81404,\"ih\",0,53858,0,\"C: 11d 13h\\/ 5 (14d\\/ 14)\",\"7k (-40%)\",\"1360.2530343457\",81404,0,\"C: 8d 2h\\/ 5 (14d\\/ 14)\",\"2k (-15%)\",\"1885.0832137876\"]]}";
		
		JSONObject o=new JSONObject(downloaded);
		System.out.println(o.length());
		
		Game.parseJSONStatusGames(o);
		Game g = Game.games2play.get(0);
		System.out.println(g.getGameID());
		System.out.println("play opp "+g.oppMove+" "+g.newMoveId);
		
		String f="/home/xtof/softs/android/adt-bundle-linux-x86-20130717/sdk/platform-tools/mygame"+g.getGameID()+".sgf";
		System.out.println("loading sgf "+g.loadSGFLocally(f));
		System.out.println(g.countMovesInSgf());
	}
}
