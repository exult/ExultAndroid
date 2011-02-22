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
	private boolean wasOvercast;
	private int fog;			// >0 if there is fog.
	private boolean wasFoggy;
	private int timeQueueCount;
	private Palette.Transition transition;	// For smooth palette transitions.
	private int timeRate;
	private static int getTimePalette(int hour, boolean dungeon) {
		if (dungeon || hour < 6)
			return Palette.PALETTE_NIGHT;
		else if (hour == 6)
			return Palette.PALETTE_DAWN;
		else if (hour == 7)
			return Palette.PALETTE_DAY;
		else if (hour < 20)
			return Palette.PALETTE_DAY;
		else if (hour == 20)
			return Palette.PALETTE_DUSK;
		else
			return Palette.PALETTE_NIGHT;
	}
	/* UNUSED
	private static boolean isLightPalette(int pal) {
		return (pal == Palette.PALETTE_SINGLE_LIGHT || pal == Palette.PALETTE_MANY_LIGHTS);
	}*/
	private static boolean isDarkPalette(int pal) {
		return (pal == Palette.PALETTE_DUSK || pal == Palette.PALETTE_NIGHT);
	}
	/* UNUSED
	private static boolean isWeatherPalette(int pal) {
		return (pal == Palette.PALETTE_OVERCAST || pal == Palette.PALETTE_FOG);
	}*/
	private static boolean isDayPalette(int pal) {
		return (pal == Palette.PALETTE_DAWN || pal == Palette.PALETTE_DAY);
	}
	private static int getFinalPalette(int pal, boolean cloudy, boolean foggy,
									int light, boolean special) {
		if ((light != 0 || special) && isDarkPalette(pal)) {
			int light_palette = Palette.PALETTE_SINGLE_LIGHT;
						// Gump mode, or light spell?
			if (special || (light > 1))
				light_palette = Palette.PALETTE_MANY_LIGHTS;
			return light_palette;
		} else if (isDayPalette(pal)) {
			if (foggy)
				return Palette.PALETTE_FOG;
			else if (cloudy)
				return Palette.PALETTE_OVERCAST;
		}
		return pal;
	}

	public GameClock() {
		hour = 6;
		dungeon = 255;
		timeRate = 1;
	}
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
		Actor main_actor = gwin.getMainActor();
		boolean invis = main_actor != null && main_actor.getFlag(GameObject.invisible);
		if (invis && !oldInvisible) {
			transition = null;
			gwin.getPal().set(Palette.PALETTE_INVISIBLE);
			if (!gwin.getPal().isFadedOut())
				gwin.getPal().apply();
			return;
		}
		oldInvisible = invis;

		if (main_actor == null || cheat.inInfravision() && !oldInfravision) {
			transition = null;
			gwin.getPal().set(Palette.PALETTE_DAY);
			if (!gwin.getPal().isFadedOut())
				gwin.getPal().apply();
			return;
		}
		oldInfravision = cheat.inInfravision();

		int new_dungeon = gwin.isInDungeon();
		int new_palette = getTimePalette(hour+1, new_dungeon != 0),
		    old_palette = getTimePalette(hour, (dungeon!=255 ? dungeon : new_dungeon) != 0);
		boolean cloudy = overcast > 0;
		boolean foggy = fog > 0;
		boolean weather_change = (cloudy != wasOvercast) || (foggy != wasFoggy);
		boolean light_sensitive = isDarkPalette(new_palette) &&
					isDarkPalette(old_palette);
		boolean light_change = light_sensitive &&
					((lightSourceLevel != oldLightLevel) ||
					 (gwin.isSpecialLight() != oldSpecialLight));

		new_palette = getFinalPalette(new_palette, cloudy, foggy,
					lightSourceLevel, gwin.isSpecialLight());
		old_palette = getFinalPalette(old_palette, wasOvercast, wasFoggy,
					oldLightLevel, oldSpecialLight);

		if (gwin.getPal().isFadedOut()) {
			transition = null;
			gwin.getPal().set(old_palette);
			if (!gwin.getPal().isFadedOut()) {
				gwin.getPal().apply();
				gwin.setAllDirty();
			}
			return;
		}
		wasOvercast = cloudy;
		wasFoggy = foggy;
		oldLightLevel = lightSourceLevel;
		oldSpecialLight = gwin.isSpecialLight();
		dungeon = new_dungeon;

		if (weather_change) {
				// TODO: Maybe implement smoother transition from
				// weather to/from dawn/sunrise/sundown/dusk.
				// Right now, it works like the original.
			transition = new Palette.Transition(old_palette, new_palette,
								hour, minute, 1, 4, hour, minute);
			return;
		} else if (light_change) {
			transition = null;
			gwin.getPal().set(new_palette);
			if (!gwin.getPal().isFadedOut()) {
				gwin.getPal().apply();
				gwin.setAllDirty();
			}
			return;
		}
		if (transition != null) {
			if (transition.setStep(hour, minute))
				return;
			transition = null;
		}
		if (old_palette != new_palette) {	// Do we have a transition?
			transition = new Palette.Transition(old_palette, new_palette,
								hour, minute, 4, 15, hour, 0);
			return;
		}
		gwin.getPal().set(new_palette);
		if (!gwin.getPal().isFadedOut()) {
			gwin.getPal().apply();
			gwin.setAllDirty();
		}
	}
	public void reset() {
		overcast = fog = 0;
		wasOvercast = wasFoggy = false;
		oldSpecialLight = false;
		oldInfravision = false;
		oldInvisible = false;
		dungeon = 255;
		transition = null;
	}
	// Start end cloud cover.
	public void setOvercast(boolean onoff) {
		overcast += (onoff ? 1 : -1);
		setPalette();		// Update palette.
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
		if (hour != oldHour)		// Update NPC schedules.
			gwin.scheduleNpcs(hour);
	}
	
	@Override
	public void addedToQueue() {
		++timeQueueCount;
	}
	@Override
	public boolean alwaysHandle() {
		return false;
	}
	@Override
	public void handleEvent(int ctime, Object udata) {
		// TODO Auto-generated method stub
		int minOld = minute;
		int hourOld = hour;
					// Time stopped?  Don't advance.
		if (gwin.isTimeStopped() == 0) {
			minute += timeRate;
			// ++++ TESTING
			// if (Game::get_game_type() == SERPENT_ISLE)
				//+++++FINISH Check_freezing();
		}
		while (minute >= 60) {	// advance to the correct hour (and day)
			minute -= 60;
			if (++hour >= 24) {
				hour -= 24;
				day++;
			}
			gwin.mendNpcs();	// Restore HP's each hour.
			checkHunger();		// Use food, and print complaints.
			gwin.scheduleNpcs(hour);
		}
		if (transition != null && !transition.setStep(hour, minute)) {
			transition = null;
			setPalette();
		} else  if (hour != hourOld)
			setPalette();

		if ((hour != hourOld) || (minute/15 != minOld/15))
			System.out.println("Clock updated to " + hour + ':' + minute);
		ctime += ticksPerMinute;
		tqueue.add(ctime, this, udata);
	}
	/*
	 *	Fake an update to the next 3-hour period.
	 */

	public void fakeNextPeriod() {
		minute = 0;
		hour = ((hour/3 + 1)*3);
		day += hour/24;			// Update day.
		hour %= 24;
		setPalette();
		checkHunger();
		gwin.scheduleNpcs(hour);
		gwin.mendNpcs();		// Just do it once, cheater.
		ExultActivity.showToast("The hour is now " + hour);
	}
	public void checkHunger() {
		gwin.getMainActor().useFood();
		int cnt = partyman.getCount();
		for (int i = 0; i < cnt; ++i)
			gwin.getNpc(partyman.getMember(i)).useFood();
	}
	@Override
	public void removedFromQueue() {
		--timeQueueCount;
	}
	public boolean inQueue() {
		return timeQueueCount > 0;
	}

}
