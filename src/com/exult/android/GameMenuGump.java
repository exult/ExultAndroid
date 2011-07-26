package com.exult.android;

import com.exult.android.Gump.Modal;

/*
 * This is the top-level menu for Black Gate or Serpent Isle.
 */
public class GameMenuGump extends Modal {
	private static final int menuChoices[] = { 0x04, 0x05, 0x08, 0x06, 0x11, 0x12, 0x07 };
	
	public GameMenuGump(ShapeFrame s) {
		super(s);
	}
}
