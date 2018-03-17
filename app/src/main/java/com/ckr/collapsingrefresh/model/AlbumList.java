package com.ckr.collapsingrefresh.model;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public class AlbumList implements Cloneable {

	private int drawableId;
	private String title;
	private String userName;
	private int type;

	public AlbumList() {
	}

	public int getDrawableId() {
		return drawableId;
	}

	public void setDrawableId(int drawableId) {
		this.drawableId = drawableId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}


	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
