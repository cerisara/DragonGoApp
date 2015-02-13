package fr.xtof54.sgfsearch;

import java.io.BufferedReader;
import java.io.FileReader;

public class SgfLoad {
	public static void load(String n) throws Exception {
		GoFrame bi = new GoFrame("toto");
		Board goban = new Board(19, bi);
		BufferedReader f = new BufferedReader(new FileReader(n));
		goban.load(f);
		f.close();
	}
	
	public static void main(String args[]) throws Exception {
		load("/home/xtof/xtofnath.sgf");
	}
}
