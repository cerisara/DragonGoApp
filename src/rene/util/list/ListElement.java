package rene.util.list;

/**
The nodes of a list.
@see rene.list.ListClass
*/

public class ListElement
// A list node with pointers to previous and next element
// and with a content of type Object.
{	ListElement Next,Previous; // the chain pointers
	Object Content; // the content of the node
	ListClass L; // Belongs to this list
	
	public ListElement (Object content)
	// get a new Element with the content and null pointers
	{	Content=content;
		Next=Previous=null;
		L=null;
	}

	// access methods:
	public Object content ()
	{	return Content;
	}
	public ListElement next () { return Next; }
	public ListElement previous () { return Previous; }
	public void list (ListClass l) { L=l; }

	// modifying methods:
	public void content (Object o) { Content=o; }
	public void next (ListElement o) { Next=o; }
	public void previous (ListElement o) { Previous=o; }
	public ListClass list () { return L; }
}


