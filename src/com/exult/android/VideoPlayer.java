package com.exult.android;

import android.app.Activity;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.VideoView;

public class VideoPlayer extends GameSingletons {
	private static VideoPlayer instance;
	private View mainView, gameView;
	private VideoView video;
	private Activity exult;
	private Thread onCompleteThread;
	
	//	Play, and execute 'thread' when done.
	public VideoPlayer(String fileName, Thread doneThread) {
		instance = this;
		onCompleteThread = doneThread;
		exult = ExultActivity.instanceOf();
		mainView = exult.findViewById(R.id.main_layout);		
		gameView = exult.findViewById(R.id.game);
		video = (VideoView) exult.findViewById(R.id.video_view);
		MediaPlayer.OnCompletionListener onComplete = new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				close();
			}
		};
		video.setOnCompletionListener(onComplete);
		video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				String msg = "Video: Error callback, what = " + what + 
				  ", extra = " + extra;
				System.out.println(msg);
				ExultActivity.showToast(msg);
				close();
				return true;
			}
		});
		String fullName = EUtil.getSystemPath("<VIDEO>/" + fileName);
		if (EUtil.U7exists(fullName) == null) {
			ExultActivity.showToast("Can't find " + fullName);
		} else {
			tqueue.pause();
			mainView.setVisibility(View.INVISIBLE);
			gameView.setVisibility(View.INVISIBLE);
			video.setVisibility(View.VISIBLE);
			video.setVideoPath(fullName);
			video.start();
		}
	}
	public static boolean closeIfPlaying() {
		if (instance == null)
			return false;
		else {
			instance.close();
			return true;
		}
	}
	public void close() {
		instance = null;
		System.out.println("VideoPlayer.close");
		mainView.setVisibility(View.VISIBLE);			
		gameView.setVisibility(View.VISIBLE);
		video.setVisibility(View.GONE);
		if (onCompleteThread != null)
			onCompleteThread.start();
		tqueue.resume();
	}
}
