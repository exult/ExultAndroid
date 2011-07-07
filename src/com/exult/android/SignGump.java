package com.exult.android;

public class SignGump extends Gump.Modal {
	protected Rectangle textArea = new Rectangle();
	private String lines[];
	private boolean serpentine;
	public SignGump(int shapenum, int nlines) {
		super(shapenum);
		lines = new String[nlines];// THIS IS A HACK, but don't ask me why this is like this,
		if (game.isSI() && shapenum==49) {
			// check for avatar read here
			Actor avatar = gwin.getMainActor();
			if (!avatar.getFlag(GameObject.read))
				serpentine = true;
			shapenum = game.getShape("gumps/goldsign");
			initShape(shapenum, null);
			setPos();	// Recenter
		}
		if(shapenum==game.getShape("gumps/woodsign"))
		{
			textArea.set(0, 4, 196, 92);
		}
		else if(shapenum==game.getShape("gumps/tombstone"))
		{
			textArea.set(0, 8, 200, 112);
		}
		else if(shapenum==game.getShape("gumps/goldsign"))
		{
			if (game.isBG())
				textArea.set(0, 4, 232, 96);
			else			// SI
				textArea.set(4, 4, 312, 96);
		}
		else if (shapenum==game.getShape("gumps/scroll"))
			textArea.set(48, 30, 146, 118);
	}
	public void addText(int line, String txt) {	
		if (line < 0 || line >= lines.length)
			return;
		// check for avatar read here
		Actor avatar = gwin.getMainActor();
		if (!serpentine && avatar.getFlag(GameObject.read)) {
			for (int i = 0; i < txt.length(); i++) {
				if (txt.charAt(i) == 40) {
				lines[line] += 'T';
				lines[line] += 'H';
				} else if (txt.charAt(i) == 41) {
					lines[line] += 'E';
					lines[line] += 'E';
				} else if (txt.charAt(i) == 42) {
					lines[line] += 'N';
					lines[line] += 'G';
				} else if (txt.charAt(i) == 43) {
					lines[line] += 'E';
					lines[line] += 'A';
				} else if (txt.charAt(i) == 44) {
					lines[line] += 'S';
					lines[line] += 'T';
				} else if (txt.charAt(i) == '|') {
					lines[line] += ' ';
				} else if (txt.charAt(i) >= 'a')
					lines[line] += txt.charAt(i) - 32;
				else
					lines[line] += txt.charAt(i);
			}
		} else {
			lines[line] = txt;
		}
	}
	public void paint() {
		int font = 1;			// Normal runes.
		if (getShapeNum() == game.getShape("gumps/goldsign")) {
			if (serpentine)
				font = 10;
			else
				font = 6;		// Embossed.
		}
		else if (serpentine)
			font = 8;
		else if (getShapeNum() == game.getShape("gumps/tombstone"))
			font = 3;
						// Get height of 1 line.
		int lheight = fonts.getTextHeight(font);
						// Get space between lines.
		int num_lines = lines.length;
		int lspace = (textArea.h - num_lines*lheight)/(num_lines + 1);
						// Paint the gump itself.
		super.paint();
		int ypos = y + textArea.y;	// Where to paint next line.
		for (int i = 0; i < num_lines; i++) {
			ypos += lspace;
			if (lines[i] == null)
				continue;
			fonts.paintText(font, lines[i],
				x + textArea.x + 
					(textArea.w - 
				fonts.getTextWidth(font, lines[i]))/2,
				ypos);
			ypos += lheight;
		}
		gwin.setPainted();
	}
}
