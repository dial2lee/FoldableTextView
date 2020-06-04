主要用于实现Android TextView的收起和展开功能
===

控件依赖于TextVie的属性maxLines，当文本内容显示行数小于或等于设置的此值时不具备收起和展开功能，当大于此值时将TextView设置的文本进行切割用来显示收起和展开状态。
此控件会在文本末尾追加‘收起’和‘扩展’文本标识可操作的行为，并可用于点击执行操作

控件的属性
--------

| 属性             | 说明                                   |
| ---------------- | -------------------------------------- |
| hasAnimation     | 在‘收起’和‘展开’时是否需要展示动画效果 |
| closeInNewLine   | ‘收起’是否需要在展示在新的一行         |
| closeAlignRight  | ‘收起’是否对齐到控件的右侧             |
| openSuffixColor  | ‘展开’文本的颜色                       |
| closeSuffixColor | ‘收起’文本的颜色                       |
| openSuffixText   | ‘展开’文本                             |
| closeSuffixText  | ‘收起’文本                             |

控件的使用
---------
```xml
<com.diallee.view.FoldableTextView
        android:id="@+id/expanded_text"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_margin="10dp"
        android:maxLines="3"
        app:closeAlignRight="false"
        app:closeInNewLine="false"
        app:closeSuffixColor="@color/colorAccent"
        app:hasAnimation="true"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:openSuffixColor="@color/colorAccent"
        android:text="Flutter是谷歌的移动UI框架，可以快速在iOS和Android上构建高质量的原生用户界面。 Flutter可以与现有的代码一起工作。在全世界，Flutter正在被越来越多的开发者和组织使用，并且Flutter是完全免费、开源的。在全球，随着Flutter被越来越多的知名公司应用在自己的商业APP中，Flutter这门新技术也逐渐进入了移动开发者的视野，尤其是当Google在2018年IO大会上发布了第一个Preview版本后，国内刮起来一股学习Flutter的热潮。为了更好的方便帮助中国开发者了解这门新技术，我们，Flutter中文网，前后发起了Flutter翻译计划、Flutter开源计划，前者主要的任务是翻译Flutter官方文档，后者则主要是开发一些常用的包来丰富Flutter生态，帮助开发者提高开发效率。而时至今日，这两件事取得的效果还都不错！" />

```

