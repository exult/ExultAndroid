package com.exult.android;

public class CombatSchedule extends Schedule {
	//	Combat options:
	public static boolean combatTrace;
	private static boolean paused;		// For suspending.
	public static int difficulty;		// 0=normal, >0 harder, <0 easier.
	public static final int // enum Mode
			original = 0,		// All automatic,
			keypause = 1;		// Kbd (space) suspends/resumes.
	public static int mode = original;
	public static boolean showHits;		// Display #'s.
						// In game:
	/* MAYBE FINISH
	public static void togglePause();	// Pause/resume.
	public static void resume()		// Always resume.
	*/
	public static boolean isPaused()
			{ return paused; }
	/*
	 * The schedule data and methods.
	 */
	public CombatSchedule(Actor n, int prev_schedule) {
		super(n);
		//+++++++++FINISH
	}
	@Override
	public void nowWhat() {
		// TODO Auto-generated method stub

	}

}
