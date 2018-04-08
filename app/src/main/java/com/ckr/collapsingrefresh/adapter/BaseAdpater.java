package com.ckr.collapsingrefresh.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public abstract class BaseAdpater<T, E extends BaseViewHolder> extends RecyclerView.Adapter<E> {
	private static final String TAG = "BaseAdpater";
	protected Context mContext;
	protected List<T> data;

	public BaseAdpater(Context context) {
		mContext = context;
		data = new ArrayList<>();
	}

	public BaseAdpater(Context context, List<T> data) {
		mContext = context;
		this.data = data;
	}

	public List<T> getData() {
		return data;
	}

	public void updateAll(List newData) {
		if (newData == null || newData.size() == 0) {
			Log.d(TAG, "updateAll: newData == null || newData.size() == 0");
			return;
		}
		data.clear();
		data.addAll(newData);
		notifyDataSetChanged();
	}

	public void updateItem(int start, T t) {
		if (t == null) {
			return;
		}
		if (start < 0 && start > data.size()) {
			throw new ArrayIndexOutOfBoundsException(start);
		}
		data.add(start, t);
		int len = data.size() - start;
		notifyItemChanged(start);
//		notifyDataSetChanged();
	}

	public void updateItem(T t) {
		if (t == null) {
			return;
		}
		int len = data.size();
		data.add(t);
		notifyItemRangeChanged(len, 1);
	}


	@Override
	public E onCreateViewHolder(ViewGroup parent, int viewType) {
		return getViewHolder(LayoutInflater.from(mContext).inflate(getLayoutId(viewType), parent, false), viewType);
	}

	@Override
	public void onBindViewHolder(E holder, int position) {
		convert(holder, position, data.get(position));
	}

	@Override
	public int getItemCount() {
		return data.size();
	}

	protected abstract E getViewHolder(View itemView, int viewType);

	protected abstract void convert(E holder, int position, T t);

	protected abstract int getLayoutId(int viewType);
}
