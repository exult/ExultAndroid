package com.exult.android;

public final class Cheat extends GameSingletons {
	private boolean hackMover;
	public boolean inHackMover() {
		return hackMover;
	}
	public void toggleHackMover() {
		hackMover = !hackMover;
		ExultActivity.showToast(hackMover?"HackMover Mode":"Ending HackMover");
	}
}
