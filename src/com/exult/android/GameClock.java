package com.exult.android;

public class GameClock extends GameSingletons implements TimeSensitive {
	public static final int ticksPerMinute = 25;	// Ticks per game minute.
	private int hour, minute;		// Time (0-23, 0-59).
	private int day;			// Keep track of days played.
	private int lightSourceLevel;		// Last set light source level.
	private int oldLightLevel;		// Last set light source level.
	private boolean oldSpecialLight;		// Last set light source level.
	private boolean oldInfravision;		// If infravision was on last time.
	private boolean oldInvisible;		// If invisibility was on last time.
	private int dungeon;		// Last set 'in_dungeon' value.
	private int overcast;			// >0 if day is overcast (e.g., from a storm).
	private boolean was_overcast;
	private int fog;			// >0 if there is fog.
	private boolean was_foggy;
	//+++++private Palette_transition *transition;	// For smooth palette transitions.
	private int time_rate;
	
	public int getHour()
		{ return hour; }
	public void setHour(int h)
		{ hour = h; }
	public int getMinute()
		{ return minute; }
	public void setMinute(int m)
		{ minute = m; }
	public int getDay()
		{ return day; }
	public void setDay(int d)
		{ day = d; }
	public int getTotalHours()	// Get total # hours.
		{ return day*24 + hour; }
	public int getTotalMinutes()
		{ return getTotalHours()*60 + minute; }
	public void setLightSource(int lev, int dun) {
		if (lev != lightSourceLevel || dun != dungeon)
			setLightSourceLevel(lev);
	}
	private void setLightSourceLevel(int lev) {
		lightSourceLevel = lev;
		setPalette();
	}
	public void setPalette() {
		//+++++++++++++FINISH
	}
	public void reset() {
		overcast = fog = 0;
		was_overcast = was_foggy = false;
		oldSpecialLight = false;
		oldInfravision = false;
		oldInvisible = false;
		dungeon = 255;
		//+++++++++transition = null;
	}
	public void increment(int numMinutes) {
		int oldHour;
		long newMin;
		
		oldHour = hour;		// Remember current 3-hour period.
		numMinutes += 7;		// Round to nearest 15 minutes.
		numMinutes -= numMinutes%15;
		newMin = minute + numMinutes;
		hour += (int) (newMin/60);		// Update hour.
		minute = (int) (newMin%60);
		day += hour/24;			// Update day.
		hour %= 24;
		
		// Update palette to new time.
		setPalette();
		// Check to see if we need to update the NPC schedules.
		/* ++++++++++FINISH
		if (hour != oldHour)		// Update NPC schedules.
			gwin.scheduleNpcs(hour);
		*/
	}
	
	@Override
	public void addedToQueue() {
	}
	@Override
	public boolean alwaysHandle() {
		return false;
	}
	@Override
	public void handleEvent(int ctime, Object udata) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removedFromQueue() {
	}

}
