# CollapsingRefresh
CollapsingToolbarLayout+ViewPager+RecyclerView的刷新功能。

## 效果演示
| ![](screenRecorder/Screenshot_2.gif) |

## Demo
[下载 APK](apk/app-debug.apk)

## 布局引用
```
     <com.ckr.smartrefresh.SmartRefreshLayout
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
	
	    <com.ckr.smartrefresh.header.ClassicsHeader
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
	
	    <com.ckr.smartrefresh.footer.ClassicsFooter
	        android:layout_width="match_parent"
	        android:layout_height="wrap_content"
	        app:srlClassicsSpinnerStyle="Translate"/>
	</com.ckr.smartrefresh.SmartRefreshLayout>
```

## 感谢
[SmartRefreshLayout](https://github.com/scwang90/SmartRefreshLayout)

[smooth-app-bar-layout](https://github.com/henrytao-me/smooth-app-bar-layout)

## 更新日志
* **1.0.1-beta(进行中)**
  * 重构代码

* **1.0.1-alpha**
  * 实现CollapsingToolbarLayout+ViewPager+RecyclerView的下拉刷新和上拉加载功能

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
