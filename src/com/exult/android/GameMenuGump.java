package com.exult.android;

import com.exult.android.Gump.Modal;

/*
 * This is the top-level menu for Black Gate or Serpent Isle.
 */
public class GameMenuGump extends Modal {
	private static final int menuChoices[] = { 0x04, 0x05, 0x08, 0x06, 0x11, 0x12, 0x07 };
	
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
			MenuItem entry = new MenuItem(this, onShape, offShape, centerx - width/2, menuy + offset);
			addElem(entry);	
			offset += offShape.getYBelow() + 3;
		}
	}
	// Handle events:
	public void onUp(int mx, int my) {
		audio.stopMusic();
		gwin.getPal().fadeOut(EConst.c_fade_out_time);
		close();//++++++TESTING
	}
	public void keyDown(int chr) // Key pressed
		{  }
	public void textInput(int chr, int unicode) // Character typed (unicode)
		{ }
	public static class MenuItem extends GumpWidget.Button {
		public MenuItem(Gump par, ShapeFrame onShape, ShapeFrame offShape, int px, int py) {
			super(par, onShape, offShape, px, py);
		}
		// What to do when 'clicked':
		@Override
		public boolean activate(boolean button) {
			//++++TODO
			return true;
		}
	}
}
