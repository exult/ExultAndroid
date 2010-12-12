package com.exult.android;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Vector;

public final class DataUtils {
	/*
	 *	Get # entries of binary data file (with Exult extension).
	 */
	private static int Read_count(InputStream in) {
		int cnt = EUtil.Read1(in);	// How the originals did it.
		if (cnt == 255)
			cnt = EUtil.Read2(in);	// Exult extension.
		return cnt;
	}
	/*
	 *	Generic base data-agnostic reader class.
	 */
	public static class BaseReader {
		protected boolean haveversion;
		protected void read_data(InputStream in, int index, int version,
				boolean patch, int game, boolean binary)
			{  }
			// Binary data file.
		protected void read_binary_internal(InputStream in, boolean patch, 
				int game) {
			int vers = 0;
			if (haveversion)
				vers = EUtil.Read1(in);
			int cnt = Read_count(in);
			for (int j = 0; j < cnt; j++)
				read_data(in, j, vers, patch, game, true);
			}
		public BaseReader(boolean h) {
			haveversion = h;
		}
		void parse(Vector<byte[]> strings, int version, 
							boolean patch, int game) {
			for (int j = 0; j<strings.size(); j++) {
				byte ptr[] = strings.elementAt(j);
				if (ptr == null)
					continue;
				DataInputStream strin = new DataInputStream(
						new ByteArrayInputStream(ptr));
				read_data(strin, j, version, patch, game, false);
			}
			strings.clear();
		}
			// Binary data file.
		void read(String fname, boolean patch, int game) {
			InputStream fin;
			try {
				fin = EUtil.U7openStream(fname);
				read_binary_internal(fin, patch, game);
				fin.close();
			} catch (IOException e) {
				return;
			}
		}
			// Binary resource file.
		void read(int game, int resource) {
			// Only for BG and SI.
			if (game != EConst.BLACK_GATE && game != EConst.SERPENT_ISLE)
				return;
			boolean bg = game == EConst.BLACK_GATE;
			String flexfile =
					bg ? EFile.EXULT_BG_FLX : EFile.EXULT_SI_FLX;
			byte[] txt = EFileManager.instanceOf().retrieve(
					flexfile, resource);
			InputStream strin = new ByteArrayInputStream(txt);
			read_binary_internal(strin, false, game);
		}
	}

	public static class IDReaderFunctor {
		public int read(InputStream in, int index, int version, 
				boolean binary)
			{ return binary ? EUtil.Read2(in) : EUtil.ReadInt((DataInputStream)in); }
	}
	public abstract static class ReaderFunctor {
		public abstract boolean read(InputStream in, int version, 
				boolean patch, int game, ShapeInfo info);
	}
	public static class PostFunctor {
		void read(InputStream in, int version, boolean patch,
				int game, ShapeInfo info) {}
	}

	/*
	 *	Generic functor-based reader class for maps.
	 */
	public static class FunctorMultidataReader extends BaseReader {
		ShapeInfo info[];
		ReaderFunctor reader;
		PostFunctor postread;
		IDReaderFunctor idread;
		public void read_data(InputStream in, int index, int version,
			boolean patch, int game, boolean binary) {
			int id = idread.read(in, index, version, binary);
			if (id >= 0) {
				ShapeInfo inf = info[id];
				reader.read(in, version, patch, game, inf);
				postread.read(in, version, patch, game, inf);
			}
		}
		public FunctorMultidataReader(ShapeInfo nfo[],
				ReaderFunctor rd, PostFunctor post, IDReaderFunctor idrd, 
									boolean hvers) {
			super(hvers);
			info = nfo;
			reader = rd;
			postread = post;
			idread = idrd;
		}
	}

	
}
