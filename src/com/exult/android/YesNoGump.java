package com.exult.android;
import java.util.Observable;
import java.util.Observer;
import android.view.KeyEvent;

public class YesNoGump extends Gump.Modal {
	private static final short yesx = 63, yesnoy = 45, nox = 84;	// Coords. of the buttons.
	private static final short areax = 6, areay = 5, areaw = 116, areah = 32;
	private Reporter reporter;
	private String text;			// Text of question. 
	private int font;				// Font #.
	private boolean answer;			// 1 for yes, 0 for no.
	public void setAnswer(boolean y) {		// Done from 'yes'/'no' button.
		answer = y;
		reporter.setChanged();
		reporter.notifyObservers(this);
		close();
	}
	private static class Reporter extends Observable {
		public void setChanged() { super.setChanged(); }
	}
	private void init(String txt, String fontname) {
		text = txt;
		font = 2;	// +++++FOR NOW.  Lookup fontname.  Default is "SMALL_BLACK_FONT".
	}
	private YesNoGump(String txt) {
		
		super(game.getShape("gumps/yesnobox"));
		init(txt, null);	
		addElem(new YesNoButton(this, yesx, yesnoy, true));
		addElem(new YesNoButton(this, nox, yesnoy, false));
		reporter = new Reporter();
	}
	boolean getAnswer()
		{ return answer; }
					// Paint it and its contents.
	public void paint() {	
		super.paint();
		// Paint text.
		fonts.paintTextBox(gwin.getWin(), font, text, 
				x + areax, y + areay, areaw, areah, 2, false, false);
		gwin.setPainted();
	}
	//	Clicktracker
	@Override
	public void onDown(int x, int y) { 
		mouseDown(x, y, 1);
	}
					// Handle events:
	public boolean mouseDown(int mx, int my, int button) {
		if (button != 1) 
			return false;
		pushed = onButton(mx, my);
		if (pushed != null)
			pushed.push(true);		// Show it.
		return true;
	}
	public boolean mouseUp(int mx, int my, int button) {
		if (button != 1) 
			return false;
		if (pushed != null) {			// Pushing a button?
			pushed.unpush(true);
			if (pushed.onButton(mx, my) != null)
				pushed.activate(true);
			pushed = null;
		}
		return true;
	}
	public void keyDown(int chr) { // Character typed.
		if (chr == 'y' || chr == 'Y' || chr == KeyEvent.KEYCODE_ENTER)
			setAnswer(true);
		else if (chr == 'n' || chr == 'N' || chr == KeyEvent.KEYCODE_BACK)
			setAnswer(false);
	}
	public static void ask(Observer c, String txt) {	// Ask question, get answer.
		YesNoGump g = new YesNoGump(txt);
		g.reporter.addObserver(c);
		g.track();
	}
	/*
	 *	A 'yes' or 'no' button.
	 */
	private class YesNoButton extends GumpWidget.Button	{
		boolean isyes;			// 1 for 'yes', 0 for 'no'.
		public YesNoButton(Gump par, int px, int py, boolean yes) {
			super(par, yes ? 
				game.getShape("gumps/yesbtn") 
				: game.getShape("gumps/nobtn"), px, py);
			isyes = yes;
		}
						// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			((YesNoGump) parent).setAnswer(isyes);
			return true;
		}
	};
}
