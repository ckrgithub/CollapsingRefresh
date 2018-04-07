# CollapsingRefresh
AppBarLayout+ViewPager+RecyclerView的刷新功能。最低支持api 16，recyclerView:26.1.0。

## 效果演示
![](screenRecorder/Screenshot_2.gif)

## Demo
[下载 APK](apk/app-debug.apk)

## 功能及使用
### 1.布局引用
```
     <com.scwang.smartrefresh.SmartRefreshLayout
	    xmlns:android="http://schemas.android.com/apk/res/android"
	    xmlns:app="http://schemas.android.com/apk/res-auto"
	    android:id="@+id/refreshLayout"
	    android:layout_width="match_parent"
	    android:layout_height="match_parent"
	    app:srlDisableContentWhenLoading="true"
	    app:srlDisableContentWhenRefresh="true"
	    app:srlEnableAutoLoadmore="false"
	    app:srlEnableHeaderTranslationContent="true"
	    app:srlEnableLoadmore="true">
	
	    <com.scwang.smartrefresh.header.ClassicsHeader
	        android:id="@+id/classicsHeader"
	        android:layout_width="match_parent"
	        android:layout_height="wrap_content"
	        app:srlClassicsSpinnerStyle="Translate"/>
	
	    <com.ckr.smoothappbarlayout.SmoothRecyclerView
	        android:id="@+id/recyclerView"
	        android:layout_width="match_parent"
	        android:layout_height="wrap_content"
	        android:clipToPadding="false"
	        android:paddingBottom="5dp"
	        android:paddingLeft="5dp"
	        android:paddingRight="5dp"
	        android:paddingTop="0dp"/>
	
	    <com.scwang.smartrefresh.footer.ClassicsFooter
	        android:layout_width="match_parent"
	        android:layout_height="wrap_content"
	        app:srlClassicsSpinnerStyle="Translate"/>
	</com.scwang.smartrefresh.SmartRefreshLayout>
```
### 2.代码使用
* **设置监听器**
```
	appBarLayout.addOnOffsetChangedListener(this);
	recyclerView.setOnSmoothScrollListener(appBarLayout);
	smartRefreshLayout.setOnCollapsingListener(this);
```
* **是否允许刷新**
```
	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		this.verticalOffset = verticalOffset;
		Logd(TAG, "onOffsetChanged: verticalOffset:" + this.verticalOffset);
		if (verticalOffset != 0) {
			boolean enableRefresh = smartRefreshLayout.isEnableRefresh();
			if (enableRefresh) {
				smartRefreshLayout.setEnableRefresh(false);
			}
		} else {
			smartRefreshLayout.setEnableRefresh(true);
		}
	}
```

## 感谢
[SmartRefreshLayout](https://github.com/scwang90/SmartRefreshLayout)

[smooth-app-bar-layout](https://github.com/henrytao-me/smooth-app-bar-layout)

## 我的开源项目
[PageRecyclerView](https://github.com/ckrgithub/PageRecyclerView)：自定义RecyclerView实现翻页功能及无限轮播

[FlexItemDecoration](https://github.com/ckrgithub/FlexItemDecoration)：recyclerView分割线的绘制

## 更新日志
* **1.0.1-release(进行中)**
  * 重构代码

* **1.0.1-beta**
  * 实现AppBarLayout+ViewPager+RecyclerView的下拉刷新和上拉加载功能

License
-------

    Copyright 2018 ckrgithub

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
