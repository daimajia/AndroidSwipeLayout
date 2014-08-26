# Android Swipe Layout

This is the brother of [AndroidViewHover](https://github.com/daimajia/AndroidViewHover).

One year ago, I started to make an app named [EverMemo](https://play.google.com/store/apps/details?id=com.zhan_dui.evermemo) with my good friends. The designer gave me a design picture, the design like this:

![](http://ww1.sinaimg.cn/mw690/610dc034jw1ejoquidvvsg208i0630u4.gif)

I found it was pretty hard to achieve this effect, cause you had to be very familiar with the Android Touch System. It was beyond my ability that moment, and I also noticed that there was no such a concept library...

Time passed, finally...as you see right now.

## Demo

![](http://ww2.sinaimg.cn/mw690/610dc034jw1ejoplapwtqg208n0e74dx.gif)

[Download Demo](https://github.com/daimajia/AndroidSwipeLayout/releases/download/v1.0.0/AndroidSwipeLayout-Demo-1.0.1-snapshot.apk)

Before I made this, I actually found some libraries (eg.[SwipeListView](https://github.com/47deg/android-swipelistview)) that helps developers to integrate swiping with your UI component. But they have too much limitation, only in ListView, or some other limitations.

When I start to make this library, I set some goals:

- Can be easily integrated in anywhere, ListView, GridView, ViewGroup etc.
- Can receive `onOpen`,`onClose`,`onUpdate` callbacks.
- Can notifiy the hidden children how much they have shown.
- Can be nested each other.

## Usage

### Step 1
#### Gradle

```groovy
dependencies {
        compile "com.daimajia.swipelayout:library:1.0.3@aar"
}
```

#### Maven

```xml
<dependency>
    <groupId>com.daimajia.swipelayout</groupId>
    <artifactId>library</artifactId>
    <version>1.0.3</version>
    <type>apklib</type>
</dependency>
```

#### Eclipse

[AndroidSwipeLayout-v1.0.3.jar](https://github.com/daimajia/AndroidSwipeLayout/releases/download/v1.0.3/AndroidSwipeLayout-v1.0.3.jar)

### Step 2

[Wiki Usage](https://github.com/daimajia/AndroidSwipeLayout/wiki/usage)

## Wiki

[Go to Wiki](https://github.com/daimajia/AndroidSwipeLayout/wiki)

## About me

A student in mainland China.

Welcome to [offer me an internship](mailto:daimajia@gmail.com). If you have any new idea about this project, feel free to [contact me](mailto:daimajia@gmail.com). :smiley:

