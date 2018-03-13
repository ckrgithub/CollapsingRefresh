package com.ckr.collapsingrefresh.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ckr.collapsingrefresh.R;
import com.ckr.collapsingrefresh.model.AlbumList;


/**
 * Created by PC大佬 on 2018/2/9.
 */

public class MyAdapter extends BaseAdpater<AlbumList, MyAdapter.MyHolder> {
    public MyAdapter(Context context) {
        super(context);
    }

    @Override
    protected MyHolder getViewHolder(View itemView, int viewType) {
        return new MyHolder(itemView);
    }

    @Override
    protected void convert(MyHolder holder, int position, AlbumList albumList) {
        int drawableId = albumList.getDrawableId();
        String userName = albumList.getUserName();
        String title = albumList.getTitle();
        holder.imageView.setImageResource(drawableId);
        holder.title.setText(title);
        holder.userName.setText(userName);
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

    class MyHolder extends BaseViewHolder {

        private ImageView imageView;
        private TextView title;
        private TextView userName;

        public MyHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.imageView);
            title = (TextView) itemView.findViewById(R.id.title);
            userName = (TextView) itemView.findViewById(R.id.userName);
        }
    }
}
