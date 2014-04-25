package fr.xtof54.jsgo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;

import fr.xtof54.jsgo.EventManager.eventType;

public class Ladder {
	String html;
	String[] userList=null, ridList;
	File cacheFile=null;
	String userRank = "unk";
	int lastClicked = -1;
    // year, month, day, hour, minutes
	int[] cacheTime = {0,0,0,0,0};

	public CharSequence getCacheTime(){
	    return cacheTime[0]+"-"+cacheTime[1]+"-"+cacheTime[2]+"."+cacheTime[3]+":"+cacheTime[4];
	}
	
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
			ridList=new String[n];
            for (int i=0;i<n;i++)
                ridList[i] = ff.readUTF();
            userRank = ff.readUTF();
            for (int i=0;i<5;i++) cacheTime[i]=ff.readInt();
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
	
	@Deprecated
	private void processHTML() {
		if (html!=null) {
			ArrayList<String> reslist = new ArrayList<String>();
			ArrayList<String> rids = new ArrayList<String>();
			int i=html.indexOf("Challenge this user");
			while (i>=0) {
			    String rid = null;
			    int j=html.lastIndexOf("rid=", i);
			    if (j>=0) {
			        j+=4;
			        int z=html.indexOf('"',j);
			        if (z-j>0) rid = html.substring(j, z);
			    }
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
					rids.add(rid);
				}
				j=html.indexOf("Challenge this user",endline);
				i=j;
			}
			userList = new String[reslist.size()];
			reslist.toArray(userList);
            ridList  = new String[reslist.size()];
            rids.toArray(ridList);
			System.out.println("end ladder to array: "+ridList.length+" "+userList.length);
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

	ArrayList<String> reslist = new ArrayList<String>();
	ArrayList<String> rids = new ArrayList<String>();
	private void treatLine(String s) {
		{
			int i=s.indexOf("TourneyUser");
			if (i>=0) {
			    int z=s.indexOf("name=\"rank",i);
	            if (z>=0) {
	                int z1=s.indexOf('>',z)+1;
	                int z2=s.indexOf('<',z1);
	                userRank=s.substring(z1, z2);
	            }
	            return;
			}
		}
		
		int i=s.indexOf("Challenge this user");
		if (i<0) return;
	    String rid = null;
	    int j=s.lastIndexOf("rid=", i);
	    if (j>=0) {
	        j+=4;
	        int z=s.indexOf('"',j);
	        if (z-j>0) rid = s.substring(j, z);
	    }
		StringBuilder userline = new StringBuilder();
	    int z=s.indexOf("name=\"rank");
		if (z>=0) {
			int z1=s.indexOf('>',z)+1;
			int z2=s.indexOf('<',z1);
			userline.append(decode(s.substring(z1,z2)));
			userline.append(' ');
		}
		
		z=s.indexOf("\" class=\"User");
		if (z>=0) {
			int z1=s.indexOf('>',z)+1;
			int z2=s.indexOf('<',z1);
			userline.append(decode(s.substring(z1,z2)));
			userline.append(' ');
		}
		z=s.indexOf("\" class=\"Rating");
		if (z>=0) {
			int z1=s.indexOf('>',z)+1;
			int z2=s.indexOf('<',z1);
			userline.append(decode(s.substring(z1,z2)));
			userline.append(' ');
		}
		reslist.add(userline.toString().trim());
		rids.add(rid);
	}

	// process HTML in a stream fashion to use less memory
	public void loadHTML(String file) {
		reslist.clear();
		rids.clear();
		try {
			BufferedReader f = new BufferedReader(new FileReader(file));
			String firsthalf = null;
			for (;;) {
				String s=f.readLine();
				if (s==null) break;
				int i=0,k;
				// loop until all "full lines" in s are captured and handled
				for (;;) {
					// get a full line
					if (firsthalf!=null) {
						// we already have the first half, we look at the end of line
						k=s.indexOf("</tr");
						if (k<0) {
							firsthalf+=s;
							break;
						} else {
							firsthalf+=s.substring(0, k);
							treatLine(firsthalf);
							firsthalf=null;
						}
						i=k;
						continue;
					}
					int j=s.indexOf("<tr ",i);
					if (j<0) {
						k=0; break;
					}
					k=s.indexOf("</tr",j);
					if (k<0) {
						// there is the beginning but not the end
						firsthalf=s.substring(j);
						break;
					} else {
						treatLine(s.substring(j, k));
					}
					i=k;
				}
				if (k==0) {
					// no beginning in the current string; we can start over with the next s
				} else if (k<0) {
					// only the first half; we have to complete
				}
			}
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		userList = new String[reslist.size()];
		reslist.toArray(userList);
        ridList  = new String[reslist.size()];
        rids.toArray(ridList);
		System.out.println("end ladder to array: "+ridList.length+" "+userList.length);
		if (userList.length==0) {
			EventManager.getEventManager().sendEvent(eventType.showMessage, "you cannot challenge anymore in this ladder");
		} else
			for (int a=0;a<15;a++)
				System.out.println("\t"+a+"\t"+userList[a]);
	}
	
	private void saveList() {
		if (userList==null) {
			System.out.println("ERROR: no ladder processing !");
			return;
		}
		try {
			DataOutputStream ff = new DataOutputStream(new FileOutputStream(cacheFile));
			ff.writeInt(userList.length);
            for (int i=0;i<userList.length;i++)
                ff.writeUTF(userList[i]);
            for (int i=0;i<userList.length;i++)
                ff.writeUTF(ridList[i]);
            ff.writeUTF(userRank);
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            int hour = c.get(Calendar.HOUR);
            int min = c.get(Calendar.MINUTE);
            ff.writeInt(year);
            cacheTime[0]=year;
            ff.writeInt(month);
            cacheTime[1]=month;
            ff.writeInt(day);
            cacheTime[2]=day;
            ff.writeInt(hour);
            cacheTime[3]=hour;
            ff.writeInt(min);
            cacheTime[4]=min;
			ff.close();
			System.out.println("ladder saved on cache");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	// this method should not be used any more because it consumes too much memory
	public void setHTML(String s) {
		html=""+s;
		processHTML();
		saveList();
	}
}