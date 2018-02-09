package com.ckr.collapsingrefresh.view;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ckr.collapsingrefresh.R;
import com.ckr.collapsingrefresh.util.ScreenUtil;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by PC大佬 on 2018/2/9.
 */

public abstract class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";
    private View view;
    private Unbinder unbinder;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(getContentLayoutId(), container, false);
        Log.d(TAG, "onCreateView: "+savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    protected abstract int getContentLayoutId();

    protected abstract void init();

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            onVisible();
        } else {
            onInvisible();
        }
    }

    protected void onVisible() {
    }

    protected void onInvisible() {
    }

    public void refreshFragment(){};
    public void onScrolled(int scrollY){};

    /*
   * 状态栏的显示
   * */
    protected void initStatusBar(View statusView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//必须大于该版本
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, ScreenUtil.getStatusBarHeight(getContext()));
            statusView.setLayoutParams(params);
            statusView.setVisibility(View.VISIBLE);
        } else {
            statusView.setVisibility(View.GONE);
        }
    }

    private Toast makeText;

    protected void toast(String content) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (makeText == null) {
            makeText = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        }
        makeText.setText(content);
        makeText.show();
    }

    public Dialog createLoadingDialog() {
        if (getActivity() == null) {
            return null;
        } else {
            Dialog loadingDialog = new Dialog(getActivity(), R.style.dialogTheme);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ScreenUtil.dp2px(150), ScreenUtil.dp2px(150));
            View inflate = View.inflate(getActivity(), R.layout.dialog_loading, null);
            loadingDialog.addContentView(inflate, params);
            return loadingDialog;
        }
    }

    protected void showDialog(Dialog dialog) {
        if (dialog != null) {
            if (!dialog.isShowing()) {
                dialog.show();
            }
        }
    }

    protected void dismissDialog(Dialog dialog) {
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

}
