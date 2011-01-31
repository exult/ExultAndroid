package com.exult.android.shapeinf;
import com.exult.android.*;
import java.io.InputStream;

public abstract class BaseInfo {
	public final static int //	Flags.
	nfo_modified = 1,
	From_patch = 2,
	Have_static = 4,
	Is_invalid = 8;
	protected int infoFlags;	
	protected void setFlag(boolean tf, int flag)
		{
		if (tf)
			infoFlags |= flag;
		else
			infoFlags &= (~flag);
		}
	public boolean isInvalid() {
		return (infoFlags&Is_invalid) != 0;
	}
	public void setInvalid(boolean tf) {
		setFlag(tf, Is_invalid); 
	}
	public abstract boolean read(InputStream in, int version, 
		boolean patch, int game, ShapeInfo info);	
}
