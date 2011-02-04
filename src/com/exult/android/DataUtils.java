package com.exult.android;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.PushbackInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Vector;

public final class DataUtils {
	/*
	 *	Get # entries of binary data file (with Exult extension).
	 */
	public static int ReadCount(InputStream in) {
		int cnt = EUtil.Read1(in);	// How the originals did it.
		if (cnt == 255)
			cnt = EUtil.Read2(in);	// Exult extension.
		return cnt;
	}
	public static int readBitFlags(PushbackInputStream in, int cnt) {
		int bit = 0;
		int flags = 0;
		while (bit < cnt) {
			int val = EUtil.ReadInt(in, -1);
			if (val == -1)
				break;
			else if (val != 0)
				flags |= (1 << bit);
			else
				flags &= ~(1 << bit);
			bit++;
			}
		return flags;
	}
	/*
	 *	Reads text data file and parses it according to passed
	 *	parser functions.
	 */
	public static void readTextDataFile
		(
		String fname,			// Name of file to read, sans extension
		BaseReader parsers[],			// What to use to parse data.
		String sections[],		// The names of the sections
		int game,
		int resource
		) throws IOException {
		int numsections = sections.length;
		int static_version = 1;
		int patch_version = 1;
		Vector<Vector<byte[]>> static_strings = new Vector<Vector<byte[]>>();
		Vector<Vector<byte[]>> patch_strings = new Vector<Vector<byte[]>>();
		if (game == EConst.BLACK_GATE || game == EConst.SERPENT_ISLE) {
			boolean bg = game == EConst.BLACK_GATE;
			String flexfile = bg ? EFile.EXULT_BG_FLX : EFile.EXULT_SI_FLX;
			EFile txtobj = EFileManager.instanceOf().getFileObject(flexfile);
			byte txt[] = txtobj.retrieve(resource);
			InputStream ds = new ByteArrayInputStream(txt);
			static_version = readTextMsgFileSections(ds,
					static_strings, sections);
			txtobj.close();
		} /* +++++++++FINISH
		else
			{
			try
				{
				snprintf(buf, 50, "<STATIC>/%s.txt", fname);
				std::ifstream in;
				U7open(in, buf, false);
				static_version = readTextMsgFileSections(in,
						static_strings, sections, numsections);
				in.close();
				}
			catch (std::exception &e)
				{
				if (!editing)
					throw e;
				static_strings.resize(numsections);
				}
		} */
		patch_strings.setSize(numsections);
		String buf = String.format("<PATCH>/%1$s.txt", fname);
		if (EUtil.U7exists(buf) != null) {
			InputStream in = EUtil.U7openStream(buf);
			patch_version = readTextMsgFileSections(in, patch_strings, sections);
			in.close();
		}
		for (int i = 0; i < numsections; i++) {
			System.out.println("readTextDataFile: parsing " + sections[i]);
			Vector<byte[]> data = static_strings.elementAt(i);
			parsers[i].parse(data, static_version, false, game);
			data = patch_strings.elementAt(i);
			if (data != null)
				parsers[i].parse(data, patch_version, true, game);
		}
		static_strings.clear();
		patch_strings.clear();
	}
	public static int readTextMsgFileSections
		(
		InputStream instream,
		Vector<Vector<byte[]> > strings,	// Strings returned here
		String sections[]			// Section names
		) throws IOException {
		int numsections = sections.length;
		strings.setSize(numsections);
		int version = 1;
		
		DataInputStream textin = new DataInputStream(instream);
        BufferedReader in = new BufferedReader(new InputStreamReader(textin));
		Vector<byte[]> versioninfo;
		in.mark(1000000);
		// Read version.
		final String versionstr = "version";
		if (searchTextMsgSection(in, versionstr) && 
				(versioninfo = readTextMsgFile(in, null)) != null) {
			byte s[] = versioninfo.elementAt(0);
			int ind, strlen = s.length;
			for (ind = 0; ind < strlen && Character.isDigit(s[ind]); ++ind)
				;
			if (ind > 0)
				version = Integer.parseInt(new String(s, 0, ind));
		}
		for (int i = 0; i < numsections; i++) {
			in.reset();
			if (!searchTextMsgSection(in, sections[i]))
				continue;
			System.out.println("Calling readTextMsgFile for " + sections[i]);
			strings.setElementAt(readTextMsgFile(in, null), i);
		}
	return version;
	}
	/*
	 *	Searches for the start of section in a text msg file.
	 *	Returns true if section is found. The data source will
	 *	be just after the %%section line.
	 */
	private static boolean searchTextMsgSection(BufferedReader in, String section)
														throws IOException {
		String s;
		while ((s = in.readLine()) != null) {
			if (s.length() == 0)
				continue;	// Empty line.
			if (!s.startsWith("%%section"))
				continue;
			int ind, strlen = s.length();
			for (ind = 9; ind < strlen && Character.isSpace(s.charAt(ind)); ++ind)
					;
			if (s.startsWith(section, ind)) {	
				System.out.println("searchTextMsgSection: found " + section);
				return true;
			}
		}
			// Section was not found.
		return false;
	}
	/*
	 *	Read in text, where each line is of the form "nnn:sssss", where nnn is
	 *	to be the Flex entry #, and anything after the ':' is the string to
	 *	store.  
	 *	If 'section' passed is null, we assume we're just passed the "%%section" line.
	 *	NOTES:	Entry #'s may be skipped, and may be given in hex (0xnnn)
	 *			or decimal.
	 *		Max. text length is 1024.
	 *		A line beginning with a '#' is a comment.
	 *		A 'section' can be marked:
	 *			%%section shapes
	 *				....
	 *			%%endsection
	 *	Output:	# of first message (i.e., lowest-numbered msg), or -1 if
	 *		error.
	 */
	private static Vector<byte[]> readTextMsgFile(BufferedReader in, String section)
														throws IOException {
		Vector<byte[]> strings = new Vector<byte[]>(1000);
		int linenum = 0;
		final int NONEFOUND = 0xffffffff;
		int first = NONEFOUND;// Index of first one found.
		int next_index = 0;// For auto-indexing of lines
		String s;
		while ((s = in.readLine()) != null) {
			++linenum;
			if (s.length() == 0)
				continue;	// Empty line.
			if (section != null) {
				if (!s.startsWith("%%section", 8))
					continue;
				int ind, strlen = s.length();
				for (ind = 9; ind < strlen && Character.isSpace(s.charAt(ind)); ++ind)
					;
				if (s.startsWith(section, ind)) {	// Found the section.
					section = null;
					continue;
				}
				System.out.println("Line #" + linenum + 
					" has the wrong section name " + " != " 
										+ section);
				return null;
			}
			if (s.startsWith("%%endsection"))
				break;
			int ind, index, endptr;

			if (s.charAt(0) == ':') {	// Auto-index lines missing an index.
				index = next_index++;
				ind = 0;
			} else {	// Get line# in decimal, hex, or oct.
				int strlen = s.length();
				for (ind = 0; ind < strlen && Character.isDigit(s.charAt(ind)); ++ind)
					;
				if (ind == 0) {
					if (s.charAt(ind) == '#')
						continue;	// Comment.
					System.out.println("Line " + linenum + " doesn't start with a number");
					return null;
				}
				index = Integer.parseInt(s.substring(0, ind));
				if (s.charAt(ind) != ':') {
					System.out.println("Missing ':' in line " + linenum + 
						".  Ignoring line");
					continue;
				}
			}
			if (index >= strings.size())
				strings.setSize(index + 1);
			strings.setElementAt(s.substring(ind + 1).getBytes(), index);
			if (index < first)
				first = index;
		}
		return strings;
	}
	/*
	 *	Generic base data-agnostic reader class.
	 */
	public static class BaseReader {
		protected boolean haveversion;
		protected void readData(InputStream in, int index, int version,
				boolean patch, int game, boolean binary)
			{  }
			// Binary data file.
		protected void read_binary_internal(InputStream in, boolean patch, 
				int game) {
			int vers = 0;
			if (haveversion)
				vers = EUtil.Read1(in);
			int cnt = ReadCount(in);
			System.out.println("**** cnt = " + cnt);
			for (int j = 0; j < cnt; j++)
				readData(in, j, vers, patch, game, true);
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
				PushbackInputStream strin = new PushbackInputStream(
						new ByteArrayInputStream(ptr));
				System.out.println("parse: " + new String(ptr));
				readData(strin, j, version, patch, game, false);
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
			if (txt == null)
				ExultActivity.fileFatal(flexfile);
			else {
				InputStream strin = new ByteArrayInputStream(txt);
				read_binary_internal(strin, false, game);
			}
		}
	}

	public static class IDReaderFunctor {
		public int read(InputStream in, int index, int version, 
				boolean binary)
			{ return binary ? EUtil.Read2(in) : EUtil.ReadInt((PushbackInputStream)in); }
	}
	public static interface ReaderFunctor {
		public boolean read(InputStream in, int version, 
				boolean patch, int game, ShapeInfo info);
	}
	public static interface PostFunctor {
		void postProcess(InputStream in, int version, boolean patch,
				int game, ShapeInfo info);
	}
	/*
	 *	Generic functor-based reader class for maps.
	 */
	public static class FunctorMultidataReader extends BaseReader {
		ShapeInfo info[];
		ReaderFunctor reader;
		PostFunctor postread;
		IDReaderFunctor idread;
		@Override
		public void readData(InputStream in, int index, int version,
			boolean patch, int game, boolean binary) {
			int id = idread.read(in, index, version, binary);
			//System.out.println("Reading entry for shape #" + id);
			if (id >= 0) {
				ShapeInfo inf = info[id];
				
				reader.read(in, version, patch, game, inf);
				if (postread != null)
					postread.postProcess(in, version, patch, game, inf);
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
