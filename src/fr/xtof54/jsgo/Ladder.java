package fr.xtof54.jsgo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;

public class Ladder {
	String html;
	String[] userList=null;
	File cacheFile=null;
	String userRank = "unk";

	public void checkCache(File dir) {
		File f = new File(dir+"/ladder.txt");
		cacheFile=f;
		if (!f.exists()) return;
		// read ladder from cache
		try {
			DataInputStream ff = new DataInputStream(new FileInputStream(f));
			int n = ff.readInt();
			userList=new String[n];
			for (int i=0;i<n;i++)
				userList[i] = ff.readUTF();
			ff.close();
			System.out.println("ladder read from cache "+n);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isLadderCached() {
		if (userList!=null) return true;
		else return false;
	}
	
	public String[] getCachedLadder() {
		return userList;
	}
	
	private String decode(String s) {
		try {
			String ss = URLDecoder.decode(s);
			return ss.replace("&nbsp;", "");
		} catch (Exception e) {
			return s.replace("&nbsp;", "");
		}
	}
	
	public void resetCache() {
		userList=null;
	}
	
	private void processHTML() {
		if (html!=null) {
			ArrayList<String> reslist = new ArrayList<String>();
			int i=html.indexOf("Challenge this user");
			while (i>=0) {
				StringBuilder userline = new StringBuilder();
				int debline = html.lastIndexOf("<tr ", i);
				int endline = html.indexOf("</tr", i);
				if (debline>=0&&endline>=0) {
					int z=html.indexOf("name=\"rank",debline);
					if (z>=0) {
						int z1=html.indexOf('>',z)+1;
						int z2=html.indexOf('<',z1);
						userline.append(decode(html.substring(z1,z2)));
						userline.append(' ');
					}
					z=html.indexOf("\" class=\"User",debline);
					if (z>=0) {
						int z1=html.indexOf('>',z)+1;
						int z2=html.indexOf('<',z1);
						userline.append(decode(html.substring(z1,z2)));
						userline.append(' ');
					}
					z=html.indexOf("\" class=\"Rating",debline);
					if (z>=0) {
						int z1=html.indexOf('>',z)+1;
						int z2=html.indexOf('<',z1);
						userline.append(decode(html.substring(z1,z2)));
						userline.append(' ');
					}
					reslist.add(userline.toString().trim());
				}
				int j=html.indexOf("Challenge this user",endline);
				i=j;
			}
			userList = new String[reslist.size()];
			reslist.toArray(userList);
			System.out.println("end ladder to array:");
			for (int a=0;a<15;a++)
				System.out.println("\t"+a+"\t"+userList[a]);
		}
		// get user rank
		int i=html.indexOf("TourneyUser");
		if (i>=0) {
		    int z=html.indexOf("name=\"rank",i);
            if (z>=0) {
                int z1=html.indexOf('>',z)+1;
                int z2=html.indexOf('<',z1);
                userRank=html.substring(z1, z2);
            }
		}
	}
	
	public void setHTML(String s) {
		html=""+s;
		processHTML();
		if (userList==null) {
			System.out.println("ERROR: no ladder processing !");
			return;
		}
		try {
			DataOutputStream ff = new DataOutputStream(new FileOutputStream(cacheFile));
			ff.writeInt(userList.length);
			for (int i=0;i<userList.length;i++)
				ff.writeUTF(userList[i]);
			ff.close();
			System.out.println("ladder saved on cache");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}