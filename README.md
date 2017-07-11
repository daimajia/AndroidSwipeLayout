# Android Swipe Layout [![Build Status](https://travis-ci.org/daimajia/AndroidSwipeLayout.svg?branch=master)](https://travis-ci.org/daimajia/AndroidSwipeLayout)

[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/daimajia/AndroidSwipeLayout?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Insight.io](https://insight.io/repoBadge/github.com/daimajia/AndroidSwipeLayout)](https://insight.io/github.com/daimajia/AndroidSwipeLayout)

This is the brother of [AndroidViewHover](https://github.com/daimajia/AndroidViewHover).

One year ago, I started to make an app named [EverMemo](https://play.google.com/store/apps/details?id=com.zhan_dui.evermemo) with my good friends. The designer gave me a design picture, the design like this:

![](http://ww1.sinaimg.cn/mw690/610dc034jw1ejoquidvvsg208i0630u4.gif)

I found it was pretty hard to achieve this effect, cause you had to be very familiar with the Android Touch System. It was beyond my ability that moment, and I also noticed that there was no such a concept library...

Time passed, finally...as you see right now.

## Demo

![](http://ww2.sinaimg.cn/mw690/610dc034jw1ejoplapwtqg208n0e74dx.gif)

[Download Demo](https://github.com/daimajia/AndroidSwipeLayout/releases/download/v1.1.8/AndroidSwipeLayout-v1.1.8.apk)

Before I made this, I actually found some libraries (eg.[SwipeListView](https://github.com/47deg/android-swipelistview)) that helps developers to integrate swiping with your UI component. 

But it only works in `ListView`, and it has too many issues that they never care. What a pity!

When I start to make this library, I set some goals:

- Can be easily integrated in anywhere, ListView, GridView, ViewGroup etc.
- Can receive `onOpen`,`onClose`,`onUpdate` callbacks.
- Can notifiy the hidden children how much they have shown.
- Can be nested each other.
- Can handle complicate situation, just like [this](https://camo.githubusercontent.com/d145d9a9508b3d204b70882c05bc3d9bd433883c/687474703a2f2f7777312e73696e61696d672e636e2f6c617267652f3631306463303334677731656b686f6a7379326172673230386530366e6774312e676966).


## Usage

### Step 1
#### Gradle

```groovy
dependencies {
    compile 'com.android.support:recyclerview-v7:21.0.0'
    compile 'com.android.support:support-v4:20.+'
    compile "com.daimajia.swipelayout:library:1.2.0@aar"
}
```

#### Maven

```xml
<dependency>
	<groupId>com.google.android</groupId>
	<artifactId>support-v4</artifactId>
	<version>r6</version>
</dependency>
<dependency>
	<groupId>com.google.android</groupId>
	<artifactId>recyclerview-v7</artifactId>
	<version>21.0.0</version>
</dependency>
<dependency>
    <groupId>com.daimajia.swipelayout</groupId>
    <artifactId>library</artifactId>
    <version>1.2.0</version>
    <type>apklib</type>
</dependency>
```

#### Eclipse

[AndroidSwipeLayout-v1.1.8.jar](https://github.com/daimajia/AndroidSwipeLayout/releases/download/v1.1.8/AndroidSwipeLayout-v1.1.8.jar)

### Step 2

**Make sure to use the internal adapter instead of your own!**

[Wiki Usage](https://github.com/daimajia/AndroidSwipeLayout/wiki/usage)

## Wiki

[Go to Wiki](https://github.com/daimajia/AndroidSwipeLayout/wiki)

## About me

A student in mainland China.

Welcome to [offer me an internship](mailto:daimajia@gmail.com). If you have any new idea about this project, feel free to [contact me](mailto:daimajia@gmail.com). :smiley:

