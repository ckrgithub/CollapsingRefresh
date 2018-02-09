package com.ckr.collapsingrefresh.view.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ckr.collapsingrefresh.R;
import com.ckr.collapsingrefresh.model.AlbumList;


/**
 * Created by PC大佬 on 2018/2/9.
 */

public class MyAdapter extends BaseAdpater<AlbumList, MyAdapter.MHorld> {
    public MyAdapter(Context context) {
        super(context);
    }

    @Override
    protected MHorld getViewHorld(View itemView, int viewType) {
        return new MHorld(itemView);
    }

    @Override
    protected void convert(MHorld horld, int position, AlbumList albumList) {
        int drawableId = albumList.getDrawableId();
        String userName = albumList.getUserName();
        String title = albumList.getTitle();
        horld.imageView.setImageResource(drawableId);
        horld.title.setText(title);
        horld.userName.setText(userName);
    }

    @Override
    public int getItemViewType(int position) {
        return position % 2;
    }

    @Override
    protected int getLayoutId(int viewType) {
        if (viewType == 0) {
            return R.layout.item_album2;
        } else {
            return R.layout.item_album;
        }
    }

    class MHorld extends BaseViewHorld {

        private ImageView imageView;
        private TextView title;
        private TextView userName;

        public MHorld(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            title = (TextView) itemView.findViewById(R.id.title);
            userName = (TextView) itemView.findViewById(R.id.userName);
        }
    }
}
