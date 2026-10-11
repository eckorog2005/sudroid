/**
 * Position.java
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

/**
 * stores the x and y of a position
 * @author rlamb
 *
 */
public class Position {
	private int col;
	private int row;
	
	/**
	 * Constructor
	 */
	public Position(){
		super();
		this.col = 0;
		this.row = 0;
	}
	
	/**
	 * Constructor
	 * 
	 * @param row
	 * @param col
	 */
	public Position(int row, int col){
		super();
		this.row = row;
		this.col = col;
	}
	
	/**
	 * get the column
	 * 
	 * @return the column
	 */
	public int getCol(){
		return this.col;
	}
	
	/**
	 * get the row
	 * 
	 * @return the row of the postition
	 */
	public int getRow(){
		return this.row;
	}
	
	/**
	 * set the column (y)
	 * 
	 * @param col
	 */
	public void setCol(int col){
		this.col = col;
	}
	
	/**
	 * set the row (x)
	 * 
	 * @param row
	 */
	public void setRow(int row){
		this.row = row;
	}
	
	/**
	 * equals method
	 */
	@Override
	public boolean equals(Object pos){
		Position temp = (Position)pos;
		if(this.col == temp.col && this.row == temp.row){
			return true;
		}
		return false;
	}
	
	/**
	 * to string method
	 * 
	 * @return string string of object
	 */
	@Override
	public String toString() {
		return "row: "+this.row+", col: "+this.col;
	}
}
