package com.ckr.collapsingrefresh.model;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public class AlbumList implements Cloneable{

    private int drawableId;
    private String title;
    private String userName;

    public AlbumList() {
    }

    public AlbumList(int drawableId, String title, String userName) {
        this.drawableId = drawableId;
        this.title = title;
        this.userName = userName;
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

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
