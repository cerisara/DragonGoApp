package rene.util.mail;

/**
A MailCallback is called when the mail is sent, or if there are
problems.
*/

public interface MailCallback
{	/**
	@param flag Mail was sent successfully?
	@param s A description of the error or state
	*/
	void result (boolean flag, String s);
}
