package com.exult.android.shapeinf;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;
import com.exult.android.*;

public class AmmoInfo  extends BaseInfo implements DataUtils.ReaderFunctor {
	@Override
	public boolean read(InputStream in, int version, 
			boolean patch, int game, ShapeInfo info) {
		return false;//+++++++++FINISH
	}
}
