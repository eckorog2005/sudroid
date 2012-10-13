/**
 * SudroidGameActivity.java
 */

/**
 *  Copyright 2010,2012 Roger Lamb 
 * 
 *  This file is part of Sudroid.
 *
 *  Sudroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Sudroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Sudroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rlamb.android.sudroid;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler.Callback;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Main class that runs the game activity
 * 
 * @author rlamb
 * @version 1
 */
public class SudroidGameActivity extends Activity {
	
	 //stores what mode was passed in
	 public static final String mode =
	        "com.rlamb.android.sudroid.library.SudroidGameView.mode";
	 
	 
	 private GameModeEnum gameType;
	 
	 
	 private int id;

	 //view of the board
	 private SudroidView mGameView;
	 //timer string
	 private TextView mTimerView;
	 
	 //called when this activity is first called
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	    	super.onCreate(savedInstanceState);
	    	String time = "";
	    	String board = "";
	    	String user = "";
	    	
	    	gameType = GameModeEnum.values()[this.getIntent().getIntExtra(mode, 1)];
	    	
	    	
	    	ProgressDialog temp = new ProgressDialog(this);
	    	temp.show();
	    	
	    	//load board
	    	board = loadBoard(this.gameType);
	    	this.id = loadID(board);
	    	
	    	//load time if time 
	    	if(this.gameType.equals(GameModeEnum.resume)){
	    		user = resumeBoard();
	    		time = loadTime(this.gameType);
	    	}
	    	
	    	//draw screen
	        setContentView(R.layout.game_screen);
	        
	        //grab the timer id and game id, and add some settings
	        mTimerView = (TextView) this.findViewById(R.id.time);
	        mGameView = (SudroidView) this.findViewById(R.id.game_view);
	        mGameView.setFocusable(true);
	        mGameView.setFocusableInTouchMode(true);
	        mGameView.setCellListener(new MyCellListener());
	        mGameView.setActivityHandler(new ActivityHandler());
	        mGameView.fillBoard(board);
	        mGameView.setGameID(this.id);
	        mGameView.setTimer(time);
	        mGameView.setDifficulty(this.gameType);
	        if(this.gameType.equals(GameModeEnum.resume)){
	        	mGameView.fillUserBoard(user);
	        }
	        
	        //add the number one to the highlighted square
	        findViewById(R.id.one).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(1);
	            }
	        });
	        
	        //add the number two to the highlighted square
	        findViewById(R.id.two).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(2);
	            }
	        });
	        
	        //add the number three to the highlighted square
	        findViewById(R.id.three).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(3);
	            }
	        });
	        
	        //add the number four to the highlighted square
	        findViewById(R.id.four).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(4);
	            }
	        });
	        
	      	//add the number five to the highlighted square
	        findViewById(R.id.five).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(5);
	            }
	        });
	        
	        //add the number six to the highlighted square
	        findViewById(R.id.six).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(6);
	            }
	        });
	        
	        //add the number seven to the highlighted square
	        findViewById(R.id.seven).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(7);
	            }
	        });
	        
	        //add the number eight to the highlighted square
	        findViewById(R.id.eight).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(8);
	            }
	        });
	        
	        //add the number nine to the highlighted square
	        findViewById(R.id.nine).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.addNumber(9);
	            }
	        });

	      	//clear the number in the highlighted square
	        findViewById(R.id.clear).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.clearNumber();
	            }
	        });
	        
	        //resets the whole puzzle
	        findViewById(R.id.reset).setOnClickListener(
	                new OnClickListener() {
	            public void onClick(View v) {
	                mGameView.resetBoard();
	            }
	        });
	        
	        temp.dismiss();
	 }
	 
	 /**
	  * load the id from the database
	  * 
	  * @param board board to search the database
	  * @return id of board
	  */
	 private int loadID(String board) {
		SudroidDBFactory db = SudroidDBFactory.getInstance(this);
		return db.getPuzzleID(board);
	}

	 /**
	  * get the resume board
	  * 
	  * @return the resume board
	  */
	private String resumeBoard() {
		SudroidDBFactory db = SudroidDBFactory.getInstance(this);
		String user = db.getResumeInput();
		return user;
	}

	/**
	 * load the time from the database
	 * 
	 * @param gameType game type selected
	 * @return time of board
	 */
	private String loadTime(GameModeEnum gameType) {
		SudroidDBFactory db = SudroidDBFactory.getInstance(this);
		if(gameType.equals(GameModeEnum.resume)){
			return db.getResumeTimer();
		}
		return "";
	}

	/**
	 * load a new game board
	 * 
	 * @param gameType2 game type
	 * @return the board
	 */
	private String loadBoard(GameModeEnum gameType2) {
		SudroidDBFactory db = SudroidDBFactory.getInstance(this);
		if(gameType2.equals(GameModeEnum.weekly)){
			return db.getWeeklyPuzzle();
		}else if(gameType2.equals(GameModeEnum.resume)){
			return db.getResumePuzzle();
		}else{
			return db.getRandomPuzzle(gameType2);
		}
	}

	/**
	 * load high score if game is finished
	 * 
	 * @param time time the game finished
	 */
	private void isFinished(String time){
		 Intent i = new Intent(this, SudroidHighScoreActivity.class);
         i.putExtra(SudroidHighScoreActivity.finishTime, time);
         if(gameType == GameModeEnum.weekly){
        	 i.putExtra("week", true);
         }
         i.putExtra("week", false);
         startActivity(i);
	 }
	 
	 /**
	  * listener class
	  * 
	  * @author rlamb
	  */
	 private class MyCellListener implements ICellListener {
		 public void onCellSelected() {

	     }
	 }
	 
	 /**
	  * handler class that changes the clock time
	  * 
	  * @author rlamb
	  */
	 protected class ActivityHandler implements Callback {
	        public boolean handleMessage(Message msg) {
	        	mTimerView.setText(msg.getData().getString("text"));
	        	if(msg.getData().getBoolean("finished")){
	        		isFinished(msg.getData().getString("text"));
	        	}
	        	return true;
	        }
	    }
	 
	 /**
	  * task to do when put into background
	  * 
	  */
	 @Override
	protected void onPause() {
		super.onPause();
		this.mGameView.pauseTimer();
		this.mGameView.saveGame();
	}
	 
	 /**
	  * task to do when game comes back
	  * 
	  */
	 @Override
	protected void onResume() {
		super.onResume();
		this.mGameView.resumeTimer();
		
	}
}
