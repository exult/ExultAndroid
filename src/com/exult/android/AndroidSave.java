package com.exult.android;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.exult.android.ExultActivity.YesNoDialog;
import com.exult.android.NewFileGump.SaveGameDetails;
import com.exult.android.NewFileGump.SaveGameParty;
import com.exult.android.NewFileGump.SaveInfo;

/*
 * A native Android save/restore screen.
 */
public class AndroidSave extends GameSingletons {
	private View myView, mainView;
	private ListView filesView;
	private EditText editView;
	private Button saveBtn, loadBtn, deleteBtn, cancelBtn;
	private Activity exult;
	private SaveInfo	games[];		// The list of savegames
	private SaveAdapter adapter;
	private int		num_games;	// Number of save games
	private int		first_free;	// The number of the first free savegame
	private int		selected = -1;
	private CheckBox selectedBtn = null;
	
	private VgaFile.ShapeFile cur_shot;		// Screenshot for current game
	private SaveGameDetails cur_details;	// Details of current game
	private SaveGameParty cur_party[];	// Party of current game

	// Gamedat is being used as a 'quicksave'
	private VgaFile.ShapeFile gd_shot;		// Screenshot in Gamedat
	private SaveGameDetails gd_details;	// Details in Gamedat
	private SaveGameParty gd_party[];	// Parts in Gamedat

	private VgaFile.ShapeFile screenshot;		// The picture to be drawn
	private SaveGameDetails details;	// The game details to show
	private SaveGameParty party[];		// The party to show
	private boolean is_readable;		// Is the save game readable
	private String filename;		// Filename of the savegame, if exists

	public AndroidSave(Activity exult) {
		myView = exult.findViewById(R.id.save_restore);
		mainView = exult.findViewById(R.id.main_layout);
		switchScreen(false);
		filesView = (ListView) exult.findViewById(R.id.sr_files);
		editView = (EditText) exult.findViewById(R.id.sr_editname);
		editView.setText("");
		this.exult = exult;
		setButtonHandlers();
		setTextHandler();
		LoadSaveGameDetails();
		setListHandler();
	}
	private void setListHandler() {
		filesView.setOnItemClickListener(new ListView.OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
	            //filesView.setSelection(pos);
	            SaveInfo g = games[pos];
	            Boolean enabled = true;
	            CheckBox btn = (CheckBox) v.findViewById(R.id.savename_choice);
	            if (btn == selectedBtn) {
	            	editView.setText("");
	            	selectedBtn.setChecked(false);
	            	selected = -1;
	            	selectedBtn = null;
	            	enabled = false;
	            	is_readable = false;
	            } else {
	            	editView.setText(g.toString());
	            	btn.setChecked(true);
	            	if (selectedBtn != null)
	            		selectedBtn.setChecked(false);
	            	selected = pos;
	            	selectedBtn = btn;
	            	is_readable = g.readable;
	            }
	            enableButtons(enabled);
	        }
	    });
	}
	private void enableButtons(boolean enabled) {
		saveBtn.setEnabled(enabled);
        loadBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
	}
	private void setButtonHandlers() {
		saveBtn = (Button) exult.findViewById(R.id.save_button);
		loadBtn = (Button) exult.findViewById(R.id.load_button);
		deleteBtn = (Button) exult.findViewById(R.id.delete_button);
    	cancelBtn = (Button) exult.findViewById(R.id.save_cancel_button);
    	enableButtons(false);
    	saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { save(false);}
        });
    	loadBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { load();}
        });
    	deleteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { deleteFile();}
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { close();}
        });
        
	}
	private void setTextHandler() {
		editView.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            	String txt = editView.getText().toString();
            	saveBtn.setEnabled(txt != null && txt.length() > 0);
            }
		}); 
	}
	private void switchScreen(boolean main) {
		if (main) {
			mainView.setVisibility(View.VISIBLE);
			myView.setVisibility(View.INVISIBLE);
		} else {
			mainView.setVisibility(View.INVISIBLE);
			myView.setVisibility(View.VISIBLE);
		}
	}
	private void close() {
		switchScreen(true);
	}
	public void load() {			// 'Load' was clicked.
		// Aborts if unsuccessful.
		if (selected != -1) 
			gwin.read(games[selected].num);
		else // Read Gamedat
			gwin.read();
		//+++NEEDED restored = true;
		
		// Reset Selection
		selected = -1;
		if (selectedBtn != null) {
			selectedBtn.setChecked(false);
			selectedBtn = null;
		}
		enableButtons(false);
		close();
	}
	// Reset everything
	private void reset() {
		selected = -1;
		editView.setText("");
		enableButtons(false);
		FreeSaveGameDetails();
		LoadSaveGameDetails();
		gwin.setAllDirty();
	}
	private void resetOnUi() {
		exult.runOnUiThread(new Runnable() {
			public void run() {
				reset();
				switchScreen(false);	// Back to save/restore dialog.
		    }
		});
	}
	public void save(boolean dontAsk) {			// 'Save' was clicked.
		String newname = editView.getText().toString();
		// Shouldn't ever happen.
		if (newname == null || newname.length() == 0)
			return;	
		// Already a game in this slot? If so ask to delete
		if (selected != -1 && !dontAsk) { 
			Observer o = new Observer() {
				public void update(Observable o, Object arg) {
					if ((Boolean)arg)
						save(true);
				}
			};
			ExultActivity.askYesNo(o,
					"Okay to write over existing saved game?");
			return;
		}
		int num = selected >= 0 ? games[selected].num 
				: first_free;
		if (num >= 0) {	// Write to gamedat, then to savegame file.
			Observer o = new Observer() {
				public void update(Observable o, Object arg) {
					resetOnUi();	// Write done, so update list.
					System.out.println("Saved game #" + selected + " successfully.");
				}
			};
			switchScreen(true);
			gwin.write(num, newname, o);
		} else try {
			gwin.write();	// Quick save.
			reset();
		} catch (IOException e) {
			System.out.println("Error during quick save");
		}
	}
	public void deleteFile() {		// 'Delete' was clicked.
		// Shouldn't ever happen.
		if (selected == -1)
			return;	
		Observer o = new Observer() {
			public void update(Observable o, Object arg) {
				if (!(Boolean)arg)
					return;
				EUtil.U7remove(games[selected].filename);
				filename = null;
				is_readable = false;
				System.out.println("Deleted Save game #" + selected + " (" +
							games[selected].filename + ") successfully.");
				resetOnUi();
			}
		};
		ExultActivity.askYesNo(o, "Okay to delete saved game?");
	}
	void LoadSaveGameDetails() {	// Loads (and sorts) all the savegame details
		int		i;
		// Gamedat Details
		/* +++++++++FINISH
		gwin.getSaveinfo(gd_shot, gd_details, gd_party);
		*/
		// Current screenshot
		cur_shot = gwin.createMiniScreenshot(false);	// Quick but low quality.

		// Current Details
		cur_details = new SaveGameDetails();

		if (gd_details != null) 
			cur_details.save_count = gd_details.save_count;
		else cur_details.save_count = 0;

		cur_details.party_size = (byte)(partyman.getCount()+1);
		cur_details.game_day = (short) (clock.getTotalHours() / 24);
		cur_details.game_hour = (byte)clock.getHour();
		cur_details.game_minute = (byte)clock.getMinute();
		
		Calendar timeinfo = Calendar.getInstance();

		cur_details.real_day = (byte)timeinfo.get(Calendar.DAY_OF_MONTH);
		cur_details.real_hour = (byte)timeinfo.get(Calendar.HOUR);
		cur_details.real_minute = (byte)timeinfo.get(Calendar.MINUTE);
		cur_details.real_month = (byte)(timeinfo.get(Calendar.MONTH)+1);
		cur_details.real_year = (short)timeinfo.get(Calendar.YEAR);
		cur_details.real_second = (byte)timeinfo.get(Calendar.SECOND);
		// Current Party
		cur_party = new SaveGameParty[cur_details.party_size];
		for (i=0; i<cur_details.party_size ; i++) {
			Actor npc;
			if (i == 0)
				npc = gwin.getMainActor();
			else
				npc = gwin.getNpc(partyman.getMember(i-1));
			cur_party[i] = new SaveGameParty();
			String namestr = npc.getNpcName();
			cur_party[i].namestr = namestr;
			int j, namelen = Math.min(namestr.length(), cur_party[i].name.length);
			for (j = 0; j < namelen; ++j)
				cur_party[i].name[j] = (byte)namestr.charAt(j);
			for ( ; j < namelen; ++j)
				cur_party[i].name[j] = (byte)0;
			cur_party[i].shape = (short) npc.getShapeNum();
			cur_party[i].shape_file = npc.getShapeFile();

			cur_party[i].dext = (byte)npc.getProperty(Actor.dexterity);
			cur_party[i].str = (byte)npc.getProperty(Actor.strength);
			cur_party[i].intel = (byte)npc.getProperty(Actor.intelligence);
			cur_party[i].health = (byte)npc.getProperty(Actor.health);
			cur_party[i].combat = (byte)npc.getProperty(Actor.combat);
			cur_party[i].mana = (byte)npc.getProperty(Actor.mana);
			cur_party[i].magic = (byte)npc.getProperty(Actor.magic);
			cur_party[i].training = (byte)npc.getProperty(Actor.training);
			cur_party[i].exp = npc.getProperty(Actor.exp);
			cur_party[i].food = (byte)npc.getProperty(Actor.food_level);
			cur_party[i].flags = npc.getFlags();
			cur_party[i].flags2 = npc.getFlags2();
		}
		party = cur_party;
		screenshot = cur_shot;
		details = cur_details;
		// Now read save game details
		String mask = String.format(EFile.SAVENAME2, game.isBG() ? "bg" : "si");

		Vector<String> filenames = new Vector<String>();
		EUtil.U7ListFiles(mask, filenames);
		num_games = filenames.size();
		
		games = new SaveInfo[num_games];	

		// Setup basic details
		for (i = 0; i<num_games; i++) {
			games[i] = new SaveInfo();
			games[i].filename = filenames.elementAt(i);
			System.out.println("FILE: " + games[i].filename);
			games[i].SetSeqNumber();
		}
		// First sort the games so they will be sorted by number
		// This is so I can work out the first free game
		SaveInfo.comparator cmp = new SaveInfo.comparator();
		if (num_games > 0)
			Arrays.sort(games, cmp);
		// Read and cache all details
		first_free = -1;
		for (i = 0; i<num_games; i++) {
			games[i].readable = gwin.getSaveInfo(games[i].num, games[i]); 
			if (first_free == -1 && i != games[i].num) 
				first_free = i;
		}
		System.out.println("firstFree = " + first_free);
		if (first_free == -1) 
			first_free = num_games;
		System.out.println("NOW firstFree = " + first_free);
		// Now sort it again, with all the details so it can be done by date
		if (num_games > 0) 
			Arrays.sort(games, cmp);
		adapter = new SaveAdapter(exult, R.layout.savename);
		filesView.setAdapter(adapter);
	}
	void	FreeSaveGameDetails() {	// Frees all the savegame details
		cur_shot = null;
		cur_details = null;
		cur_party = null;
		gd_shot = null;
		gd_details = null;
		gd_party = null;
		filename = null;
		games = null;
	}
	
	private class SaveAdapter extends ArrayAdapter<SaveInfo> {
		private Context ctx;
		
        public SaveAdapter(Context context, int textViewResourceId) {
                super(context, textViewResourceId, games);
                ctx = context;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	View v = convertView;
        	if (v == null) {
        		LayoutInflater vi = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        		v = vi.inflate(R.layout.savename, null);
        	}
        	SaveInfo g = games[position];
        	if (g != null) {
        		CheckBox btn = (CheckBox) v.findViewById(R.id.savename_choice);
        		TextView txt = (TextView) v.findViewById(R.id.savename_text);
        		if (txt != null)
        			txt.setText(g.savename);
        		if(btn != null)
        			btn.setChecked(position == selected);
        	}
        	return v;
        }
	};
}
