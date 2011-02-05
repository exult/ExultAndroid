package com.exult.android.shapeinf;
import com.exult.android.*;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

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
	
	public static class FrameInfo extends BaseInfo 
									implements Comparable<FrameInfo> {
		protected int	frame;		// Frame for which this applies or -1 for any.
		protected int	quality;	// Quality for which this applies or -1 for any.
		private static FrameInfo search = null;
		
		public int getFrame()
			{ return frame; }
		public int getQuality() 
			{ return quality; }
		@Override
		public boolean read(InputStream in, int version, 
							boolean patch, int game, ShapeInfo info) {
			return false;
		}
		@Override
		public int compareTo(FrameInfo i2) {
			int v = frame - i2.frame;
			if (v == 0)
				v = quality - i2.quality;
			return v;
		}
		public static <T extends FrameInfo> void addVectorInfo(T inf, Vector<T> vec) {
			int ind = Collections.binarySearch(vec, inf);
			if (ind < 0)	// Not found?
				vec.insertElementAt(inf, -ind + 1);
			else			// Found, so replace.
				vec.setElementAt(inf, ind);
		}
		public static <T extends FrameInfo> T searchDoubleWildCards(Vector<T> vec,
												int frame, int quality) {
			T found;
			if (search == null)
				search = new FrameInfo();
			search.frame = frame; search.quality = quality;
			int ind = Collections.binarySearch(vec, search);
			if (ind >= 0) {
				found = vec.elementAt(ind);
				if (!found.isInvalid())
					return found;
			}
			int sz = vec.size();
			if (quality != -1) {
				ind = -ind + 1;
				if (ind < sz) {
					found = vec.elementAt(-ind + 1);
					if (found.frame == frame) {
						// Maybe quality is to blame. Try wildcard qual.
						search.quality = -1;
						ind = Collections.binarySearch(vec, search);
						if (ind >= 0 && !vec.elementAt(ind).isInvalid())
							return vec.elementAt(ind);
					}
				}
				// Maybe frame is to blame? Try search for specific
				// quality with wildcard frame.
				search.quality = quality;
				search.frame = -1;
				ind = Collections.binarySearch(vec, search);
				if (ind >= 0 && !vec.elementAt(ind).isInvalid())
					return vec.elementAt(ind);
			}
			// *Still* haven't found it. Last try: wildcard frame *and* quality.
			search.frame = search.quality = -1;
			ind = Collections.binarySearch(vec, search);
			if (ind >= 0 && !vec.elementAt(ind).isInvalid())
				return vec.elementAt(ind);
			return null;
		}
	}
}
