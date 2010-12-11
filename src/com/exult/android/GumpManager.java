package com.exult.android;
import java.util.LinkedList;
import java.util.ListIterator;

public class GumpManager extends GameSingletons {
	private LinkedList<Gump> openGumps;
	
	public GumpManager() {
		openGumps = new LinkedList<Gump>();
	}
	public void addGump(Gump g) {
		openGumps.addLast(g);
	}
	public void addGump(GameObject obj, int shapenum, boolean actorgump) {
		//+++++++++++FINISH
	}
	public boolean closeGump(Gump g) {
		return g == null ? false : openGumps.remove(g);
	}
	public void paint() {
		ListIterator<Gump> iter = openGumps.listIterator();
		while (iter.hasNext()) {
			Gump g = iter.next();
			g.paint();
		}
	}
}
