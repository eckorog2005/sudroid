/**
 * Cell.java
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
 * holds the cell value for a spot in the sudoku board.
 * 
 * @author rlamb
 *
 */
public class Cell {

	//value of the cell
	private int value;
	
	//if error
	private boolean errorFlag;
	
	//invalid selection flag
	private boolean invalidSelection;
	
	/**
	 * Constructor
	 */
	public Cell(){
		super();
		this.value = 0;
		this.errorFlag = false;
		this.invalidSelection = false;
	}
	
	/**
	 * Constructor
	 */
	public Cell(int value){
		super();
		this.value = value;
		this.errorFlag = false;
		this.invalidSelection = false;
	}

	/**
	 * set the error flag
	 * 
	 * @param errorFlag
	 */
	public void setErrorFlag(boolean errorFlag) {
		this.errorFlag = errorFlag;
	}

	/**
	 * gets the error flag
	 * 
	 * @return
	 */
	public boolean isError() {
		return errorFlag;
	}

	/**
	 * set the value of the cell
	 * 
	 * @param value
	 */
	public void setValue(int value) {
		this.value = value;
	}

	/**
	 * get the value of the cell
	 * 
	 * @return
	 */
	public int getValue() {
		return value;
	}
	
	/**
	 * gets the invalid flag
	 * 
	 * @return
	 */
	public boolean isInvalidSelection(){
		return this.invalidSelection;
	}
	
	/**
	 * set the invalid selection flag
	 * 
	 * @param invalidSelection
	 */
	public void setInvalidSelection(boolean invalidSelection){
		this.invalidSelection = invalidSelection;
	}
}
