/**
 * SudroidView.java
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


import java.util.Timer;
import java.util.TimerTask;
import com.rlamb.android.sudroid.R;
import com.rlamb.android.sudroid.SudroidGameActivity.ActivityHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Handler.Callback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * The view that does all the sudoku actions
 * 
 * @author rlamb
 * @version 1
 */
public class SudroidView extends View{

	//frames per second
	public static final long FPS_MS = 1000/2;
	
	//background
	private Drawable mDrawableBg;
	
	//highlight sprite
	private Bitmap highlight;
	
	//Sudoku class to handle the math
	private Board sudoku;
	
	//margin size of board
	private static final int MARGIN = 1;
	private static final int MSG_CHANGE = 1;

	//src and dest rectangles
    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();
    
    //hightlight rectangle
    private final Rect mHighlightRect = new Rect();
    
    //paint for the view
    private Paint mBmpPaint;
    
    //listener
    private ICellListener mCellListener;
    
    //hightlighted position
    private Position displayHighlighted;
    
    //offset values
    private int mSxy;
	private int mOffetX;
	private int mOffetY;
	
	//holds this class handlers
	private final Handler mHandler = new Handler(new MyHandler());
	private Handler aHandler;

	//timer value
	private int mTimerLimit;
	
	// updates the screen clock. Also used for tempo timing.
    private Timer mTimer = null;

    //task to support second breaks
    private TimerTask mTimerTask = null;

    // one second - used to update timer
    private int mTaskIntervalInMillis = 1000;
    
    private boolean mTimerFlag;
    
    private int id;
    
    private GameModeEnum gameType;
    
    //database factory
    private SudroidDBFactory db;
	
	/**
	 * Constructor
	 * 
	 * @param context
	 * @param attrs
	 */
	public SudroidView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.requestFocus();
		
		//draw grid
		mDrawableBg = getResources().getDrawable(R.drawable.grid);
        setBackgroundDrawable(mDrawableBg);
        
        //set the sprites
        setBitmaps();
        
        //set highlight square if not null
        if(this.highlight != null){
        	this.mSrcRect.set(0, 0, highlight.getWidth() - 1, highlight.getHeight() - 1);
        }
        
        //set paint object
        this.mBmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBmpPaint.setTextSize(40);
        
        //make new highlight if needed.
        this.displayHighlighted = new Position(-1,-1);
        mTimerLimit = -1;

        //load board from database
        db = SudroidDBFactory.getInstance(context);
        db.checkWeeklyPuzzle();
        
        //initialize the board
        this.sudoku = new Board();
        
        // kick off the timer task for counter update if not already
        // initialized
        try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mTimerFlag = true;
		if(mTimer == null){
			mTimer = new Timer();
		}
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                public void run() {
                    doCountUp();
                }
            };
            //schedule the new timer
            mTimer.schedule(mTimerTask, mTaskIntervalInMillis);

        }// end of TimerTask init block
	}
	
	//clear the number that is highlighted if there
	public void clearNumber(){
		if(this.displayHighlighted.getCol() != -1){
			this.sudoku.clearNumber(this.displayHighlighted);
			mHandler.sendEmptyMessageDelayed(MSG_CHANGE, FPS_MS);
		}
	}
	
	//add the number entered into the highlighted square
	public void addNumber(int value){
		if(this.displayHighlighted.getCol() != -1){
			try {
				this.sudoku.addNumber(value, this.displayHighlighted);
			} catch (InvalidNumberValueException e) {
			//never should get here
			} catch (InvalidPositionLocationException e) {
			//never should get here
			} catch (InvalidSodokuPositionException e) {
			//do nothing, taken care of later
			}finally{
				mHandler.sendEmptyMessageDelayed(MSG_CHANGE, FPS_MS);
			}
		}
	}

	//set the listener object
    public void setCellListener(ICellListener cellListener) {
        mCellListener = cellListener;
    }
    
    //check if game is finished
    public boolean isFinished(){
    	return this.sudoku.isFinished();	
    }
    
    //draw new screen
    @Override
    protected void onDraw(Canvas canvas){
    	super.onDraw(canvas);
    	
    	 //Initialize the data
    	 int sxy = this.mSxy;
         int x7 = this.mOffetX;
         int y7 = this.mOffetY;
         
         //loop though each cell
         for (int j = 0, y = y7; j < 9; j++, y += sxy) {
             for (int i = 0, x = x7; i < 9; i++, x += sxy) {
                 Cell tempNumber = this.sudoku.getCell(new Position(j+1,i+1));
            	 mDstRect.offsetTo(MARGIN+x, MARGIN+y);
            	 
            	 //if a number draw the right sprite
            	 if(tempNumber.getValue() != 0){
            		 if(tempNumber.isInvalidSelection()){
            			 this.mBmpPaint.setColor(Color.rgb(0, 150, 0));
            			 canvas.drawText(Integer.valueOf(tempNumber.getValue()).toString(),MARGIN+x+15, MARGIN+y+40, this.mBmpPaint);
            		 }else if(tempNumber.isError()){
            			 this.mBmpPaint.setColor(Color.RED);
            			 canvas.drawText(Integer.valueOf(tempNumber.getValue()).toString(),MARGIN+x+15, MARGIN+y+40, this.mBmpPaint);
            		 }else{
            			 this.mBmpPaint.setColor(Color.BLACK);
            			 canvas.drawText(Integer.valueOf(tempNumber.getValue()).toString(),MARGIN+x+15, MARGIN+y+40, this.mBmpPaint);	 
            		 }
            	 }
            	 
            	 //draw the highlighted square if here
                 if(this.displayHighlighted.getRow() == j+1 &&
                		 this.displayHighlighted.getCol() == i+1){
                	 canvas.drawBitmap(this.highlight, this.mSrcRect, this.mDstRect, this.mBmpPaint);
                 }
             }
         }
         if(this.isFinished()){
        	 mTimer.cancel();
        	 mTimerTask.cancel();
        	 
        	 Message msg = aHandler.obtainMessage();

             Bundle b = new Bundle();
             b.putString("text", this.changeTimeToString());
             b.putBoolean("finished", true);
             this.mTimerFlag = false;
             msg.setData(b);
             aHandler.sendMessage(msg);
         }
    }
    
    //Measure the view
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Keep the view squared
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int d = w == 0 ? h : h == 0 ? w : w < h ? w : h;
        setMeasuredDimension(d, d);
    }

    //if view size change
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int sx = (w - 2 * MARGIN) / 9;
        int sy = (h - 2 * MARGIN) / 9;

        int size = sx < sy ? sx : sy;

        mSxy = size;
        mOffetX = (w - 9 * size) / 2;
        mOffetY = (h - 9 * size) / 2;

        mDstRect.set(MARGIN, MARGIN, size - MARGIN, size - MARGIN);
    }
    
    //when board is touched
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        //do nothing if on touch
        if (action == MotionEvent.ACTION_DOWN) {
            return true;

        //on release, add highlighted square
        } else if (action == MotionEvent.ACTION_UP) {
            
        	//get coordinates 
        	int x = (int) event.getX();
            int y = (int) event.getY();

            //set to cell number
            int sxy = mSxy;
            x = (x - MARGIN) / sxy;
            y = (y - MARGIN) / sxy;

            //set row if legal value
            if (this.isEnabled() && x >= 0 && x < 9 && y >= 0 & y < 9) {
                this.mHighlightRect.set(MARGIN + x * sxy, MARGIN + y * sxy,
                               MARGIN + (x + 1) * sxy, MARGIN + (y + 1) * sxy);
                this.displayHighlighted.setRow(y+1);
                this.displayHighlighted.setCol(x+1);
                
                if (mCellListener != null) {
                    mCellListener.onCellSelected();
                }
                mHandler.sendEmptyMessageDelayed(MSG_CHANGE, FPS_MS);
            }

            return true;
        }

        return false;
    }
    
    //save state
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();

        //database work
        super.onSaveInstanceState();
        SudroidDatabaseHelper helper = 
        	new SudroidDatabaseHelper(this.getContext());
    	SQLiteDatabase db = helper.getReadableDatabase();
    	db.delete("Current", null, null);
    	ContentValues values = new ContentValues();
    	values.put("_id", this.id);
    	values.put("time", this.changeTimeToString());
    	values.put("difficulty", this.gameType.name());
    	values.put("userEntered", this.sudoku.getUserEnteredString());
    	db.insert("Current", null, values);
    	db.close();
        return b;
    }

    /**
     * set the sprites up
     */
  	private void setBitmaps() {
		this.highlight = this.getResBitmap(R.drawable.highlight);
	}
	
  	/**
  	 * convert the pics to bitmaps
  	 * 
  	 * @param bmpResId
  	 * @return
  	 */
	private Bitmap getResBitmap(int bmpResId) {
        Options opts = new Options();
        opts.inDither = false;

        Resources res = getResources();
        Bitmap bmp = BitmapFactory.decodeResource(res, bmpResId, opts);

        if (bmp == null && isInEditMode()) {
            // BitmapFactory.decodeResource doesn't work from the rendering
            // library in Eclipse's Graphical Layout Editor. Use this workaround instead.

            Drawable d = res.getDrawable(bmpResId);
            int w = d.getIntrinsicWidth();
            int h = d.getIntrinsicHeight();
            bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, w - 1, h - 1);
            d.draw(c);
        }

        return bmp;
    }
	
	/**
	 * set the timer of the board
	 * 
	 * @param time time to set to.
	 */
	public void setTimer(String time){
		int min = 0;
		int sec = 0;
		String[] splits = time.split(":");
		if(splits.length == 2){
			min = Integer.parseInt(splits[0]);
			min = min * 60;
			sec = Integer.parseInt(splits[1]);
			mTimerLimit = min + sec;
		}
	}
	
	/**
     * Does the work of updating timer
     * 
     */
    private void doCountUp() {
        if(mTimerFlag){
        	//Log.d(TAG,"Time left is " + mTimerLimit);
        	mTimerLimit = mTimerLimit + 1;
        	String mTimerValue = this.changeTimeToString();

        	Message msg = aHandler.obtainMessage();

        	Bundle b = new Bundle();
        	b.putString("text", mTimerValue);
        	b.putBoolean("finished", false);

        	mTimerTask = new TimerTask() {
        		public void run() {
        			doCountUp();
            	}
        	};

        	mTimer.schedule(mTimerTask, mTaskIntervalInMillis);

        	//this is how we send data back up to the main JetBoyView thread.
        	//if you look in constructor of JetBoyView you will see code for
        	//Handling of messages. This is borrowed directly from lunar lander.
        	//Thanks again!
        	msg.setData(b);
        	aHandler.sendMessage(msg);
        
        }
    }
	
	
	/**
	 * handle to refresh the board
	 * 
	 * @author rlamb
	 *
	 */
	private class MyHandler implements Callback {
        public boolean handleMessage(Message msg) {
            invalidate();
        	return true;
        }
    }


	/**
	 * sets the timer handler.
	 * 
	 * @param activityHandler
	 */
	public void setActivityHandler(ActivityHandler activityHandler) {
		aHandler = new Handler(activityHandler);	
	}
	
	/**
	 * convert the time to a string value
	 * 
	 * @return string time
	 */
	private String changeTimeToString(){
		String mTimerValue = "";
		try {
        	//subtract one minute and see what the result is.
        	int Minutes = mTimerLimit / 60;
        	int seconds = mTimerLimit % 60;

			if (Minutes >= 10) {
        		mTimerValue = Minutes + ":";
        	} else {
        		mTimerValue = "0"+Minutes+":";
        	}
        	if (seconds >= 10) {
        		mTimerValue = mTimerValue.concat(Integer.toString(seconds));
        	} else {
            	mTimerValue = mTimerValue.concat("0"+seconds);
        	}
    	} catch (Exception e1) {
        	Log.e("error", "doCountUp threw " + e1.toString());
    	}
    	return mTimerValue;
	}
	
	/**
	 * Pauses the game timer
	 */
	public void pauseTimer(){
		this.mTimerFlag = false;
		mTimer.cancel();
		mTimer.purge();
		mTimer = null;
		mTimerTask.cancel();
		mTimerTask = null;
	}
	
	/**
	 * resumes the time
	 * 
	 */
	public void resumeTimer(){
		// kick off the timer task for counter update if not already
        // initialized
		mTimerFlag = true;
		if(mTimer == null){
			mTimer = new Timer();
		}
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                public void run() {
                    doCountUp();
                }
            };
            //schedule the new timer
            mTimer.schedule(mTimerTask, mTaskIntervalInMillis);
        }
	}

	/**
	 * save the game
	 * 
	 */
	public void saveGame() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * fills the board loaded from the database
	 * 
	 * @param board board data to fill in
	 */
	public void fillBoard(String board) {
		int counter = 0;
		if(board != null && 
				board.length() == 
					this.sudoku.getSize() * this.sudoku.getSize()){
			for(int i = 1; i <= this.sudoku.getSize(); i++){
				for(int j = 1; j <= this.sudoku.getSize(); j++){
					if(board.charAt(counter) != '0'){
						try {
							this.sudoku.addNumber(Integer.parseInt(
									board.substring(counter, counter + 1)), 
									new Position(i,j));
							this.sudoku.getCell(new Position(i,j)).
									setInvalidSelection(true);
						} catch (InvalidNumberValueException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidPositionLocationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidSodokuPositionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					counter++;
				}
			}
		}
	}

	/**
	 * reset the whole board
	 */
	public void resetBoard() {
		for(int x = 1; x <= this.sudoku.getSize(); x++){
			for(int y = 1; y <= this.sudoku.getSize(); y++){
				Position pos = new Position(x, y);
				Cell cell = this.sudoku.getCell(pos);
				if(!cell.isInvalidSelection()){
					this.sudoku.clearNumber(pos);
				}
			}
		}
		mHandler.sendEmptyMessageDelayed(MSG_CHANGE, FPS_MS);
	}

	/**
	 * set the game id
	 * 
	 * @param id2 id to set
	 */
	public void setGameID(int id2) {
		this.id = id2;
	}

	/**
	 * set the difficulty of the board
	 * 
	 * @param gameType game type
	 */
	public void setDifficulty(GameModeEnum gameType) {
		this.gameType = gameType;
	}

	/**
	 * fill the user data from a resume game
	 * 
	 * @param board board data
	 */
	public void fillUserBoard(String board) {
		int counter = 0;
		if(board != null && 
				board.length() == 
					this.sudoku.getSize() * this.sudoku.getSize()){
			for(int i = 1; i <= this.sudoku.getSize(); i++){
				for(int j = 1; j <= this.sudoku.getSize(); j++){
					if(board.charAt(counter) != '0'){
						try {
							this.sudoku.addNumber(Integer.parseInt(
									board.substring(counter, counter + 1)), 
									new Position(i,j));
						} catch (InvalidNumberValueException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidPositionLocationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidSodokuPositionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					counter++;
				}
			}
		}
	}
}
