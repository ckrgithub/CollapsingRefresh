package com.scwang.smartrefresh;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;

import com.ckr.behavior.R;
import com.ckr.behavior.listener.OnOffsetListener;
import com.scwang.smartrefresh.api.DefaultRefreshFooterCreater;
import com.scwang.smartrefresh.api.DefaultRefreshHeaderCreater;
import com.scwang.smartrefresh.api.RefreshContent;
import com.scwang.smartrefresh.api.RefreshFooter;
import com.scwang.smartrefresh.api.RefreshHeader;
import com.scwang.smartrefresh.api.RefreshKernel;
import com.scwang.smartrefresh.api.RefreshLayout;
import com.scwang.smartrefresh.api.RefreshScrollBoundary;
import com.scwang.smartrefresh.constant.DimensionStatus;
import com.scwang.smartrefresh.constant.RefreshState;
import com.scwang.smartrefresh.constant.SpinnerStyle;
import com.scwang.smartrefresh.footer.ClassicsFooter;
import com.scwang.smartrefresh.header.ClassicsHeader;
import com.scwang.smartrefresh.impl.RefreshContentWrapper;
import com.scwang.smartrefresh.impl.RefreshFooterWrapper;
import com.scwang.smartrefresh.impl.RefreshHeaderWrapper;
import com.scwang.smartrefresh.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.listener.OnMultiPurposeListener;
import com.scwang.smartrefresh.listener.OnRefreshListener;
import com.scwang.smartrefresh.listener.OnRefreshLoadmoreListener;
import com.scwang.smartrefresh.util.DensityUtil;
import com.scwang.smartrefresh.util.PostRunable;
import com.scwang.smartrefresh.util.ViscousFluidInterpolator;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.ckr.behavior.util.RefreshLog.Logd;
import static com.ckr.behavior.util.RefreshLog.Loge;
import static com.ckr.behavior.util.RefreshLog.Logw;
import static com.scwang.smartrefresh.util.DensityUtil.dp2px;
import static java.lang.System.currentTimeMillis;

/**
 * 智能刷新布局
 * Intelligent Refreshlayout
 * Created by SCWANG on 2017/5/26.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class SmartRefreshLayout extends ViewGroup implements RefreshLayout {

	//<editor-fold desc="属性变量 property and variable">

	//<editor-fold desc="滑动属性">
	protected int mTouchSlop;
	protected int mSpinner;//当前的 Spinner
	protected int mLastSpinner;//最后的，的Spinner
	protected int mTouchSpinner;//触摸时候，的Spinner
	protected int mReboundDuration = 250;
	protected int mScreenHeightPixels;
	protected float mTouchX;
	protected float mTouchY;
	protected float mLastTouchX;//用于实现Header的左右拖动效果
	protected float mLastTouchY;//用于实现多点触摸
	protected float mDragRate = .5f;
	protected boolean mIsBeingDragged;
	protected Interpolator mReboundInterpolator;
	protected View mFixedHeaderView;//固定在头部的视图
	protected View mFixedFooterView;//固定在底部的视图
	protected int mFixedHeaderViewId;//固定在头部的视图Id
	protected int mFixedFooterViewId;//固定在 头部的视图Id
	//</editor-fold>

	//<editor-fold desc="功能属性">
	protected int[] mPrimaryColors;
	protected boolean mEnableRefresh = true;
	protected boolean mEnableLoadmore = false;
	protected boolean mDisableContentWhenRefresh = false;//是否开启在刷新时候禁止操作内容视图
	protected boolean mDisableContentWhenLoading = false;//是否开启在刷新时候禁止操作内容视图
	protected boolean mEnableHeaderTranslationContent = true;//是否启用内容视图拖动效果
	protected boolean mEnableFooterTranslationContent = true;//是否启用内容视图拖动效果
	protected boolean mEnablePreviewInEditMode = true;//是否在编辑模式下开启预览功能
	protected boolean mEnableOverScrollBounce = true;//是否启用越界回弹
	protected boolean mEnableAutoLoadmore = true;//是否在列表滚动到底部时自动加载更多
	protected boolean mEnablePureScrollMode = false;//是否开启纯滚动模式
	protected boolean mEnableScrollContentWhenLoaded = true;//是否在加载更多完成之后滚动内容显示新数据
	protected boolean mLoadmoreFinished = false;//数据是否全部加载完成，如果完成就不能在触发加载事件

	protected boolean mManualLoadmore = false;//是否手动设置过Loadmore，用于智能开启
	protected boolean mManualNestedScrolling = false;//是否手动设置过 NestedScrolling，用于智能开启
	//</editor-fold>

	//<editor-fold desc="监听属性">
	protected OnRefreshListener mRefreshListener;
	protected OnLoadmoreListener mLoadmoreListener;
	protected OnMultiPurposeListener mOnMultiPurposeListener;
	//</editor-fold>

	//<editor-fold desc="嵌套滚动">
	protected int[] mParentScrollConsumed = new int[2];
	protected int[] mParentOffsetInWindow = new int[2];
	protected int mTotalUnconsumed;
	protected boolean mNestedScrollInProgress;
	protected NestedScrollingChildHelper mNestedScrollingChildHelper;
	protected NestedScrollingParentHelper mNestedScrollingParentHelper;
	//</editor-fold>

	//<editor-fold desc="内部视图">
	/**
	 * 头部高度
	 */
	protected int mHeaderHeight;
	protected DimensionStatus mHeaderHeightStatus = DimensionStatus.DefaultUnNotify;
	/**
	 * 底部高度
	 */
	protected int mFooterHeight;
	protected DimensionStatus mFooterHeightStatus = DimensionStatus.DefaultUnNotify;

	/**
	 * 扩展高度
	 */
	protected int mHeaderExtendHeight;
	/**
	 * 扩展高度
	 */
	protected int mFooterExtendHeight;
	/**
	 * 最大拖动比率(最大高度/Header高度)
	 */
	protected float mHeaderMaxDragRate = 2.0f;
	/**
	 * 最大拖动比率(最大高度/Footer高度)
	 */
	protected float mFooterMaxDragRate = 3.0f;
	/**
	 * 下拉头部视图
	 */
	protected RefreshHeader mRefreshHeader;
	/**
	 * 显示内容视图
	 */
	protected RefreshContent mRefreshContent;
	/**
	 * 上拉底部视图
	 */
	protected RefreshFooter mRefreshFooter;
	//</editor-fold>

	protected Paint mPaint;
	protected RefreshKernel mKernel;

	protected RefreshState mState = RefreshState.None;          //主状态
	protected RefreshState mViceState = RefreshState.None;      //副状态（刷新时候的状态）

	protected long mLastLoadingTime = 0;
	protected long mLastRefreshingTime = 0;

	protected int mHeaderBackgroundColor = 0;
	protected int mFooterBackgroundColor = 0;

	protected boolean mHeaderNeedTouchEventWhenRefreshing;
	protected boolean mFooterNeedTouchEventWhenRefreshing;

	protected static boolean sManualFooterCreater = false;
	protected static DefaultRefreshFooterCreater sFooterCreater = new DefaultRefreshFooterCreater() {
		@NonNull
		@Override
		public RefreshFooter createRefreshFooter(Context context, RefreshLayout layout) {
			return new ClassicsFooter(context);
		}
	};
	protected static DefaultRefreshHeaderCreater sHeaderCreater = new DefaultRefreshHeaderCreater() {
		@NonNull
		@Override
		public RefreshHeader createRefreshHeader(Context context, RefreshLayout layout) {
			return new ClassicsHeader(context);
		}
	};

	//</editor-fold>

	//<editor-fold desc="构造方法 construction methods">
	public SmartRefreshLayout(Context context) {
		super(context);
		this.initView(context, null);
	}

	public SmartRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.initView(context, attrs);
	}

	public SmartRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.initView(context, attrs);
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	public SmartRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		this.initView(context, attrs);
	}

	private void initView(Context context, AttributeSet attrs) {
		setClipToPadding(false);

		mScreenHeightPixels = context.getResources().getDisplayMetrics().heightPixels;
		mReboundInterpolator = new ViscousFluidInterpolator();
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

		mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
		mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);

		DensityUtil density = new DensityUtil();
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SmartRefreshLayout);

		ViewCompat.setNestedScrollingEnabled(this, ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableNestedScrolling, false));
		mDragRate = ta.getFloat(R.styleable.SmartRefreshLayout_srlDragRate, mDragRate);
		mHeaderMaxDragRate = ta.getFloat(R.styleable.SmartRefreshLayout_srlHeaderMaxDragRate, mHeaderMaxDragRate);
		mFooterMaxDragRate = ta.getFloat(R.styleable.SmartRefreshLayout_srlFooterMaxDragRate, mFooterMaxDragRate);
		mEnableRefresh = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableRefresh, mEnableRefresh);
		mReboundDuration = ta.getInt(R.styleable.SmartRefreshLayout_srlReboundDuration, mReboundDuration);
		mEnableLoadmore = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableLoadmore, mEnableLoadmore);
		mHeaderHeight = ta.getDimensionPixelOffset(R.styleable.SmartRefreshLayout_srlHeaderHeight, density.dip2px(100));
		mFooterHeight = ta.getDimensionPixelOffset(R.styleable.SmartRefreshLayout_srlFooterHeight, density.dip2px(60));
		mDisableContentWhenRefresh = ta.getBoolean(R.styleable.SmartRefreshLayout_srlDisableContentWhenRefresh, mDisableContentWhenRefresh);
		mDisableContentWhenLoading = ta.getBoolean(R.styleable.SmartRefreshLayout_srlDisableContentWhenLoading, mDisableContentWhenLoading);
		mEnableHeaderTranslationContent = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableHeaderTranslationContent, mEnableHeaderTranslationContent);
		mEnableFooterTranslationContent = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableFooterTranslationContent, mEnableFooterTranslationContent);
		mEnablePreviewInEditMode = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnablePreviewInEditMode, mEnablePreviewInEditMode);
		mEnableAutoLoadmore = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableAutoLoadmore, mEnableAutoLoadmore);
		mEnableOverScrollBounce = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableAutoLoadmore, mEnableOverScrollBounce);
		mEnablePureScrollMode = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnablePureScrollMode, mEnablePureScrollMode);
		mEnableScrollContentWhenLoaded = ta.getBoolean(R.styleable.SmartRefreshLayout_srlEnableScrollContentWhenLoaded, mEnableScrollContentWhenLoaded);
		mFixedHeaderViewId = ta.getResourceId(R.styleable.SmartRefreshLayout_srlFixedHeaderViewId, View.NO_ID);
		mFixedFooterViewId = ta.getResourceId(R.styleable.SmartRefreshLayout_srlFixedFooterViewId, View.NO_ID);

		mManualLoadmore = ta.hasValue(R.styleable.SmartRefreshLayout_srlEnableLoadmore);
		mManualNestedScrolling = ta.hasValue(R.styleable.SmartRefreshLayout_srlEnableNestedScrolling);
		mHeaderHeightStatus = ta.hasValue(R.styleable.SmartRefreshLayout_srlHeaderHeight) ? DimensionStatus.XmlLayoutUnNotify : mHeaderHeightStatus;
		mFooterHeightStatus = ta.hasValue(R.styleable.SmartRefreshLayout_srlFooterHeight) ? DimensionStatus.XmlLayoutUnNotify : mFooterHeightStatus;

		mFooterExtendHeight = (int) Math.max((mFooterHeight * (mHeaderMaxDragRate - 1)), 0);
		mHeaderExtendHeight = (int) Math.max((mHeaderHeight * (mHeaderMaxDragRate - 1)), 0);

		int accentColor = ta.getColor(R.styleable.SmartRefreshLayout_srlAccentColor, 0);
		int primaryColor = ta.getColor(R.styleable.SmartRefreshLayout_srlPrimaryColor, 0);
		if (primaryColor != 0) {
			if (accentColor != 0) {
				mPrimaryColors = new int[]{primaryColor, accentColor};
			} else {
				mPrimaryColors = new int[]{primaryColor};
			}
		}

		ta.recycle();

	}
	//</editor-fold>

	//<editor-fold desc="生命周期 life cycle">

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		Logd(TAG, "onFinish,Inflate: ");
		final int count = getChildCount();
		if (count > 3) {
			throw new RuntimeException("最多只支持3个子View，Most only support three sub view");
		} else if (mEnablePureScrollMode && count > 1) {
			throw new RuntimeException("PureScrollMode模式只支持一个子View，Most only support one sub view in PureScrollMode");
		}
		Logw(TAG, "onFinishInflate: count:" + count);
		//定义为确认的子View索引
		boolean[] uncertains = new boolean[count];
		//第一次查找确认的 子View
		for (int i = 0; i < count; i++) {
			View view = getChildAt(i);
			Logw(TAG, "onFinishInflate: view:" + view);
			if (view instanceof RefreshHeader && mRefreshHeader == null) {
				mRefreshHeader = ((RefreshHeader) view);
			} else if (view instanceof RefreshFooter && mRefreshFooter == null) {
				mEnableLoadmore = mEnableLoadmore || !mManualLoadmore;
				mRefreshFooter = ((RefreshFooter) view);
			} else if (mRefreshContent == null && (view instanceof AbsListView
					|| view instanceof WebView
					|| view instanceof ScrollView
					|| view instanceof ScrollingView
					|| view instanceof NestedScrollingChild
					|| view instanceof NestedScrollingParent
					|| view instanceof ViewPager)) {
				mRefreshContent = new RefreshContentWrapper(view);
			} else if (RefreshHeaderWrapper.isTagedHeader(view) && mRefreshHeader == null) {
				mRefreshHeader = new RefreshHeaderWrapper(view);
			} else if (RefreshFooterWrapper.isTagedFooter(view) && mRefreshFooter == null) {
				mRefreshFooter = new RefreshFooterWrapper(view);
			} else if (RefreshContentWrapper.isTagedContent(view) && mRefreshContent == null) {
				mRefreshContent = new RefreshContentWrapper(view);
			} else {
				uncertains[i] = true;//标记未确认
			}
		}
		//如果有 未确认（uncertains）的子View 通过智能算法计算
		for (int i = 0; i < count; i++) {
			if (uncertains[i]) {
				View view = getChildAt(i);
				Logd(TAG, "onFinishInflate: uncertains:" + view);
				if (count == 1 && mRefreshContent == null) {
					mRefreshContent = new RefreshContentWrapper(view);
				} else if (i == 0 && mRefreshHeader == null) {
					mRefreshHeader = new RefreshHeaderWrapper(view);
				} else if (count == 2 && mRefreshContent == null) {
					mRefreshContent = new RefreshContentWrapper(view);
				} else if (i == 2 && mRefreshFooter == null) {
					mEnableLoadmore = mEnableLoadmore || !mManualLoadmore;
					mRefreshFooter = new RefreshFooterWrapper(view);
				} else if (mRefreshContent == null) {
					mRefreshContent = new RefreshContentWrapper(view);
				}
			}
		}

		if (isInEditMode()) {
			if (mPrimaryColors != null) {
				if (mRefreshHeader != null) {
					mRefreshHeader.setPrimaryColors(mPrimaryColors);
				}
				if (mRefreshFooter != null) {
					mRefreshFooter.setPrimaryColors(mPrimaryColors);
				}
			}

			//重新排序
			// TODO: 2017/10/8 头部、内容、底部排序
			if (mRefreshContent != null) {
				bringChildToFront(mRefreshContent.getView());
			}
			if (mRefreshHeader != null && mRefreshHeader.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
				if (isHeaderFixBehind) {
				} else {
					bringChildToFront(mRefreshHeader.getView());
				}
			}
			if (mRefreshFooter != null && mRefreshFooter.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
				bringChildToFront(mRefreshFooter.getView());
			}

			if (mKernel == null) {
				mKernel = new RefreshKernelImpl();
			}
		}

	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		Logd(TAG, "onFinish,onAttachedToWindow: ");
		if (isInEditMode()) return;

		if (mRefreshContent == null
				&& mRefreshHeader == null
				&& mRefreshFooter == null) {
			onFinishInflate();
		}

		if (mKernel == null) {
			mKernel = new RefreshKernelImpl();
		}

		if (mRefreshContent == null) {
			for (int i = 0, len = getChildCount(); i < len; i++) {
				View view = getChildAt(i);
				if ((mRefreshHeader == null || view != mRefreshHeader.getView()) &&
						(mRefreshFooter == null || view != mRefreshFooter.getView())) {
					mRefreshContent = new RefreshContentWrapper(view);
				}
			}
			if (mRefreshContent == null) {
				mRefreshContent = new RefreshContentWrapper(getContext());
				mRefreshContent.getView().setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));
			}
		}
		if (mFixedHeaderViewId > 0 && mFixedHeaderView == null) {
			mFixedHeaderView = findViewById(mFixedHeaderViewId);
		}
		if (mFixedFooterViewId > 0 && mFixedFooterView == null) {
			mFixedFooterView = findViewById(mFixedFooterViewId);
		}

		mRefreshContent.setupComponent(mKernel, mFixedHeaderView, mFixedFooterView);

		if (mRefreshHeader == null) {
			if (mEnablePureScrollMode) {
//                注释此效果       ckr  by 2017.8.15
//                mRefreshHeader = new FalsifyHeader(getContext());
			} else {
				mRefreshHeader = sHeaderCreater.createRefreshHeader(getContext(), this);
			}
			if (!(mRefreshHeader.getView().getLayoutParams() instanceof MarginLayoutParams)) {
				if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale) {
					addView(mRefreshHeader.getView(), MATCH_PARENT, MATCH_PARENT);
				} else {
					addView(mRefreshHeader.getView(), MATCH_PARENT, WRAP_CONTENT);
				}
			}
		}
		if (mRefreshFooter == null) {
			if (mEnablePureScrollMode) {
				//                注释此效果       ckr  by 2017.8.15
//                mRefreshFooter = new RefreshFooterWrapper(new FalsifyHeader(getContext()));
//                mEnableLoadmore = mEnableLoadmore || !mManualLoadmore;
			} else {
				mRefreshFooter = sFooterCreater.createRefreshFooter(getContext(), this);
				mEnableLoadmore = mEnableLoadmore || (!mManualLoadmore && sManualFooterCreater);
			}
			if (!(mRefreshFooter.getView().getLayoutParams() instanceof MarginLayoutParams)) {
				if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Scale) {
					addView(mRefreshFooter.getView(), MATCH_PARENT, MATCH_PARENT);
				} else {
					addView(mRefreshFooter.getView(), MATCH_PARENT, WRAP_CONTENT);
				}
			}
		}

		//重新排序
		bringChildToFront(mRefreshContent.getView());
		// TODO: 2017/10/8 头部、内容、底部排序
		if (mRefreshHeader.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
			if (isHeaderFixBehind) {

			} else {
				bringChildToFront(mRefreshHeader.getView());
			}
		}
		if (mRefreshFooter.getSpinnerStyle() != SpinnerStyle.FixedBehind) {
			bringChildToFront(mRefreshFooter.getView());
		}

		if (mRefreshListener == null) {
			mRefreshListener = new OnRefreshListener() {
				@Override
				public void onRefresh(RefreshLayout refreshlayout) {
					refreshlayout.finishRefresh(3000);
				}
			};
		}
		if (mLoadmoreListener == null) {
			mLoadmoreListener = new OnLoadmoreListener() {
				@Override
				public void onLoadmore(RefreshLayout refreshlayout) {
					refreshlayout.finishLoadmore(2000);
				}
			};
		}
		if (mPrimaryColors != null) {
			mRefreshHeader.setPrimaryColors(mPrimaryColors);
			mRefreshFooter.setPrimaryColors(mPrimaryColors);
		}

		try {
			if (!mManualNestedScrolling && !isNestedScrollingEnabled()
					&& getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
				setNestedScrollingEnabled(true);
			}
		} catch (Throwable e) {//try 不能删除
		}
	}

	boolean isHeaderFixBehind;

	public void setHeaderFixBehind(boolean b) {
		isHeaderFixBehind = b;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int minimumHeight = 0;
		final boolean isInEditMode = isInEditMode() && mEnablePreviewInEditMode;

		if (mRefreshHeader != null) {
			final View headerView = mRefreshHeader.getView();
			final LayoutParams lp = (LayoutParams) headerView.getLayoutParams();
			final int widthSpec = getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin, lp.width);
			int heightSpec = heightMeasureSpec;

			if (mHeaderHeightStatus.gteReplaceWith(DimensionStatus.XmlLayoutUnNotify)) {
				heightSpec = makeMeasureSpec(Math.max(mHeaderHeight - lp.bottomMargin, 0), EXACTLY);
				headerView.measure(widthSpec, heightSpec);
			} else if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.MatchLayout) {
				headerView.measure(widthSpec, heightSpec);
			} else if (lp.height > 0) {
				if (mHeaderHeightStatus.canReplaceWith(DimensionStatus.XmlExact)) {
					mHeaderHeightStatus = DimensionStatus.XmlExact;
					mHeaderHeight = lp.height + lp.bottomMargin;
					mHeaderExtendHeight = (int) Math.max((mHeaderHeight * (mHeaderMaxDragRate - 1)), 0);
					mRefreshHeader.onInitialized(mKernel, mHeaderHeight, mHeaderExtendHeight);
				}
				heightSpec = makeMeasureSpec(lp.height, EXACTLY);
				headerView.measure(widthSpec, heightSpec);
			} else if (lp.height == WRAP_CONTENT) {
				heightSpec = makeMeasureSpec(Math.max(getSize(heightMeasureSpec) - lp.bottomMargin, 0), AT_MOST);
				headerView.measure(widthSpec, heightSpec);
				int measuredHeight = headerView.getMeasuredHeight();
				if (measuredHeight > 0 && mHeaderHeightStatus.canReplaceWith(DimensionStatus.XmlWrap)) {
					mHeaderHeightStatus = DimensionStatus.XmlWrap;
					mHeaderHeight = headerView.getMeasuredHeight() + lp.bottomMargin;
					mHeaderExtendHeight = (int) Math.max((mHeaderHeight * (mHeaderMaxDragRate - 1)), 0);
					mRefreshHeader.onInitialized(mKernel, mHeaderHeight, mHeaderExtendHeight);
				} else if (measuredHeight <= 0) {
					heightSpec = makeMeasureSpec(Math.max(mHeaderHeight - lp.bottomMargin, 0), EXACTLY);
					headerView.measure(widthSpec, heightSpec);
				}
			} else if (lp.height == MATCH_PARENT) {
				heightSpec = makeMeasureSpec(Math.max(mHeaderHeight - lp.bottomMargin, 0), EXACTLY);
				headerView.measure(widthSpec, heightSpec);
			} else {
				headerView.measure(widthSpec, heightSpec);
			}
			if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale && !isInEditMode) {
				final int height = Math.max(0, mSpinner);
				heightSpec = makeMeasureSpec(Math.max(height - lp.bottomMargin, 0), EXACTLY);
				headerView.measure(widthSpec, heightSpec);
			}

			if (!mHeaderHeightStatus.notifyed) {
				mHeaderHeightStatus = mHeaderHeightStatus.notifyed();
				mRefreshHeader.onInitialized(mKernel, mHeaderHeight, mHeaderExtendHeight);
			}

			if (isInEditMode) {
				minimumHeight += headerView.getMeasuredHeight();
			}
		}

		if (mRefreshFooter != null) {
			final View footerView = mRefreshFooter.getView();
			final LayoutParams lp = (LayoutParams) footerView.getLayoutParams();
			final int widthSpec = getChildMeasureSpec(widthMeasureSpec, lp.leftMargin + lp.rightMargin, lp.width);
			int heightSpec = heightMeasureSpec;
			if (mFooterHeightStatus.gteReplaceWith(DimensionStatus.XmlLayoutUnNotify)) {
				heightSpec = makeMeasureSpec(Math.max(mFooterHeight - lp.topMargin, 0), EXACTLY);
				footerView.measure(widthSpec, heightSpec);
			} else if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.MatchLayout) {
				footerView.measure(widthSpec, heightSpec);
			} else if (lp.height > 0) {
				if (mFooterHeightStatus.canReplaceWith(DimensionStatus.XmlExact)) {
					mFooterHeightStatus = DimensionStatus.XmlExact;
					mFooterHeight = lp.height + lp.topMargin;
					mFooterExtendHeight = (int) Math.max((mFooterHeight * (mFooterMaxDragRate - 1)), 0);
					mRefreshFooter.onInitialized(mKernel, mFooterHeight, mFooterExtendHeight);
				}
				heightSpec = makeMeasureSpec(lp.height - lp.topMargin, EXACTLY);
				footerView.measure(widthSpec, heightSpec);
			} else if (lp.height == WRAP_CONTENT) {
				heightSpec = makeMeasureSpec(Math.max(getSize(heightMeasureSpec) - lp.topMargin, 0), AT_MOST);
				footerView.measure(widthSpec, heightSpec);
				int measuredHeight = footerView.getMeasuredHeight();
				if (measuredHeight > 0 && mFooterHeightStatus.canReplaceWith(DimensionStatus.XmlWrap)) {
					mFooterHeightStatus = DimensionStatus.XmlWrap;
					mFooterHeight = footerView.getMeasuredHeight() + lp.topMargin;
					mFooterExtendHeight = (int) Math.max((mFooterHeight * (mFooterMaxDragRate - 1)), 0);
					mRefreshFooter.onInitialized(mKernel, mFooterHeight, mFooterExtendHeight);
				} else if (measuredHeight <= 0) {
					heightSpec = makeMeasureSpec(Math.max(mFooterHeight - lp.topMargin, 0), EXACTLY);
					footerView.measure(widthSpec, heightSpec);
				}
			} else if (lp.height == MATCH_PARENT) {
				heightSpec = makeMeasureSpec(Math.max(mFooterHeight - lp.topMargin, 0), EXACTLY);
				footerView.measure(widthSpec, heightSpec);
			} else {
				footerView.measure(widthSpec, heightSpec);
			}

			if (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Scale && !isInEditMode) {
				final int height = Math.max(0, -mSpinner);
				heightSpec = makeMeasureSpec(Math.max(height - lp.topMargin, 0), EXACTLY);
				footerView.measure(widthSpec, heightSpec);
			}

			if (!mFooterHeightStatus.notifyed) {
				mFooterHeightStatus = mFooterHeightStatus.notifyed();
				mRefreshFooter.onInitialized(mKernel, mFooterHeight, mFooterExtendHeight);
			}

			if (isInEditMode) {
				minimumHeight += footerView.getMeasuredHeight();
			}
		}

		if (mRefreshContent != null) {
			final LayoutParams lp = (LayoutParams) mRefreshContent.getLayoutParams();
			final int widthSpec = getChildMeasureSpec(widthMeasureSpec,
					getPaddingLeft() + getPaddingRight() +
							lp.leftMargin + lp.rightMargin, lp.width);
			final int heightSpec = getChildMeasureSpec(heightMeasureSpec,
					getPaddingTop() + getPaddingBottom() +
							lp.topMargin + lp.bottomMargin +
							((isInEditMode && mRefreshHeader != null && (mEnableHeaderTranslationContent || mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind)) ? mHeaderHeight : 0) +
							((isInEditMode && mRefreshFooter != null && (mEnableFooterTranslationContent || mRefreshFooter.getSpinnerStyle() == SpinnerStyle.FixedBehind)) ? mFooterHeight : 0), lp.height);
			mRefreshContent.measure(widthSpec, heightSpec);
			mRefreshContent.onInitialHeaderAndFooter(mHeaderHeight, mFooterHeight);
			minimumHeight += mRefreshContent.getMeasuredHeight();
		}

		setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolveSize(minimumHeight, heightMeasureSpec));

		mLastTouchX = getMeasuredWidth() / 2;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int paddingLeft = getPaddingLeft();
		final int paddingTop = getPaddingTop();
		final int paddingBottom = getPaddingBottom();
		final boolean isInEditMode = isInEditMode() && mEnablePreviewInEditMode;

		if (mRefreshContent != null) {
			final LayoutParams lp = (LayoutParams) mRefreshContent.getLayoutParams();
			int left = paddingLeft + lp.leftMargin;
			int top = paddingTop + lp.topMargin;
			int right = left + mRefreshContent.getMeasuredWidth();
			int bottom = top + mRefreshContent.getMeasuredHeight();
			if (isInEditMode && mRefreshHeader != null && (mEnableHeaderTranslationContent || mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind)) {
				top = top + mHeaderHeight;
				bottom = bottom + mHeaderHeight;
			}
			mRefreshContent.layout(left, top, right, bottom);
		}

		if (mRefreshHeader != null) {
			final View headerView = mRefreshHeader.getView();
			final LayoutParams lp = (LayoutParams) headerView.getLayoutParams();
			int left = lp.leftMargin;
			int top = lp.topMargin;
			int right = left + headerView.getMeasuredWidth();
			int bottom = top + headerView.getMeasuredHeight();
			if (!isInEditMode) {
				if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Translate) {
					top = top - mHeaderHeight + Math.max(0, mSpinner);
					bottom = top + headerView.getMeasuredHeight();
				} else if (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale) {
					bottom = top + Math.max(Math.max(0, mSpinner) - lp.bottomMargin, 0);
				}
			}
			headerView.layout(left, top, right, bottom);
		}

		if (mRefreshFooter != null) {
			final View footerView = mRefreshFooter.getView();
			final LayoutParams lp = (LayoutParams) footerView.getLayoutParams();
			final SpinnerStyle style = mRefreshFooter.getSpinnerStyle();
			int left = lp.leftMargin;
			int top = lp.topMargin + getMeasuredHeight();

			if (isInEditMode
					|| style == SpinnerStyle.FixedFront
					|| style == SpinnerStyle.FixedBehind) {
				top = top - mFooterHeight;
			} else if (style == SpinnerStyle.Scale || style == SpinnerStyle.Translate) {
				top = top - Math.max(Math.max(-mSpinner, 0) - lp.topMargin, 0);
			}

			int right = left + footerView.getMeasuredWidth();
			int bottom = top + footerView.getMeasuredHeight();
			footerView.layout(left, top, right, bottom);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mKernel = null;
		mRefreshHeader = null;
		mRefreshFooter = null;
		mRefreshContent = null;
		mFixedHeaderView = null;
		mFixedFooterView = null;
		mManualLoadmore = true;
		mRefreshListener = null;
		mLoadmoreListener = null;
		mManualNestedScrolling = true;
		mOnMultiPurposeListener = null;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		boolean isInEditMode = mEnablePreviewInEditMode && isInEditMode();
		if (mHeaderBackgroundColor != 0 && (mSpinner > 0 || isInEditMode)) {
			mPaint.setColor(mHeaderBackgroundColor);
			canvas.drawRect(0, 0, getWidth(), (isInEditMode) ? mHeaderHeight : mSpinner, mPaint);
		} else if (mFooterBackgroundColor != 0 && (mSpinner < 0 || isInEditMode)) {
			final int height = getHeight();
			mPaint.setColor(mFooterBackgroundColor);
			canvas.drawRect(0, height - (isInEditMode ? (mFooterHeight) : -mSpinner), getWidth(), height, mPaint);
		}
		super.dispatchDraw(canvas);
	}

	//</editor-fold>

	//<editor-fold desc="滑动判断 judgement of slide">
	MotionEvent mFalsifyEvent = null;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return super.onInterceptTouchEvent(ev);
	}

	boolean overSmooth = false;//无法说明
	boolean flag = false;//无法说明

	@Override
	public boolean dispatchTouchEvent(MotionEvent e) {
		Logd(TAG, "dispatchTouchEvent: mNestedScrollInProgress:" + mNestedScrollInProgress);
		//<editor-fold desc="多点触摸计算代码">
		final int action = MotionEventCompat.getActionMasked(e);
		final boolean pointerUp = action == MotionEvent.ACTION_POINTER_UP;
		final int skipIndex = pointerUp ? e.getActionIndex() : -1;

		// Determine focal point
		float sumX = 0, sumY = 0;
		final int count = e.getPointerCount();
		for (int i = 0; i < count; i++) {
			if (skipIndex == i) continue;
			sumX += e.getX(i);
			sumY += e.getY(i);
		}
		final int div = pointerUp ? count - 1 : count;
		final float touchX = sumX / div;
		final float touchY = sumY / div;
		if ((action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_POINTER_DOWN)
				&& mIsBeingDragged) {
			mTouchY += touchY - mLastTouchY;
			Logd(TAG, "dispatchTouchEvent: mTouchY:" + mTouchY);
		}
		mLastTouchX = touchX;
		mLastTouchY = touchY;
		//</editor-fold>

		//<editor-fold>
		if (mRefreshContent != null) {
			//为 RefreshContent 传递当前触摸事件的坐标，用于智能判断对应坐标位置View的滚动边界和相关信息
			switch (action) {
				case MotionEvent.ACTION_DOWN:
					mRefreshContent.onActionDown(e);
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					mRefreshContent.onActionUpOrCancel();
			}
		}
		if ((reboundAnimator != null && !interceptAnimator(action))
				|| (mState == RefreshState.Loading && mDisableContentWhenLoading)
				|| (mState == RefreshState.Refreshing && mDisableContentWhenRefresh)) {
			Logd(TAG, "dispatchTouchEvent, mState:" + mState);
			return false;
		}
		if (mNestedScrollInProgress) {//嵌套滚动时，补充竖直方向不滚动，但是水平方向滚动，需要通知 onHorizontalDrag
			int totalUnconsumed = this.mTotalUnconsumed;
			boolean ret = super.dispatchTouchEvent(e);
			if (action == MotionEvent.ACTION_MOVE && totalUnconsumed == mTotalUnconsumed) {
				final int offsetX = (int) mLastTouchX;
				final int offsetMax = getWidth();
				final float percentX = mLastTouchX / offsetMax;
				if (mSpinner > 0 && mRefreshHeader != null && mRefreshHeader.isSupportHorizontalDrag()) {
					mRefreshHeader.onHorizontalDrag(percentX, offsetX, offsetMax);
				} else if (mSpinner < 0 && mRefreshFooter != null && mRefreshFooter.isSupportHorizontalDrag()) {
					mRefreshFooter.onHorizontalDrag(percentX, offsetX, offsetMax);
				}
			}
			return ret;
		} else if (!isEnabled()
				|| (!mEnableRefresh && !mEnableLoadmore)
				|| (mHeaderNeedTouchEventWhenRefreshing && (mState == RefreshState.Refreshing || mState == RefreshState.RefreshFinish))
				|| (mFooterNeedTouchEventWhenRefreshing && (mState == RefreshState.Loading || mState == RefreshState.LoadFinish))
				) {
			return super.dispatchTouchEvent(e);
		}
		//</editor-fold>

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mTouchX = touchX;
				mTouchY = touchY;
				mLastTouchY = touchY;
				mLastSpinner = 0;
				mTouchSpinner = mSpinner;
				mIsBeingDragged = false;
				super.dispatchTouchEvent(e);
				return true;

			case MotionEvent.ACTION_MOVE:
				Logd(TAG, "dispatchTouchEvent: sumY:" + sumY + ",div:" + div + ",touchY:" + touchY
						+ ",mTouchY:" + mTouchY + ",overSmooth:" + overSmooth);
				float dx = touchX - mTouchX;
				float dy = touchY - mTouchY;
				mLastTouchY = touchY;
				//<editor-fold>
				int currentOffset = 0;
				int totalRange = 0;
				if (mOnOffsetListener != null) {
					currentOffset = mOnOffsetListener.getCurrentOffset();
					totalRange = mOnOffsetListener.getTotalRange();
				}
				other = false;
				if ((!mIsBeingDragged || overSmooth)) {
					Logd(TAG, "dispatchTouchEvent000: canLoadmore:" + mRefreshContent.canLoadmore() + ",canRefresh:" + mRefreshContent.canRefresh()
							+ ", mSpinner:" + mSpinner + ",dy:" + dy + ",dx:" + dx + ",currentOffset:" + currentOffset);
					if (Math.abs(dy) >= mTouchSlop && Math.abs(dx) < Math.abs(dy)
							&& ((currentOffset == 0 && dy >= 0) || (currentOffset == -totalRange && dy <= 0))) {//展开状态时只允许下拉，折叠时只允许上拉
						if (dy > 0 && (mSpinner < 0 || (mEnableRefresh && mRefreshContent.canRefresh()))) {
							Loge(TAG, "dispatchTouchEvent111: mSpinner:" + mSpinner + ",dy:" + dy + ",mState：" + mState + ",mTouchSpinner:" + mTouchSpinner);
							if (mState == RefreshState.Refreshing && mSpinner >= 0) {
								mIsBeingDragged = true;
								if (mTouchSpinner == mHeaderHeight) {
									dy = dy - mTouchSpinner;
								}
							} else {
								if (mSpinner < 0) {
									setStatePullUpToLoad();
								} else {
									setStatePullDownToRefresh();
								}
								mIsBeingDragged = true;
								mTouchY = touchY - mTouchSlop;
								dy = touchY - mTouchY;
								e.setAction(MotionEvent.ACTION_CANCEL);
								super.dispatchTouchEvent(e);
							}
						} else if (dy < 0 && (mSpinner > 0 || (mEnableLoadmore && mRefreshContent.canLoadmore()))) {
							Loge(TAG, "dispatchTouchEvent222: mSpinner:" + mSpinner + ",dy:" + dy + ",mState：" + mState + ",mViceState:" + mViceState);
							if (mSpinner > 0) {
								if ((currentOffset == -totalRange && dy + mTouchSpinner > 0)) {
									Logd(TAG, "dispatchTouchEvent222: overSmooth = false");
									overSmooth = false;
								}
								setStatePullDownToRefresh();
							} else {
								setStatePullUpToLoad();
							}
							mIsBeingDragged = true;
							mTouchY = touchY + mTouchSlop;
							dy = touchY - mTouchY;
							e.setAction(MotionEvent.ACTION_CANCEL);
							super.dispatchTouchEvent(e);
						} else {
							Logd(TAG, "dispatchTouchEvent: 滑动允许最大角度为45度:" + mTouchSpinner + ",mState:" + mState);
							if ((mState != RefreshState.Refreshing)
									&& (mEnableLoadmore && mRefreshContent.canLoadmore()
									|| (currentOffset == 0 && mEnableRefresh && mRefreshContent.canRefresh()))) {
								Logd(TAG, "dispatchTouchEvent: mEnableLoadmore = true");
								overSmooth = false;
								if (dy > 0 && Math.abs(dx) > Math.abs(dy)) {
									if (mSpinner < 0) {
										setStatePullUpToLoad();
									} else {
										setStatePullDownToRefresh();
									}
									mIsBeingDragged = true;
									mTouchY = touchY - mTouchSlop;
									dy = touchY - mTouchY;
									e.setAction(MotionEvent.ACTION_CANCEL);
									super.dispatchTouchEvent(e);
								}
							} else {
								if (mState == RefreshState.Refreshing && (currentOffset == -totalRange)) {
									flag = true;
									Logd(TAG, "dispatchTouchEvent: mIsBeingDragged :" + mViceState + ",spinner:" + (dy + mTouchSpinner));
									long time = e.getEventTime();
									MotionEvent em = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, mTouchX + dx, mTouchY + dy + mTouchSpinner, 0);
									return super.dispatchTouchEvent(em);
								} else {
									return super.dispatchTouchEvent(e);
								}
							}
						}
					} else {
						Logd(TAG, "dispatchTouchEvent: 滑动不允许:" + mTouchSpinner + ",mState:" + mState + ",mViceState:" + mViceState);
						overSmooth = false;
						if ((mState != RefreshState.Refreshing)
								&& (mEnableLoadmore && mRefreshContent.canLoadmore() || (currentOffset == 0 && mEnableRefresh && mRefreshContent.canRefresh()))) {
							Logd(TAG, "dispatchTouchEvent: overSmooth = false");
							if (dy > 0 && Math.abs(dx) > Math.abs(dy)) {
								if (mSpinner < 0) {
									setStatePullUpToLoad();
									mIsBeingDragged = true;
									mTouchY = touchY - mTouchSlop;
									dy = touchY - mTouchY;
									e.setAction(MotionEvent.ACTION_CANCEL);
									super.dispatchTouchEvent(e);
								} else {
									if (currentOffset == 0) {
										setStatePullDownToRefresh();
									}
									mIsBeingDragged = true;
									mTouchY = touchY - mTouchSlop;
									dy = touchY - mTouchY;
									e.setAction(MotionEvent.ACTION_CANCEL);
									super.dispatchTouchEvent(e);
								}
							}
						} else {
							if (mState == RefreshState.Refreshing) {
								Logd(TAG, "dispatchTouchEvent: mIsBeingDragged :" + mState + ",mSpinner;" + mSpinner);
								if (mSpinner > 0) {
									flag = false;
									mIsBeingDragged = true;
									setStatePullDownToRefresh();
								} else {
									if (flag) {
										if (currentOffset == 0) {
											flag = false;
										}
										if (mTouchSpinner > 0) {
											float spinner = dy + mTouchSpinner;
											Loge(TAG, "dispatchTouchEvent: spinner:" + spinner);
											long time = e.getEventTime();
											MotionEvent mFalsifyEvent = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, mTouchX + dx, mTouchY, 0);
											super.dispatchTouchEvent(mFalsifyEvent);
											MotionEvent em = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, mTouchX + dx, mTouchY + spinner, 0);
											return super.dispatchTouchEvent(em);
										} else {
											flag = false;
										}
									}
									return super.dispatchTouchEvent(e);
								}
							} else if (mState == RefreshState.Loading && (currentOffset == -totalRange && mTouchSpinner < 0)) {
								Logd(TAG, "dispatchTouchEvent: mIsBeingDragged :" + mState);
								if (mSpinner < 0) {
									mIsBeingDragged = true;
									setStatePullUpToLoad();
								} else {
									long time = e.getEventTime();
									MotionEvent em = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, mTouchX + dx, mTouchY + dy + mTouchSpinner, 0);
									return super.dispatchTouchEvent(em);
								}
							} else {
								return super.dispatchTouchEvent(e);
							}
						}
					}
				}
				//</editor-fold>
				//<editor-fold>
				if (mIsBeingDragged) {
					final float spinner = dy + mTouchSpinner;
					Logd(TAG, "dispatchTouchEvent333: isHeader:" + getViceState().isHeader() + ",isFooter:" + getViceState().isFooter()
							+ ",spinner:" + spinner + ",mLastSpinner:" + mLastSpinner + ",dy:" + dy + ",mTouchSpinner:" + mTouchSpinner + ",mTouchY：" + mTouchY + ",currentOffset:" + currentOffset);
					if (currentOffset == -totalRange && !mRefreshContent.canLoadmore()) {
						if (mState == RefreshState.Refreshing && (dy + mTouchSpinner > 0)) {
							overSmooth = false;
							other = true;
						} else {
							overSmooth = true;
						}
					}
					if ((mRefreshContent != null)
							&& (getViceState().isHeader() && (spinner < 0 || mLastSpinner < 0))
							|| (getViceState().isFooter() && (spinner > 0 || mLastSpinner > 0))
							|| (currentOffset != 0 && currentOffset != -totalRange)) {
						Loge(TAG, "dispatchTouchEvent333: 进入,mSpinner:" + mSpinner + ",spinner:" + spinner);
						long time = e.getEventTime();
						if (mFalsifyEvent == null) {
							mFalsifyEvent = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, mTouchX + dx, mTouchY, 0);
							super.dispatchTouchEvent(mFalsifyEvent);
						}
						MotionEvent em = MotionEvent.obtain(time, time, MotionEvent.ACTION_MOVE, mTouchX + dx, mTouchY + spinner, 0);
						boolean consume = super.dispatchTouchEvent(em);
						mFalsifyEvent = null;
						if (mSpinner != 0) {
							moveSpinnerInfinitely(0);
						}
						return true;
					}

					Logd(TAG, "dispatchTouchEvent444: mState:" + mState + ",mViceState:" + mViceState + ",isDraging:" + getViceState().isDraging() + ",spinner：" + spinner);
					if (getViceState().isDraging()) {//展开状态时只允许下拉，折叠时只允许上拉
						float moveSpinner = spinner;
						if (currentOffset == 0 && moveSpinner >= 0) {
							moveSpinnerInfinitely(moveSpinner);
							return true;
						} else if (currentOffset == -totalRange) {
							moveSpinnerInfinitely(moveSpinner);
							return true;
						}
					}
				}
				//</editor-fold>
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mIsBeingDragged = false;
				currentOffset = 0;
				totalRange = 0;
				if (mOnOffsetListener != null) {
					currentOffset = mOnOffsetListener.getCurrentOffset();
					totalRange = mOnOffsetListener.getTotalRange();
				}
				Logd(TAG, "dispatchTouchEvent: action:" + e.getAction() + ",mFalsifyEvent:" + mFalsifyEvent + ",currentOffset：" + currentOffset + ",mSpinner：" + mSpinner);
				if (currentOffset == 0) {
					if (mEnableRefresh && mRefreshContent.canRefresh()) {
						boolean overSpinner = overSpinner();
						Logd(TAG, "dispatchTouchEvent: overSpinner:" + overSpinner);
						if (overSpinner) {
							return true;
						}
					}
				} else if (currentOffset == -totalRange) {//解决加载更多即将结束时，重新按压放手后，RefreshFooter 不隐藏
					boolean overSpinner = overSpinner();
					Logd(TAG, "dispatchTouchEvent: overSpinner:" + overSpinner);
					if (overSpinner) {
						return true;
					}
				} else {
					if (action == MotionEvent.ACTION_UP) {//解决刷新或加载更多即将结束时，重新按压放手后，RefreshHeader或RefreshFooter 不隐藏
						boolean overSpinner = overSpinner();
						Logd(TAG, "dispatchTouchEvent: overSpinner:" + overSpinner);
						if (overSpinner) {
							return true;
						}
					}
				}
				break;
		}
		return super.dispatchTouchEvent(e);
	}

	OnOffsetListener mOnOffsetListener;

	public void setOnCollapsingListener(OnOffsetListener mOnOffsetListener) {
		this.mOnOffsetListener = mOnOffsetListener;
	}

	/**
	 * 在动画执行时，触摸屏幕，打断动画，转为拖动状态
	 */
	protected boolean interceptAnimator(int action) {
		if (reboundAnimator != null && action == MotionEvent.ACTION_DOWN) {
			if (mState == RefreshState.PullDownCanceled || mState == RefreshState.RefreshFinish) {
				setStatePullDownToRefresh();
			} else if (mState == RefreshState.PullUpCanceled || mState == RefreshState.LoadFinish) {
				setStatePullUpToLoad();
			}
			reboundAnimator.cancel();
			reboundAnimator = null;
			return true;
		}
		return false;
	}

	@Override
	public void requestDisallowInterceptTouchEvent(boolean b) {
		// if this is a List < L or another view that doesn't support nested
		// scrolling, ignore this request so that the vertical scroll event
		// isn't stolen
		View target = mRefreshContent.getScrollableView();
		if ((Build.VERSION.SDK_INT >= 21 || !(target instanceof AbsListView))
				&& (target == null || ViewCompat.isNestedScrollingEnabled(target))) {
			super.requestDisallowInterceptTouchEvent(b);
			//} else {
			// Nope.
		}
	}

	//</editor-fold>

	//<editor-fold desc="状态更改 state changes">

	protected void notifyStateChanged(RefreshState state) {
		final RefreshState oldState = mState;
		if (oldState != state) {
			mState = state;
			mViceState = RefreshState.None;
			if (mRefreshFooter != null) {
				mRefreshFooter.onStateChanged(this, oldState, state);
			}
			if (mRefreshHeader != null) {
				mRefreshHeader.onStateChanged(this, oldState, state);
			}
			if (mOnMultiPurposeListener != null) {
				mOnMultiPurposeListener.onStateChanged(this, oldState, state);
			}
		}
	}

	protected void setStatePullUpToLoad() {
		if (mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
			notifyStateChanged(RefreshState.PullToUpLoad);
		} else {
			setViceState(RefreshState.PullToUpLoad);
		}
	}

	protected void setStateReleaseToLoad() {
		if (mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
			notifyStateChanged(RefreshState.ReleaseToLoad);
		} else {
			setViceState(RefreshState.ReleaseToLoad);
		}
	}

	protected void setStateReleaseToRefresh() {
		if (mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
			notifyStateChanged(RefreshState.ReleaseToRefresh);
		} else {
			setViceState(RefreshState.ReleaseToRefresh);
		}
	}

	protected void setStatePullDownToRefresh() {
		if (mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
			notifyStateChanged(RefreshState.PullDownToRefresh);
		} else {
			setViceState(RefreshState.PullDownToRefresh);
		}
	}

	protected void setStatePullDownCanceled() {
		if (mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
			notifyStateChanged(RefreshState.PullDownCanceled);
			resetStatus();
		} else {
			setViceState(RefreshState.PullDownCanceled);
		}
	}

	protected void setStatePullUpCanceled() {
		if (mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
			notifyStateChanged(RefreshState.PullUpCanceled);
			resetStatus();
		} else {
			setViceState(RefreshState.PullUpCanceled);
		}
	}

	protected void setStateLodingFinish() {
		notifyStateChanged(RefreshState.LoadFinish);
	}

	protected void setStateRefresingFinish() {
		notifyStateChanged(RefreshState.RefreshFinish);
	}

	protected void setStateLoding() {
		mLastLoadingTime = currentTimeMillis();
		notifyStateChanged(RefreshState.Loading);
		animSpinner(-mFooterHeight);
		if (mLoadmoreListener != null) {
			mLoadmoreListener.onLoadmore(this);
		}
		if (mRefreshFooter != null) {
			mRefreshFooter.onStartAnimator(this, mFooterHeight, mFooterExtendHeight);
		}
		if (mOnMultiPurposeListener != null) {
			mOnMultiPurposeListener.onLoadmore(this);
			mOnMultiPurposeListener.onFooterStartAnimator(mRefreshFooter, mFooterHeight, mFooterExtendHeight);
		}
	}

	protected void setStateRefresing() {
		mLastRefreshingTime = currentTimeMillis();
		Logd(TAG, "setStateRefresing: mLastRefreshingTime:" + mLastRefreshingTime);
		notifyStateChanged(RefreshState.Refreshing);
		animSpinner(mHeaderHeight);
		if (mRefreshListener != null) {
			mRefreshListener.onRefresh(this);
		}
		if (mRefreshHeader != null) {
			mRefreshHeader.onStartAnimator(this, mHeaderHeight, mHeaderExtendHeight);
		}
		if (mOnMultiPurposeListener != null) {
			mOnMultiPurposeListener.onRefresh(this);
			mOnMultiPurposeListener.onHeaderStartAnimator(mRefreshHeader, mHeaderHeight, mHeaderExtendHeight);
		}
	}

	/**
	 * 重置状态
	 */
	protected void resetStatus() {
		if (mState != RefreshState.None) {
			if (mSpinner == 0) {
				notifyStateChanged(RefreshState.None);
			}
		}
		if (mSpinner != 0) {
			animSpinner(0);
		}
	}


	protected RefreshState getViceState() {
		return (mState == RefreshState.Refreshing || mState == RefreshState.Loading) ? mViceState : mState;
	}

	protected void setViceState(RefreshState state) {
		if (mState == RefreshState.Refreshing || mState == RefreshState.Loading) {
			if (mViceState != state) {
				mViceState = state;
			}
		}
	}

	//</editor-fold>

	//<editor-fold desc="视图位移 displacement">

	//<editor-fold desc="动画监听 Animator Listener">
	protected ValueAnimator reboundAnimator;
	protected AnimatorListener reboundAnimatorEndListener = new AnimatorListenerAdapter() {
		@Override
		public void onAnimationEnd(Animator animation) {
			reboundAnimator = null;
			if ((int) ((ValueAnimator) animation).getAnimatedValue() == 0) {
				if (mState != RefreshState.None && mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
					notifyStateChanged(RefreshState.None);
				}
			}
		}
	};

	protected AnimatorUpdateListener reboundUpdateListener = new AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			moveSpinner((int) animation.getAnimatedValue(), true);
		}
	};
	//</editor-fold>

	protected ValueAnimator animSpinner(int endSpinner) {
		return animSpinner(endSpinner, 0);
	}

	protected ValueAnimator animSpinner(int endSpinner, int startDelay) {
		return animSpinner(endSpinner, startDelay, mReboundInterpolator);
	}

	protected ValueAnimator animSpinner(int endSpinner, int startDelay, Interpolator interpolator) {
		Logd(TAG, "animSpinner: 回弹动画：mSpinner:" + mSpinner + ",endSpinner:" + endSpinner);
		if (mSpinner != endSpinner) {
			if (reboundAnimator != null) {
				reboundAnimator.cancel();
			}
			reboundAnimator = ValueAnimator.ofInt(mSpinner, endSpinner);
			reboundAnimator.setDuration(mReboundDuration);
			reboundAnimator.setInterpolator(interpolator);
			reboundAnimator.addUpdateListener(reboundUpdateListener);
			reboundAnimator.addListener(reboundAnimatorEndListener);
			reboundAnimator.setStartDelay(startDelay);
			reboundAnimator.start();
		}
		return reboundAnimator;
	}

	/**
	 * 执行回弹动画
	 */
	protected ValueAnimator animSpinnerBounce(int bounceSpinner) {
		if (reboundAnimator == null) {
			mLastTouchX = getMeasuredWidth() / 2;
			if (mState == RefreshState.Refreshing && bounceSpinner > 0) {
				reboundAnimator = ValueAnimator.ofInt(mSpinner, Math.min(2 * bounceSpinner, mHeaderHeight));
				reboundAnimator.addListener(reboundAnimatorEndListener);
			} else if (mState == RefreshState.Loading && bounceSpinner < 0) {
				reboundAnimator = ValueAnimator.ofInt(mSpinner, Math.max(2 * bounceSpinner, -mFooterHeight));
				reboundAnimator.addListener(reboundAnimatorEndListener);
			} else if (mSpinner == 0 && mEnableOverScrollBounce) {
				if (bounceSpinner > 0) {
					if (mState != RefreshState.Loading) {
						setStatePullDownToRefresh();
					}
					reboundAnimator = ValueAnimator.ofInt(0, Math.min(bounceSpinner, mHeaderHeight + mHeaderExtendHeight));
				} else {
					if (mState != RefreshState.Refreshing) {
						setStatePullUpToLoad();
					}
					reboundAnimator = ValueAnimator.ofInt(0, Math.max(bounceSpinner, -mFooterHeight - mFooterExtendHeight));
				}
				reboundAnimator.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationStart(Animator animation) {
					}

					@Override
					public void onAnimationEnd(Animator animation) {
						reboundAnimator = ValueAnimator.ofInt(mSpinner, 0);
						reboundAnimator.setDuration(mReboundDuration * 2 / 3);
						reboundAnimator.setInterpolator(new DecelerateInterpolator());
						reboundAnimator.addUpdateListener(reboundUpdateListener);
						reboundAnimator.addListener(reboundAnimatorEndListener);
						reboundAnimator.start();
					}
				});
			}
			if (reboundAnimator != null) {
				reboundAnimator.setDuration(mReboundDuration * 2 / 3);
				reboundAnimator.setInterpolator(new DecelerateInterpolator());
				reboundAnimator.addUpdateListener(reboundUpdateListener);
				reboundAnimator.start();
			}
		}
		return reboundAnimator;
	}

	/**
	 * 手势拖动结束
	 * 开始执行回弹动画
	 */
	protected boolean overSpinner() {
		if (mState == RefreshState.Loading) {
			if (mSpinner < -mFooterHeight) {
				mTotalUnconsumed = -mFooterHeight;
				animSpinner(-mFooterHeight);
			} else if (mSpinner > 0) {
				mTotalUnconsumed = 0;
				animSpinner(0);
			} else {
				return false;
			}
		} else if (mState == RefreshState.Refreshing) {
			Logd(TAG, "overSpinner: mSpinner:" + mSpinner + ",mHeaderHeight:" + mHeaderHeight);
			if (mSpinner > mHeaderHeight) {
				mTotalUnconsumed = mHeaderHeight;
				animSpinner(mHeaderHeight);
			} else if (mSpinner < 0) {
				mTotalUnconsumed = 0;
				animSpinner(0);
			} else {
				return false;
			}
		} else if (mState == RefreshState.PullDownToRefresh
				|| (mEnablePureScrollMode && mState == RefreshState.ReleaseToRefresh)) {
			setStatePullDownCanceled();
		} else if (mState == RefreshState.PullToUpLoad
				|| (mEnablePureScrollMode && mState == RefreshState.ReleaseToLoad)) {
			setStatePullUpCanceled();
		} else if (mState == RefreshState.ReleaseToRefresh) {
			Logd(TAG, "overSpinner: ");
			setStateRefresing();
		} else if (mState == RefreshState.ReleaseToLoad) {
			setStateLoding();
		} else if (mSpinner != 0) {
			animSpinner(0);
		} else {
			return false;
		}
		return true;
	}

	boolean other;

	protected void moveSpinnerInfinitely(float dy) {
		if (mState == RefreshState.Refreshing && dy >= 0) {
			if (dy < mHeaderHeight) {
				Loge(TAG, "moveSpinnerInfinitely: Refreshing < mHeaderHeight:" + dy);
				moveSpinner((int) dy, false);
			} else {
				Loge(TAG, "moveSpinnerInfinitely: Refreshing > mHeaderHeight:" + dy);
				final double M = mHeaderExtendHeight;
				final double H = Math.max(mScreenHeightPixels * 4 / 3, getHeight()) - mHeaderHeight;
				final double x = Math.max(0, (dy - mHeaderHeight) * mDragRate);
				final double y = Math.min(M * (1 - Math.pow(100, -x / H)), x);// 公式 y = M(1-40^(-x/H))
				moveSpinner((int) y + mHeaderHeight, false);
			}
		} else if (mState == RefreshState.Loading && dy < 0) {
			if (dy > -mFooterHeight) {
				Loge(TAG, "moveSpinnerInfinitely: Loading > -mFooterHeight:" + dy);
				moveSpinner((int) dy, false);
			} else {
				Loge(TAG, "moveSpinnerInfinitely: Loading < -mFooterHeight:" + dy);
				final double M = mFooterExtendHeight;
				final double H = Math.max(mScreenHeightPixels * 4 / 3, getHeight()) - mFooterHeight;
				final double x = -Math.min(0, (dy + mHeaderHeight) * mDragRate);
				final double y = -Math.min(M * (1 - Math.pow(100, -x / H)), x);// 公式 y = M(1-40^(-x/H))
				moveSpinner((int) y - mFooterHeight, false);
			}
		} else if (dy >= 0) {
			Loge(TAG, "moveSpinnerInfinitely: >=0:" + dy);
			final double M = mHeaderExtendHeight + mHeaderHeight;
			final double H = Math.max(mScreenHeightPixels / 2, getHeight());
			final double x = Math.max(0, dy * mDragRate);
			final double y = Math.min(M * (1 - Math.pow(100, -x / H)), x);// 公式 y = M(1-40^(-x/H))
			moveSpinner((int) y, false);
		} else {
			final double M = mFooterExtendHeight + mFooterHeight;
			final double H = Math.max(mScreenHeightPixels / 2, getHeight());
			final double x = -Math.min(0, dy * mDragRate);
			final double y = -Math.min(M * (1 - Math.pow(100, -x / H)), x);// 公式 y = M(1-40^(-x/H))
			Loge(TAG, "moveSpinnerInfinitely: <= 0：" + y);
			moveSpinner((int) y, false);
		}
	}

	/**
	 * 移动滚动 Scroll
	 * moveSpinner 的取名来自 谷歌官方的 @{@link android.support.v4.widget.SwipeRefreshLayout#moveSpinner(float)}
	 */
	protected void moveSpinner(int spinner, boolean isAnimator) {
		Loge(TAG, "moveSpinner: mSpinner:" + mSpinner + ",spinner:" + spinner);
		if (mSpinner == spinner
				&& (mRefreshHeader == null || !mRefreshHeader.isSupportHorizontalDrag())
				&& (mRefreshFooter == null || !mRefreshFooter.isSupportHorizontalDrag())) {
			return;
		}
		final int oldSpinner = mSpinner;
		this.mSpinner = spinner;
		if (!isAnimator && getViceState().isDraging()) {
			if (mSpinner > mHeaderHeight) {
				setStateReleaseToRefresh();
			} else if (-mSpinner > mFooterHeight && !mLoadmoreFinished) {
				setStateReleaseToLoad();
			} else if (mSpinner < 0 && !mLoadmoreFinished) {
				setStatePullUpToLoad();
			} else if (mSpinner > 0) {
				Logd(TAG, "moveSpinner: mSpinner > 0");
				setStatePullDownToRefresh();
			}
		}
		if (mRefreshContent != null) {
			if (spinner > 0) {
				if (mEnableHeaderTranslationContent || mRefreshHeader == null || mRefreshHeader.getSpinnerStyle() == SpinnerStyle.FixedBehind) {
					Loge(TAG, "moveSpinner: mEnableHeaderTranslationContent:" + mEnableHeaderTranslationContent);
					mRefreshContent.moveSpinner(spinner);
					if (mHeaderBackgroundColor != 0) {
						invalidate();
					}
				}
			} else {
				if (mEnableFooterTranslationContent || mRefreshFooter == null || mRefreshFooter.getSpinnerStyle() == SpinnerStyle.FixedBehind) {
					mRefreshContent.moveSpinner(spinner);
					if (mHeaderBackgroundColor != 0) {
						invalidate();
					}
				}
			}
		}
		Logd(TAG, "moveSpinner: oldSpinner:" + oldSpinner + ",spinner:" + spinner);
		if ((spinner >= 0 || oldSpinner > 0) && mRefreshHeader != null) {
			spinner = Math.max(spinner, 0);
			Logd(TAG, "moveSpinner: mEnableRefresh:" + mEnableRefresh);
			if ((mEnableRefresh || other) || (mState == RefreshState.RefreshFinish && isAnimator)) {
				if (oldSpinner != mSpinner
						&& (mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Scale
						|| mRefreshHeader.getSpinnerStyle() == SpinnerStyle.Translate)) {
					mRefreshHeader.getView().requestLayout();
				}
			}

			final int offset = spinner;
			final int headerHeight = mHeaderHeight;
			final int extendHeight = mHeaderExtendHeight;
			final float percent = 1f * spinner / mHeaderHeight;
			if (isAnimator) {
				mRefreshHeader.onReleasing(percent, offset, headerHeight, extendHeight);
				if (mOnMultiPurposeListener != null) {
					mOnMultiPurposeListener.onHeaderReleasing(mRefreshHeader, percent, offset, headerHeight, extendHeight);
				}
			} else {
				if (mRefreshHeader.isSupportHorizontalDrag()) {
					final int offsetX = (int) mLastTouchX;
					final int offsetMax = getWidth();
					final float percentX = mLastTouchX / offsetMax;
					mRefreshHeader.onHorizontalDrag(percentX, offsetX, offsetMax);
				}
				mRefreshHeader.onPullingDown(percent, offset, headerHeight, extendHeight);
				if (mOnMultiPurposeListener != null) {
					mOnMultiPurposeListener.onHeaderPulling(mRefreshHeader, percent, offset, headerHeight, extendHeight);
				}
			}
		}
		if ((spinner <= 0 || oldSpinner < 0) && mRefreshFooter != null) {
			spinner = Math.min(spinner, 0);
			Logd(TAG, "moveSpinner: mEnableLoadmore:" + mEnableRefresh);
			if (mEnableLoadmore || (mState == RefreshState.LoadFinish && isAnimator)) {
				if (oldSpinner != mSpinner
						&& (mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Scale
						|| mRefreshFooter.getSpinnerStyle() == SpinnerStyle.Translate)) {
					mRefreshFooter.getView().requestLayout();
				}
			}

			final int offset = -spinner;
			final int footerHeight = mFooterHeight;
			final int extendHeight = mFooterExtendHeight;
			final float percent = -spinner * 1f / mFooterHeight;
			if (isAnimator) {
				mRefreshFooter.onPullReleasing(percent, offset, footerHeight, extendHeight);
				if (mOnMultiPurposeListener != null) {
					mOnMultiPurposeListener.onFooterReleasing(mRefreshFooter, percent, offset, footerHeight, extendHeight);
				}
			} else {
				if (mRefreshFooter.isSupportHorizontalDrag()) {
					final int offsetX = (int) mLastTouchX;
					final int offsetMax = getWidth();
					final float percentX = mLastTouchX / offsetMax;
					mRefreshFooter.onHorizontalDrag(percentX, offsetX, offsetMax);
				}
				mRefreshFooter.onPullingUp(percent, offset, footerHeight, extendHeight);
				if (mOnMultiPurposeListener != null) {
					mOnMultiPurposeListener.onFooterPulling(mRefreshFooter, percent, offset, footerHeight, extendHeight);
				}
			}
		}
	}

	//</editor-fold>

	//<editor-fold desc="布局参数 LayoutParams">
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(MATCH_PARENT, MATCH_PARENT);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	public static class LayoutParams extends MarginLayoutParams {

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SmartRefreshLayout_Layout);
			backgroundColor = ta.getColor(R.styleable.SmartRefreshLayout_Layout_srlBackgroundColor, backgroundColor);
			if (ta.hasValue(R.styleable.SmartRefreshLayout_Layout_srlSpinnerStyle)) {
				spinnerStyle = SpinnerStyle.values()[ta.getInt(R.styleable.SmartRefreshLayout_Layout_srlSpinnerStyle, SpinnerStyle.Translate.ordinal())];
			}
			ta.recycle();
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public int backgroundColor = 0;
		public SpinnerStyle spinnerStyle = null;
	}
	//</editor-fold>

	//<editor-fold desc="嵌套滚动 NestedScrolling">

	//<editor-fold desc="NestedScrollingParent">
	@Override
	public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
		boolean accepted = isEnabled() && isNestedScrollingEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
		accepted = accepted && (mEnableRefresh || mEnableLoadmore);
		return accepted;
	}

	@Override
	public void onNestedScrollAccepted(View child, View target, int axes) {
		// Reset the counter of how much leftover scroll needs to be consumed.
		mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
		// Dispatch up to the nested parent
		startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
		mTotalUnconsumed = 0;
		mTouchSpinner = mSpinner;
		mNestedScrollInProgress = true;
	}

	@Override
	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
		// If we are in the middle of consuming, a scroll, then we want to move the spinner back up
		// before allowing the list to scroll
		if (mState == RefreshState.Refreshing || mState == RefreshState.Loading) {
			final int[] parentConsumed = mParentScrollConsumed;
			if (dispatchNestedPreScroll(dx, dy, parentConsumed, null)) {
				dy -= parentConsumed[1];
			}

			//判断 mTotalUnconsumed和dy 同为负数或者正数
			if (mState == RefreshState.Refreshing && (dy * mTotalUnconsumed > 0 || mTouchSpinner > 0)) {
				consumed[1] = 0;
				if (Math.abs(dy) > Math.abs(mTotalUnconsumed)) {
					consumed[1] += mTotalUnconsumed;
					mTotalUnconsumed = 0;
					dy -= mTotalUnconsumed;
					if (mTouchSpinner <= 0) {
						moveSpinnerInfinitely(0);
					}
				} else {
					mTotalUnconsumed -= dy;
					consumed[1] += dy;
					dy = 0;
					moveSpinnerInfinitely(mTotalUnconsumed + mTouchSpinner);
				}

				if (dy > 0 && mTouchSpinner > 0) {
					if (dy > mTouchSpinner) {
						consumed[1] += mTouchSpinner;
						mTouchSpinner = 0;
					} else {
						mTouchSpinner -= dy;
						consumed[1] += dy;
					}
					moveSpinnerInfinitely(mTouchSpinner);
				}
			} else {
				if (mState == RefreshState.Loading && (dy * mTotalUnconsumed > 0 || mTouchSpinner < 0)) {
					consumed[1] = 0;
					if (Math.abs(dy) > Math.abs(mTotalUnconsumed)) {
						consumed[1] += mTotalUnconsumed;
						mTotalUnconsumed = 0;
						dy -= mTotalUnconsumed;
						if (mTouchSpinner >= 0) {
							moveSpinnerInfinitely(0);
						}
					} else {
						mTotalUnconsumed -= dy;
						consumed[1] += dy;
						dy = 0;
						moveSpinnerInfinitely(mTotalUnconsumed + mTouchSpinner);
					}

					if (dy < 0 && mTouchSpinner < 0) {
						if (dy < mTouchSpinner) {
							consumed[1] += mTouchSpinner;
							mTouchSpinner = 0;
						} else {
							mTouchSpinner -= dy;
							consumed[1] += dy;
						}
						moveSpinnerInfinitely(mTouchSpinner);
					}
				}
			}
		} else {
			if (mEnableRefresh && dy > 0 && mTotalUnconsumed > 0) {
				if (dy > mTotalUnconsumed) {
					consumed[1] = dy - mTotalUnconsumed;
					mTotalUnconsumed = 0;
				} else {
					mTotalUnconsumed -= dy;
					consumed[1] = dy;
				}
				moveSpinnerInfinitely(mTotalUnconsumed);
			} else if (mEnableLoadmore && dy < 0 && mTotalUnconsumed < 0) {
				if (dy < mTotalUnconsumed) {
					consumed[1] = dy - mTotalUnconsumed;
					mTotalUnconsumed = 0;
				} else {
					mTotalUnconsumed -= dy;
					consumed[1] = dy;
				}
				moveSpinnerInfinitely(mTotalUnconsumed);
			}

			// If a client layout is using a custom start position for the circle
			// view, they mean to hide it again before scrolling the child view
			// If we get back to mTotalUnconsumed == 0 and there is more to go, hide
			// the circle so it isn't exposed if its blocking content is moved
//        if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
//                && Math.abs(dy - consumed[1]) > 0) {
//            mCircleView.setVisibility(View.GONE);
//        }

			// Now let our nested parent consume the leftovers
			final int[] parentConsumed = mParentScrollConsumed;
			if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
				consumed[0] += parentConsumed[0];
				consumed[1] += parentConsumed[1];
			}
		}

	}

	@Override
	public int getNestedScrollAxes() {
		return mNestedScrollingParentHelper.getNestedScrollAxes();
	}

	@Override
	public void onStopNestedScroll(View target) {
		mNestedScrollingParentHelper.onStopNestedScroll(target);
		mNestedScrollInProgress = false;
		// Finish the spinner for nested scrolling if we ever consumed any
		// unconsumed nested scroll
//        if (mState != RefreshState.Refreshing && mState != RefreshState.Loading) {
//        }
		mTotalUnconsumed = 0;
		overSpinner();
		// Dispatch up our nested parent
		stopNestedScroll();
	}

	@Override
	public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
							   final int dxUnconsumed, final int dyUnconsumed) {
		// Dispatch up to the nested parent first
		dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
				mParentOffsetInWindow);

		// This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
		// sometimes between two nested scrolling views, we need a way to be able to know when any
		// nested scrolling parent has stopped handling events. We do that by using the
		// 'offset in window 'functionality to see if we have been moved from the event.
		// This is a decent indication of whether we should take over the event stream or not.

		final int dy = dyUnconsumed + mParentOffsetInWindow[1];
		if (mState == RefreshState.Refreshing || mState == RefreshState.Loading) {
			if (mEnableRefresh && dy < 0 && (mRefreshContent == null || mRefreshContent.canRefresh())) {
				mTotalUnconsumed += Math.abs(dy);
				moveSpinnerInfinitely(mTotalUnconsumed + mTouchSpinner);
			} else if (mEnableLoadmore && dy > 0 && (mRefreshContent == null || mRefreshContent.canLoadmore())) {
				mTotalUnconsumed -= Math.abs(dy);
				moveSpinnerInfinitely(mTotalUnconsumed + mTouchSpinner);
			}
		} else {
			if (mEnableRefresh && dy < 0 && (mRefreshContent == null || mRefreshContent.canRefresh())) {
				if (mState == RefreshState.None) {
					setStatePullDownToRefresh();
				}
				mTotalUnconsumed += Math.abs(dy);
				moveSpinnerInfinitely(mTotalUnconsumed);
			} else if (mEnableLoadmore && dy > 0
					&& (mRefreshContent == null || mRefreshContent.canLoadmore())) {
				if (mState == RefreshState.None && !mLoadmoreFinished) {
					setStatePullUpToLoad();
				}
				mTotalUnconsumed -= Math.abs(dy);
				moveSpinnerInfinitely(mTotalUnconsumed);
			}
		}
	}

	@Override
	public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
		return reboundAnimator != null || mState == RefreshState.ReleaseToRefresh || mState == RefreshState.ReleaseToLoad || (mState == RefreshState.PullDownToRefresh && mSpinner > 0) || (mState == RefreshState.PullToUpLoad && mSpinner > 0) || (mState == RefreshState.Refreshing && mSpinner != 0) || (mState == RefreshState.Loading && mSpinner != 0) || dispatchNestedPreFling(velocityX, velocityY);
	}

	@Override
	public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
		return dispatchNestedFling(velocityX, velocityY, consumed);
	}
	//</editor-fold>

	//<editor-fold desc="NestedScrollingChild">
	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		mManualNestedScrolling = true;
		mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
	}

	@Override
	public boolean isNestedScrollingEnabled() {
		return mNestedScrollingChildHelper.isNestedScrollingEnabled();
	}

	@Override
	public boolean startNestedScroll(int axes) {
		return mNestedScrollingChildHelper.startNestedScroll(axes);
	}

	@Override
	public void stopNestedScroll() {
		mNestedScrollingChildHelper.stopNestedScroll();
	}

	@Override
	public boolean hasNestedScrollingParent() {
		return mNestedScrollingChildHelper.hasNestedScrollingParent();
	}

	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
										int dyUnconsumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
				dxUnconsumed, dyUnconsumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return mNestedScrollingChildHelper.dispatchNestedPreScroll(
				dx, dy, consumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
	}
	//</editor-fold>

	//</editor-fold>

	//<editor-fold desc="开放接口 open interface">
	@Override
	public SmartRefreshLayout setFooterHeight(float heightDp) {
		return setFooterHeightPx(dp2px(heightDp));
	}

	@Override
	public SmartRefreshLayout setFooterHeightPx(int heightPx) {
		if (mFooterHeightStatus.canReplaceWith(DimensionStatus.CodeExact)) {
			mFooterHeight = heightPx;
			mFooterExtendHeight = (int) Math.max((heightPx * (mFooterMaxDragRate - 1)), 0);
			mFooterHeightStatus = DimensionStatus.CodeExactUnNotify;
			if (mRefreshFooter != null) {
				mRefreshFooter.getView().requestLayout();
			}
		}
		return this;
	}

	@Override
	public SmartRefreshLayout setHeaderHeight(float heightDp) {
		return setHeaderHeightPx(dp2px(heightDp));
	}

	@Override
	public SmartRefreshLayout setHeaderHeightPx(int heightPx) {
		if (mHeaderHeightStatus.canReplaceWith(DimensionStatus.CodeExact)) {
			mHeaderHeight = heightPx;
			mHeaderExtendHeight = (int) Math.max((heightPx * (mHeaderMaxDragRate - 1)), 0);
			mHeaderHeightStatus = DimensionStatus.CodeExactUnNotify;
			if (mRefreshHeader != null) {
				mRefreshHeader.getView().requestLayout();
			}
		}
		return this;
	}

	@Override
	public SmartRefreshLayout setDragRate(float rate) {
		this.mDragRate = rate;
		return this;
	}

	/**
	 * 设置下拉最大高度和Header高度的比率（将会影响可以下拉的最大高度）
	 */
	@Override
	public SmartRefreshLayout setHeaderMaxDragRate(float rate) {
		this.mHeaderMaxDragRate = rate;
		this.mHeaderExtendHeight = (int) Math.max((mHeaderHeight * (mHeaderMaxDragRate - 1)), 0);
		if (mRefreshHeader != null && mKernel != null) {
			mRefreshHeader.onInitialized(mKernel, mHeaderHeight, mHeaderExtendHeight);
		} else {
			mHeaderHeightStatus = mHeaderHeightStatus.unNotify();
		}
		return this;
	}

	/**
	 * 设置上啦最大高度和Footer高度的比率（将会影响可以上啦的最大高度）
	 */
	@Override
	public SmartRefreshLayout setFooterMaxDragRate(float rate) {
		this.mFooterMaxDragRate = rate;
		this.mFooterExtendHeight = (int) Math.max((mFooterHeight * (mFooterMaxDragRate - 1)), 0);
		if (mRefreshFooter != null && mKernel != null) {
			mRefreshFooter.onInitialized(mKernel, mFooterHeight, mFooterExtendHeight);
		} else {
			mFooterHeightStatus = mFooterHeightStatus.unNotify();
		}
		return this;
	}

	/**
	 * 设置回弹显示插值器
	 */
	@Override
	public SmartRefreshLayout setReboundInterpolator(Interpolator interpolator) {
		this.mReboundInterpolator = interpolator;
		return this;
	}

	/**
	 * 设置回弹动画时长
	 */
	@Override
	public SmartRefreshLayout setReboundDuration(int duration) {
		this.mReboundDuration = duration;
		return this;
	}

	/**
	 * 设置是否启用上啦加载更多（默认启用）
	 */
	@Override
	public SmartRefreshLayout setEnableLoadmore(boolean enable) {
		this.mManualLoadmore = true;
		this.mEnableLoadmore = enable;
		return this;
	}

	/**
	 * 是否启用下拉刷新（默认启用）
	 */
	@Override
	public SmartRefreshLayout setEnableRefresh(boolean enable) {
		this.mEnableRefresh = enable;
		return this;
	}

	/**
	 * 设置是否启用内容视图拖动效果
	 */
	@Override
	public SmartRefreshLayout setEnableHeaderTranslationContent(boolean enable) {
		this.mEnableHeaderTranslationContent = enable;
		return this;
	}

	/**
	 * 设置是否启用内容视图拖动效果
	 */
	@Override
	public SmartRefreshLayout setEnableFooterTranslationContent(boolean enable) {
		this.mEnableFooterTranslationContent = enable;
		return this;
	}

	/**
	 * 设置是否开启在刷新时候禁止操作内容视图
	 */
	@Override
	public SmartRefreshLayout setDisableContentWhenRefresh(boolean disable) {
		this.mDisableContentWhenRefresh = disable;
		return this;
	}

	/**
	 * 设置是否开启在加载时候禁止操作内容视图
	 */
	@Override
	public SmartRefreshLayout setDisableContentWhenLoading(boolean disable) {
		this.mDisableContentWhenLoading = disable;
		return this;
	}

	/**
	 * 设置是否监听列表在滚动到底部时触发加载事件
	 */
	@Override
	public SmartRefreshLayout setEnableAutoLoadmore(boolean enable) {
		this.mEnableAutoLoadmore = enable;
		return this;
	}

	/**
	 * 设置是否启用越界回弹
	 */
	@Override
	public SmartRefreshLayout setEnableOverScrollBounce(boolean enable) {
		this.mEnableOverScrollBounce = enable;
		return this;
	}

	/**
	 * 设置是否开启纯滚动模式
	 */
	@Override
	public SmartRefreshLayout setEnablePureScrollMode(boolean enable) {
		this.mEnablePureScrollMode = enable;
		return this;
	}

	/**
	 * 设置是否在加载更多完成之后滚动内容显示新数据
	 */
	@Override
	public SmartRefreshLayout setEnableScrollContentWhenLoaded(boolean enable) {
		this.mEnableScrollContentWhenLoaded = enable;
		return this;
	}

	/**
	 * 设置指定的Header
	 */
	@Override
	public SmartRefreshLayout setRefreshHeader(RefreshHeader header) {
		if (mRefreshHeader != null) {
			removeView(mRefreshHeader.getView());
		}
		this.mRefreshHeader = header;
		this.mHeaderHeightStatus = mHeaderHeightStatus.unNotify();
		this.addView(mRefreshHeader.getView());
		return this;
	}

	/**
	 * 设置指定的Header
	 */
	@Override
	public SmartRefreshLayout setRefreshHeader(RefreshHeader header, int width, int height) {
		if (mRefreshHeader != null) {
			removeView(mRefreshHeader.getView());
		}
		this.mRefreshHeader = header;
		this.mHeaderHeightStatus = mHeaderHeightStatus.unNotify();
		this.addView(mRefreshHeader.getView(), width, height);
		return this;
	}

	/**
	 * 设置指定的Footer
	 */
	@Override
	public SmartRefreshLayout setRefreshFooter(RefreshFooter footer) {
		if (mRefreshFooter != null) {
			removeView(mRefreshFooter.getView());
		}
		this.mRefreshFooter = footer;
		this.mFooterHeightStatus = mFooterHeightStatus.unNotify();
		this.mEnableLoadmore = !mManualLoadmore || mEnableLoadmore;
		this.addView(mRefreshFooter.getView());
		return this;
	}

	/**
	 * 设置指定的Footer
	 */
	@Override
	public SmartRefreshLayout setRefreshFooter(RefreshFooter footer, int width, int height) {
		if (mRefreshFooter != null) {
			removeView(mRefreshFooter.getView());
		}
		this.mRefreshFooter = footer;
		this.mFooterHeightStatus = mFooterHeightStatus.unNotify();
		this.mEnableLoadmore = !mManualLoadmore || mEnableLoadmore;
		this.addView(mRefreshFooter.getView(), width, height);
		return this;
	}

	/**
	 * 获取底部上啦组件的实现
	 */
	@Nullable
	@Override
	public RefreshFooter getRefreshFooter() {
		return mRefreshFooter;
	}

	/**
	 * 获取顶部下拉组件的实现
	 */
	@Nullable
	@Override
	public RefreshHeader getRefreshHeader() {
		return mRefreshHeader;
	}

	/**
	 * 获取状态
	 */
	@Override
	public RefreshState getState() {
		return mState;
	}

	/**
	 * 获取实体布局视图
	 */
	@Override
	public SmartRefreshLayout getLayout() {
		return this;
	}

	/**
	 * 单独设置刷新监听器
	 */
	@Override
	public SmartRefreshLayout setOnRefreshListener(OnRefreshListener listener) {
		this.mRefreshListener = listener;
		return this;
	}

	/**
	 * 单独设置加载监听器
	 */
	@Override
	public SmartRefreshLayout setOnLoadmoreListener(OnLoadmoreListener listener) {
		this.mLoadmoreListener = listener;
		this.mEnableLoadmore = mEnableLoadmore || !mManualLoadmore;
		return this;
	}

	/**
	 * 同时设置刷新和加载监听器
	 */
	@Override
	public SmartRefreshLayout setOnRefreshLoadmoreListener(OnRefreshLoadmoreListener listener) {
		this.mRefreshListener = listener;
		this.mLoadmoreListener = listener;
		this.mEnableLoadmore = mEnableLoadmore || !mManualLoadmore;
		return this;
	}

	/**
	 * 设置多功能监听器
	 */
	@Override
	public SmartRefreshLayout setOnMultiPurposeListener(OnMultiPurposeListener listener) {
		this.mOnMultiPurposeListener = listener;
		return this;
	}

	/**
	 * 设置主题颜色
	 */
	@Override
	public SmartRefreshLayout setPrimaryColorsId(@ColorRes int... primaryColorId) {
		int[] colors = new int[primaryColorId.length];
		for (int i = 0; i < primaryColorId.length; i++) {
			colors[i] = ContextCompat.getColor(getContext(), primaryColorId[i]);
		}
		setPrimaryColors(colors);
		return this;
	}

	/**
	 * 设置主题颜色
	 */
	@Override
	public SmartRefreshLayout setPrimaryColors(int... colors) {
		if (mRefreshHeader != null) {
			mRefreshHeader.setPrimaryColors(colors);
		}
		if (mRefreshFooter != null) {
			mRefreshFooter.setPrimaryColors(colors);
		}
		mPrimaryColors = colors;
		return this;
	}

	/**
	 * 设置滚动边界
	 */
	@Override
	public RefreshLayout setRefreshScrollBoundary(RefreshScrollBoundary boundary) {
		if (mRefreshContent != null) {
			mRefreshContent.setRefreshScrollBoundary(boundary);
		}
		return this;
	}

	/**
	 * 设置数据全部加载完成，将不能再次触发加载功能
	 */
	@Override
	public SmartRefreshLayout setLoadmoreFinished(boolean finished) {
		if (finished != mLoadmoreFinished && mRefreshFooter != null) {
			mLoadmoreFinished = finished;
			mRefreshFooter.setLoadmoreFinished(finished);
		} else {
			mLoadmoreFinished = finished;
		}
		return this;
	}

	/**
	 * 完成刷新
	 */
	@Override
	public SmartRefreshLayout finishRefresh() {
		long passTime = System.currentTimeMillis() - mLastRefreshingTime;
		Logd(TAG, "finishRefresh111: passTime:" + passTime + ",mLastRefreshTime:" + mLastRefreshingTime);
		return finishRefresh(Math.max(0, 1000 - (int) passTime));//保证刷新动画有1000毫秒的时间
	}

	/**
	 * 完成加载
	 */
	@Override
	public SmartRefreshLayout finishLoadmore() {
		long passTime = System.currentTimeMillis() - mLastLoadingTime;
		return finishLoadmore(Math.max(0, 1000 - (int) passTime));//保证加载动画有1000毫秒的时间
	}

	/**
	 * 完成刷新
	 */
	@Override
	public SmartRefreshLayout finishRefresh(int delayed) {
		return finishRefresh(delayed, true);
	}

	/**
	 * 完成刷新
	 */
	// TODO: 2017/9/9 刷新时间差小于1000毫秒有bug
	@Override
	public SmartRefreshLayout finishRefresh(boolean success) {
		long passTime = System.currentTimeMillis() - mLastRefreshingTime;
		return finishRefresh(Math.max(0, 1000 - (int) passTime), success);
	}

	private static final String TAG = "SmartRefreshLayout";

	/**
	 * 完成刷新
	 */
	// TODO: 2017/9/9 有bug，view.postDelayed()不执行
	@Override
	public SmartRefreshLayout finishRefresh(int delayed, final boolean success) {
		Logd(TAG, "finishRefresh() called with: delayed = [" + delayed + "], success = [" + success + "]");
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Logd(TAG, "run: 状态：mState:" + mState);
				if (mState == RefreshState.Refreshing) {
					Logd(TAG, "run: 刷新状态");
					if (mRefreshHeader != null) {
						Logd(TAG, "run: 头部不为null");
						int startDelay = mRefreshHeader.onFinish(SmartRefreshLayout.this, success);
						notifyStateChanged(RefreshState.RefreshFinish);
						if (mOnMultiPurposeListener != null) {
							mOnMultiPurposeListener.onHeaderFinish(mRefreshHeader, success);
						}
						if (startDelay < Integer.MAX_VALUE) {
							if (mSpinner == 0) {
								Logd(TAG, "run: 重置状态");
								resetStatus();
							} else {
								Logd(TAG, "run: startDelay");
								animSpinner(0, startDelay);
							}
						}
					} else {
						Logd(TAG, "run: 头部为null");
						resetStatus();
					}
				}
			}
		};
		if (delayed > 0) {
			postDelayed2(runnable, delayed);
		} else {
			runnable.run();
		}
		return this;
	}

	/**
	 * 完成加载
	 */
	@Override
	public SmartRefreshLayout finishLoadmore(int delayed) {
		return finishLoadmore(delayed, true);
	}

	/**
	 * 完成加载
	 */
	@Override
	public SmartRefreshLayout finishLoadmore(boolean success) {
		long passTime = System.currentTimeMillis() - mLastLoadingTime;
		return finishLoadmore(Math.max(0, 1000 - (int) passTime), success);
	}

	/**
	 * 完成加载
	 */
	@Override
	public SmartRefreshLayout finishLoadmore(int delayed, final boolean success) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (mState == RefreshState.Loading) {
					if (mRefreshFooter != null && mKernel != null) {
						int startDelay = mRefreshFooter.onFinish(SmartRefreshLayout.this, success);
						if (startDelay == Integer.MAX_VALUE) {
							return;
						}
						notifyStateChanged(RefreshState.LoadFinish);
						AnimatorUpdateListener updateListener = mRefreshContent.onLoadingFinish(mKernel, mFooterHeight, startDelay, mReboundInterpolator, mReboundDuration);
						if (mOnMultiPurposeListener != null) {
							mOnMultiPurposeListener.onFooterFinish(mRefreshFooter, success);
						}
						if (mSpinner == 0) {
							resetStatus();
						} else {
							ValueAnimator valueAnimator = animSpinner(0, startDelay);
							if (updateListener != null && valueAnimator != null) {
								valueAnimator.addUpdateListener(updateListener);
							}
						}
					} else {
						resetStatus();
					}
				}
			}
		};
		if (delayed > 0) {
			postDelayed2(runnable, delayed);
		} else {
			runnable.run();
		}
		return this;
	}

	/**
	 * 是否正在刷新
	 */
	@Override
	public boolean isRefreshing() {
		return mState == RefreshState.Refreshing;
	}

	/**
	 * 是否正在加载
	 */
	@Override
	public boolean isLoading() {
		return mState == RefreshState.Loading;
	}

	/**
	 * 自动刷新
	 */
	@Override
	public boolean autoRefresh() {
		return autoRefresh(500);
	}

	/**
	 * 自动刷新
	 */
	@Override
	public boolean autoRefresh(int delayed) {
		return autoRefresh(delayed, 1f * (mHeaderHeight + mHeaderExtendHeight / 2) / mHeaderHeight);
	}

	/**
	 * 自动刷新
	 */
	@Override
	public boolean autoRefresh(int delayed, final float dragrate) {
		Logd(TAG, "autoRefresh: drag_rate:" + dragrate + ",mState:" + mState + ",mEnableRefresh:" + mEnableRefresh);
		if (mState == RefreshState.None && mEnableRefresh) {
			if (reboundAnimator != null) {
				reboundAnimator.cancel();
			}
			reboundAnimator = new ValueAnimator();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					reboundAnimator = ValueAnimator.ofInt(mSpinner, (int) (mHeaderHeight * dragrate));
					reboundAnimator.setDuration(mReboundDuration);
					reboundAnimator.setInterpolator(new DecelerateInterpolator());
					reboundAnimator.addUpdateListener(new AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							moveSpinner((int) animation.getAnimatedValue(), false);
						}
					});
					reboundAnimator.addListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationStart(Animator animation) {
							mLastTouchX = getMeasuredWidth() / 2;
							setStatePullDownToRefresh();
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							reboundAnimator = null;
							if (mState != RefreshState.ReleaseToRefresh) {
								setStateReleaseToRefresh();
							}
							overSpinner();
						}
					});
					reboundAnimator.start();
				}
			};
			Logd(TAG, "autoRefresh: delayed:" + delayed);
			if (delayed > 0) {
				postDelayed2(runnable, delayed);
			} else {
				runnable.run();
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 自动加载
	 */
	@Override
	public boolean autoLoadmore() {
		return autoLoadmore(500);
	}

	/**
	 * 自动加载
	 */
	@Override
	public boolean autoLoadmore(int delayed) {
		return autoLoadmore(delayed, 1f * (mFooterHeight + mFooterExtendHeight / 2) / mFooterHeight);
	}

	/**
	 * 自动加载
	 */
	@Override
	public boolean autoLoadmore(int delayed, final float dragrate) {
		if (mState == RefreshState.None && (mEnableLoadmore && !mLoadmoreFinished)) {
			if (reboundAnimator != null) {
				reboundAnimator.cancel();
			}
			reboundAnimator = new ValueAnimator();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					reboundAnimator = ValueAnimator.ofInt(mSpinner, -(int) (mFooterHeight * dragrate));
					reboundAnimator.setDuration(mReboundDuration);
					reboundAnimator.setInterpolator(new DecelerateInterpolator());
					reboundAnimator.addUpdateListener(new AnimatorUpdateListener() {
						@Override
						public void onAnimationUpdate(ValueAnimator animation) {
							moveSpinner((int) animation.getAnimatedValue(), false);
						}
					});
					reboundAnimator.addListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationStart(Animator animation) {
							mLastTouchX = getMeasuredWidth() / 2;
							setStatePullUpToLoad();
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							reboundAnimator = null;
							if (mState != RefreshState.ReleaseToLoad) {
								setStateReleaseToLoad();
							}
							overSpinner();
						}
					});
					reboundAnimator.start();
				}
			};
			if (delayed > 0) {
				postDelayed(runnable, delayed);
			} else {
				runnable.run();
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isEnableLoadmore() {
		return mEnableLoadmore;
	}

	@Override
	public boolean isLoadmoreFinished() {
		return mLoadmoreFinished;
	}

	@Override
	public boolean isEnableAutoLoadmore() {
		return mEnableAutoLoadmore;
	}

	@Override
	public boolean isEnableRefresh() {
		return mEnableRefresh;
	}

	@Override
	public boolean isEnableOverScrollBounce() {
		return mEnableOverScrollBounce;
	}

	@Override
	public boolean isEnablePureScrollMode() {
		return mEnablePureScrollMode;
	}

	@Override
	public boolean isEnableScrollContentWhenLoaded() {
		return mEnableScrollContentWhenLoaded;
	}

	/**
	 * 设置默认Header构建器
	 */
	public static void setDefaultRefreshHeaderCreater(@NonNull DefaultRefreshHeaderCreater creater) {
		sHeaderCreater = creater;
	}

	/**
	 * 设置默认Footer构建器
	 */
	public static void setDefaultRefreshFooterCreater(@NonNull DefaultRefreshFooterCreater creater) {
		sFooterCreater = creater;
		sManualFooterCreater = true;
	}

	//</editor-fold>

	//<editor-fold desc="核心接口 RefreshKernel">
	protected class RefreshKernelImpl implements RefreshKernel {
		@NonNull
		@Override
		public RefreshLayout getRefreshLayout() {
			return SmartRefreshLayout.this;
		}

		@NonNull
		@Override
		public RefreshContent getRefreshContent() {
			return SmartRefreshLayout.this.mRefreshContent;
		}

		//<editor-fold desc="状态更改 state changes">
		public RefreshKernel setStatePullUpToLoad() {
			SmartRefreshLayout.this.setStatePullUpToLoad();
			return this;
		}

		public RefreshKernel setStateReleaseToLoad() {
			SmartRefreshLayout.this.setStateReleaseToLoad();
			return this;
		}

		public RefreshKernel setStateReleaseToRefresh() {
			SmartRefreshLayout.this.setStateReleaseToRefresh();
			return this;
		}

		public RefreshKernel setStatePullDownToRefresh() {
			SmartRefreshLayout.this.setStatePullDownToRefresh();
			return this;
		}

		public RefreshKernel setStatePullDownCanceled() {
			SmartRefreshLayout.this.setStatePullDownCanceled();
			return this;
		}

		public RefreshKernel setStatePullUpCanceled() {
			SmartRefreshLayout.this.setStatePullUpCanceled();
			return this;
		}

		public RefreshKernel setStateLoding() {
			SmartRefreshLayout.this.setStateLoding();
			return this;
		}

		public RefreshKernel setStateRefresing() {
			SmartRefreshLayout.this.setStateRefresing();
			return this;
		}

		@Override
		public RefreshKernel setStateLodingFinish() {
			SmartRefreshLayout.this.setStateLodingFinish();
			return this;
		}

		@Override
		public RefreshKernel setStateRefresingFinish() {
			SmartRefreshLayout.this.setStateRefresingFinish();
			return this;
		}

		public RefreshKernel resetStatus() {
			SmartRefreshLayout.this.resetStatus();
			return this;
		}
		//</editor-fold>

		//<editor-fold desc="视图位移 Spinner">
		public RefreshKernel overSpinner() {
			SmartRefreshLayout.this.overSpinner();
			return this;
		}

		public RefreshKernel moveSpinnerInfinitely(float dy) {
			SmartRefreshLayout.this.moveSpinnerInfinitely(dy);
			return this;
		}

		public RefreshKernel moveSpinner(int spinner, boolean isAnimator) {
			SmartRefreshLayout.this.moveSpinner(spinner, isAnimator);
			return this;
		}

		public RefreshKernel animSpinner(int endSpinner) {
			SmartRefreshLayout.this.animSpinner(endSpinner);
			return this;
		}

		@Override
		public RefreshKernel animSpinnerBounce(int bounceSpinner) {
			SmartRefreshLayout.this.animSpinnerBounce(bounceSpinner);
			return this;
		}

		@Override
		public int getSpinner() {
			return mSpinner;
		}
		//</editor-fold>

		//<editor-fold desc="绘制背景 Backgound">
		@Override
		public RefreshKernel requestDrawBackgoundForHeader(int backgroundColor) {
			if (mPaint == null && backgroundColor != 0) {
				mPaint = new Paint();
			}
			mHeaderBackgroundColor = backgroundColor;
			return this;
		}

		@Override
		public RefreshKernel requestDrawBackgoundForFooter(int backgroundColor) {
			if (mPaint == null && backgroundColor != 0) {
				mPaint = new Paint();
			}
			mFooterBackgroundColor = backgroundColor;
			return this;
		}
		//</editor-fold>

		//<editor-fold desc="注册事件">
		@Override
		public RefreshKernel requestHeaderNeedTouchEventWhenRefreshing(boolean request) {
			mHeaderNeedTouchEventWhenRefreshing = request;
			return this;
		}

		@Override
		public RefreshKernel requestFooterNeedTouchEventWhenLoading(boolean request) {
			mFooterNeedTouchEventWhenRefreshing = request;
			return this;
		}
		//</editor-fold>
	}
	//</editor-fold>

	//<editor-fold desc="postDelayed 优化,防止内存泄漏">
	@Override
	public boolean post(Runnable action) {
		return super.post(new PostRunable(action));
	}

	@Override
	public boolean postDelayed(Runnable action, long delayMillis) {
		return super.postDelayed(new PostRunable(action), delayMillis);
	}

	public boolean postDelayed2(Runnable action, long delayMillis) {
		return super.postDelayed(action, delayMillis);
	}
	//</editor-fold>
}