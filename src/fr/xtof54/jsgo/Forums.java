package fr.xtof54.jsgo;

/**
 * Main page, once logged-in:
 * http://www.dragongoserver.net/forum/index.php
 * In this page, look for lines with the string 'class="NewFlag"'. These lines points to cats with unread messages.
 * The first href in this line points to he list of topics in this cat, again with the NewFlag tag (I guess)
 * Again, one can use the first href in the line to go to the target thread with new messages.
 * 
 * In this last page, there is a first table with the titles of the posts, and a second table with the content.
 * Maybe it's good to skip the first table and directly jumps to the second table by
 * looking the table after 'Reading thread posts', and then prints all messages in revers order, but still
 * marking the new ones in colored background thanks to the 'class="NewFlag"' in the line
 * 
 * @author xtof
 *
 */
public class Forums {

}
