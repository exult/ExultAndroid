package com.exult.android.shapeinf;
import com.exult.android.*;
import java.io.PushbackInputStream;
import java.io.InputStream;
import com.exult.android.ShapeInfo;

public class ExplosionInfo extends BaseInfo {
	private int		sprite;			// Explosion sprite.
	private int		sfxnum;			// SFX to play or 255 for none.
	public int getSprite()
		{ return sprite; }
	public int getSfx()
		{ return sfxnum; }
	public static int getInfoFlag()
		{ return 0x10; }
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		PushbackInputStream txtin = (PushbackInputStream) in;
		sprite = EUtil.ReadInt(txtin);
		if (sprite == -0xff) {	// means delete entry.
			setInvalid(true);
			return true;
		}
		sfxnum = EUtil.ReadInt(txtin, -1);
		if (sfxnum == 255)
			sfxnum = -1;
		return true;
	}

}
