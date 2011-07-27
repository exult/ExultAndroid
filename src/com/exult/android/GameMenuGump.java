package com.exult.android;

import com.exult.android.Gump.Modal;

/*
 * This is the top-level menu for Black Gate or Serpent Isle.
 */
public class GameMenuGump extends Modal {
	private static final int menuChoices[] = { 0x04, 0x05, 0x08, 0x06, 0x11, 0x12, 0x07 };
	private GumpWidget.Button selected;
	
	public GameMenuGump(ShapeFrame s) {
		super(s);
		int offset = 0, cnt = menuChoices.length;
		int topy = (gwin.getHeight() - 200)/2;
		int centerx = gwin.getWidth()/2;
		int menuy = topy + 120;
		VgaFile menuShapes = game.getMenuShapes();
		for (int i = 0; i < cnt; ++i) {
			ShapeFrame onShape = menuShapes.getShape(menuChoices[i], 1),
					  offShape = menuShapes.getShape(menuChoices[i], 0);
			int width = offShape.getWidth();
			MenuItem entry = new MenuItem(this, i, onShape, offShape, centerx - width/2, menuy + offset);
			addElem(entry);	
			offset += offShape.getYBelow() + 3;
		}
	}
	@Override
	public void close() {
		audio.stopMusic();
		gwin.getPal().fadeOut(EConst.c_fade_out_time);
		super.close();
	}
	// Handle events:
	@Override
	public void onMotion(int mx, int my) {
		GumpWidget.Button item = onButton(mx, my);
		if (item != selected) {
			if (selected != null) {
				selected.setPushed(false);
				selected.paint();
			}
			if (item != null) {
				item.setPushed(true);
				item.paint();
			}
			selected = item;
			gwin.setPainted();
		}
	}
	@Override
	public void onUp(int mx, int my) {
		GumpWidget.Button item = onButton(mx, my);
		if (item != null)
			item.activate(true);
	}
	public void keyDown(int chr) // Key pressed
		{  }
	public void textInput(int chr, int unicode) // Character typed (unicode)
		{ }
	public void handleChoice(int id) {
		switch (id) {
		case 0:	// Intro
			break;
		case 1: // New Game
			break;
		case 2:	// Journey Onwards
			close(); break;
		case 3:	// Credits
			break;
		case 4:	// Quotes
			break;
		case 5:	// End game
			break;
		case 6:	// Return to Exult menu
			break;
		}
	}
	
	public static class MenuItem extends GumpWidget.Button {
		int id;
		public MenuItem(Gump par, int i, ShapeFrame onShape, ShapeFrame offShape, int px, int py) {
			super(par, onShape, offShape, px, py);
			id = i;
		}
		// What to do when 'clicked':
		@Override
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			((GameMenuGump)parent).handleChoice(id);
			return true;
		}
	}
}
