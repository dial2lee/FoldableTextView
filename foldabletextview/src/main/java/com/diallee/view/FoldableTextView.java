package com.diallee.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.diallee.foldabletextview.example.R;

public class FoldableTextView extends AppCompatTextView {
    private static final String TAG = FoldableTextView.class.getSimpleName();
    // 省略号
    public static final String ELLIPSIS_STRING = new String(new char[]{'\u2026'});
    // 默认收起状态最大的展示行数
    private static final int DEFAULT_MAX_LINE = 3;
    // 默认展开和收起的文字标识
    private static final String DEFAULT_OPEN_SUFFIX = "展开";
    private static final String DEFAULT_CLOSE_SUFFIX = "收起";
    // 是否切换时展示动画效果
    private volatile boolean animating = false;
    // 当前状态
    private boolean isClosed = false;
    // 收起状态最大的展示行数
    private int mMaxLines = DEFAULT_MAX_LINE;
    // TextView可展示宽度，包含paddingLeft和paddingRight
    private int mViewWidth = 0;
    // 原始的文本
    private CharSequence originalText;

    private SpannableStringBuilder mOpenSpannableStr, mCloseSpannableStr;

    private boolean hasAnimation = false;
    private Animation mOpenAnim, mCloseAnim;
    private int mOpenHeight, mCLoseHeight;
    private boolean mExpandable;
    private boolean mCloseInNewLine = false;
    private boolean mCloseisRightAlign = false;
    // 收起和展开的文字
    @Nullable
    private SpannableString mOpenSuffixSpan, mCloseSuffixSpan;
    // 展开的文字标识
    private String mOpenSuffixStr = DEFAULT_OPEN_SUFFIX;
    // 收起的文字标识
    private String mCloseSuffixStr = DEFAULT_CLOSE_SUFFIX;
    // 展开和收起的文字颜色
    private int mOpenSuffixColor, mCloseSuffixColor;
    // 是否为自己设置的文本
    private boolean isSelfSet = false;

    private OnClickListener mOnClickListener;

    // 是否点击触发切换展开和收起状态
    private boolean isClickToggleState = false;

    private CharSequenceToSpannableHandler mCharSequenceToSpannableHandler;

    public FoldableTextView(Context context) {
        super(context);
        initialize(null);
    }

    public FoldableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    public FoldableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs);
    }

    /**
     * 初始化
     */
    private void initialize(AttributeSet attrs) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs,
                R.styleable.FoldableTextView, 0, 0);
        hasAnimation = typedArray.getBoolean(R.styleable.FoldableTextView_hasAnimation, true);
        mCloseInNewLine = typedArray.getBoolean(R.styleable.FoldableTextView_closeInNewLine, false);
        mCloseisRightAlign = typedArray.getBoolean(R.styleable.FoldableTextView_closeAlignRight, false);
        isClickToggleState = typedArray.getBoolean(R.styleable.FoldableTextView_isClickToggleState, false);
        mOpenSuffixColor = typedArray.getColor(R.styleable.FoldableTextView_closeSuffixColor, Color.parseColor("#F23030"));
        mCloseSuffixColor = typedArray.getColor(R.styleable.FoldableTextView_openSuffixColor, Color.parseColor("#F23030"));
        mOpenSuffixStr = typedArray.getString(R.styleable.FoldableTextView_openSuffixText);
        if (TextUtils.isEmpty(mOpenSuffixStr)) {
            mOpenSuffixStr = DEFAULT_OPEN_SUFFIX;
        }
        mCloseSuffixStr = typedArray.getString(R.styleable.FoldableTextView_closeSuffixText);
        if (TextUtils.isEmpty(mCloseSuffixStr)) {
            mCloseSuffixStr = DEFAULT_CLOSE_SUFFIX;
        }
        typedArray.recycle();

        setMovementMethod(OverLinkMovementMethod.getInstance());
        setIncludeFontPadding(false);
        updateOpenSuffixSpan();
        updateCloseSuffixSpan();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int measuredWidth = getMeasuredWidth();
        if (mViewWidth != measuredWidth) {
            mViewWidth = measuredWidth;
            setOriginalText();
        }
    }

    // 外界设置的text保留为原始的需要显示的文本
    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (!isSelfSet) {
            originalText = text;
        } else {
            isSelfSet = false;
        }
    }

    private void setSelfText(CharSequence text) {
        setText(text);
        isSelfSet = true;
    }

    public void setOriginalText() {
        mExpandable = false;
        final int maxLines = mMaxLines;
        SpannableStringBuilder tempText = charSequenceToSpannable(originalText);
        mOpenSpannableStr = charSequenceToSpannable(originalText);

        if (maxLines > 0) {
            Layout layout = createStaticLayout(tempText);
            mExpandable = layout.getLineCount() > maxLines;
            if (mExpandable) {
                //计算原文截取位置
                int endPos = layout.getLineEnd(maxLines - 1);

                if (originalText.length() <= endPos) {
                    mCloseSpannableStr = charSequenceToSpannable(originalText);
                } else {
                    mCloseSpannableStr = charSequenceToSpannable(originalText.subSequence(0, endPos));
                }
                SpannableStringBuilder tempText2 = charSequenceToSpannable(mCloseSpannableStr).append(ELLIPSIS_STRING);
                if (mOpenSuffixSpan != null) {
                    tempText2.append(mOpenSuffixSpan);
                }
                //循环判断，收起内容添加展开后缀后的内容
                Layout tempLayout = createStaticLayout(tempText2);
                while (tempLayout.getLineCount() > maxLines) {
                    int lastSpace = mCloseSpannableStr.length() - 1;
                    if (lastSpace == -1) {
                        break;
                    }
                    if (originalText.length() <= lastSpace) {
                        mCloseSpannableStr = charSequenceToSpannable(originalText);
                    } else {
                        mCloseSpannableStr = charSequenceToSpannable(originalText.subSequence(0, lastSpace));
                    }
                    tempText2 = charSequenceToSpannable(mCloseSpannableStr).append(ELLIPSIS_STRING);
                    if (mOpenSuffixSpan != null) {
                        tempText2.append(mOpenSuffixSpan);
                    }
                    tempLayout = createStaticLayout(tempText2);
                }
                //计算收起的文本高度
                mCLoseHeight = tempLayout.getHeight() + getPaddingTop() + getPaddingBottom();
                mCloseSpannableStr = tempText2;

                if (mCloseSuffixSpan != null) {
                    if (mCloseInNewLine) {
                        mOpenSpannableStr.append("\n");
                        mOpenSpannableStr.append(mCloseSuffixSpan);
                    } else {
                        mOpenSpannableStr.append(mCloseSuffixSpan);
                        Layout tempLayout2 = createStaticLayout(mOpenSpannableStr);
                        if (tempLayout2.getLineCount() > layout.getLineCount()) {
                            mOpenSpannableStr = charSequenceToSpannable(originalText);
                            mOpenSpannableStr.append("\n");
                            mOpenSpannableStr.append(mCloseSuffixSpan);
                        } else {
                            if (mCloseisRightAlign) {
                                int insertIndex;
                                do {
                                    insertIndex = mOpenSpannableStr.length() - mCloseSuffixSpan.length();
                                    mOpenSpannableStr.insert(insertIndex, " ");
                                    tempLayout2 = createStaticLayout(mOpenSpannableStr);
                                } while (tempLayout2.getLineCount() == layout.getLineCount());
                                mOpenSpannableStr.delete(insertIndex, insertIndex + 1);
                            }
                        }
                    }
                }
            }
        }
        isClosed = mExpandable;
        if (mExpandable) {
            setSelfText(mCloseSpannableStr);
            //设置监听
            super.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isClickToggleState) {
                        switchOpenClose();
                    }
                    if (mOnClickListener != null) {
                        mOnClickListener.onClick(v);
                    }
                }
            });
        } else {
            setSelfText(mOpenSpannableStr);
        }
    }

    private int hasEnCharCount(CharSequence str) {
        int count = 0;
        if (!TextUtils.isEmpty(str)) {
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c >= ' ' && c <= '~') {
                    count++;
                }
            }
        }
        return count;
    }

    private void switchOpenClose() {
        if (mExpandable) {
            isClosed = !isClosed;
            if (isClosed) {
                close();
            } else {
                open();
            }
        }
    }

    /**
     * 设置是否有动画
     *
     * @param hasAnimation
     */
    public void setHasAnimation(boolean hasAnimation) {
        this.hasAnimation = hasAnimation;
    }

    public void toggleState() {
        if (isClosed) {
            open();
        } else {
            close();
        }
    }

    /**
     * 展开
     */
    private void open() {
        if (hasAnimation) {
            Layout layout = createStaticLayout(mOpenSpannableStr);
            mOpenHeight = layout.getHeight() + getPaddingTop() + getPaddingBottom();
            executeOpenAnim();
        } else {
            FoldableTextView.super.setMaxLines(Integer.MAX_VALUE);
            setSelfText(mOpenSpannableStr);
            if (mOpenCloseCallback != null) {
                mOpenCloseCallback.onOpen();
            }
        }
    }

    /**
     * 收起
     */
    private void close() {
        if (hasAnimation) {
            executeCloseAnim();
        } else {
            FoldableTextView.super.setMaxLines(mMaxLines);
            setSelfText(mCloseSpannableStr);
            if (mOpenCloseCallback != null) {
                mOpenCloseCallback.onClose();
            }
        }
    }

    /**
     * 执行展开动画
     */
    private void executeOpenAnim() {
        //创建展开动画
        if (mOpenAnim == null) {
            mOpenAnim = new ExpandCollapseAnimation(this, mCLoseHeight, mOpenHeight);
            mOpenAnim.setFillAfter(true);
            mOpenAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    FoldableTextView.super.setMaxLines(Integer.MAX_VALUE);
                    setSelfText(mOpenSpannableStr);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //  动画结束后textview设置展开的状态
                    getLayoutParams().height = mOpenHeight;
                    requestLayout();
                    animating = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        if (animating) {
            return;
        }
        animating = true;
        clearAnimation();
        //  执行动画
        startAnimation(mOpenAnim);
    }

    /**
     * 执行收起动画
     */
    private void executeCloseAnim() {
        //创建收起动画
        if (mCloseAnim == null) {
            mCloseAnim = new ExpandCollapseAnimation(this, mOpenHeight, mCLoseHeight);
            mCloseAnim.setFillAfter(true);
            mCloseAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animating = false;
                    FoldableTextView.super.setMaxLines(mMaxLines);
                    setSelfText(mCloseSpannableStr);
                    getLayoutParams().height = mCLoseHeight;
                    requestLayout();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        if (animating) {
            return;
        }
        animating = true;
        clearAnimation();
        //  执行动画
        startAnimation(mCloseAnim);
    }

    private Layout createStaticLayout(SpannableStringBuilder spannable) {
        int contentWidth = mViewWidth - getPaddingLeft() - getPaddingRight();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(spannable, 0, spannable.length(), getPaint(), contentWidth);
            builder.setAlignment(Layout.Alignment.ALIGN_NORMAL);
            builder.setIncludePad(getIncludeFontPadding());
            builder.setLineSpacing(getLineSpacingExtra(), getLineSpacingMultiplier());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setUseLineSpacingFromFallbacks(isFallbackLineSpacing());
            }
            builder.setBreakStrategy(getBreakStrategy());
            builder.setHyphenationFrequency(getHyphenationFrequency());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setJustificationMode(getJustificationMode());
            }
            return builder.build();
        } else {
            return new StaticLayout(spannable, getPaint(), contentWidth, Layout.Alignment.ALIGN_NORMAL,
                    getLineSpacingMultiplier(), getLineSpacingExtra(), getIncludeFontPadding());
        }
    }

    private SpannableStringBuilder charSequenceToSpannable(@NonNull CharSequence charSequence) {
        SpannableStringBuilder spannableStringBuilder = null;
        if (mCharSequenceToSpannableHandler != null) {
            spannableStringBuilder = mCharSequenceToSpannableHandler.charSequenceToSpannable(charSequence);
        }
        if (spannableStringBuilder == null) {
            spannableStringBuilder = new SpannableStringBuilder(charSequence);
        }
        return spannableStringBuilder;
    }

    @Override
    public void setMaxLines(int maxLines) {
        this.mMaxLines = maxLines;
        super.setMaxLines(maxLines);
    }

    /**
     * 设置展开后缀text
     *
     * @param openSuffix
     */
    public void setOpenSuffix(String openSuffix) {
        mOpenSuffixStr = openSuffix;
        updateOpenSuffixSpan();
    }

    /**
     * 设置展开后缀文本颜色
     *
     * @param openSuffixColor
     */
    public void setOpenSuffixColor(@ColorInt int openSuffixColor) {
        mOpenSuffixColor = openSuffixColor;
        updateOpenSuffixSpan();
    }

    /**
     * 设置收起后缀text
     *
     * @param closeSuffix
     */
    public void setCloseSuffix(String closeSuffix) {
        mCloseSuffixStr = closeSuffix;
        updateCloseSuffixSpan();
    }

    /**
     * 设置收起后缀文本颜色
     *
     * @param closeSuffixColor
     */
    public void setCloseSuffixColor(@ColorInt int closeSuffixColor) {
        mCloseSuffixColor = closeSuffixColor;
        updateCloseSuffixSpan();
    }

    /**
     * 收起后缀是否另起一行
     *
     * @param closeInNewLine
     */
    public void setCloseInNewLine(boolean closeInNewLine) {
        mCloseInNewLine = closeInNewLine;
        updateCloseSuffixSpan();
//        setOriginalText();
    }

    public void setCloseAlignRight(boolean isAlighRight) {
        mCloseisRightAlign = isAlighRight;
        updateCloseSuffixSpan();
//        setOriginalText();
    }

    /**
     * 更新展开后缀Spannable
     */
    private void updateOpenSuffixSpan() {
        if (TextUtils.isEmpty(mOpenSuffixStr)) {
            mOpenSuffixSpan = null;
            return;
        }
        mOpenSuffixSpan = new SpannableString(mOpenSuffixStr);
        mOpenSuffixSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, mOpenSuffixStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mOpenSuffixSpan.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                switchOpenClose();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, 0, mOpenSuffixStr.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
    }

    /**
     * 更新收起后缀Spannable
     */
    private void updateCloseSuffixSpan() {
        if (TextUtils.isEmpty(mCloseSuffixStr)) {
            mCloseSuffixSpan = null;
            return;
        }
        mCloseSuffixSpan = new SpannableString(mCloseSuffixStr);
        mCloseSuffixSpan.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, mCloseSuffixStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (mCloseisRightAlign && mCloseInNewLine) {
            AlignmentSpan alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE);
            mCloseSuffixSpan.setSpan(alignmentSpan, 0, mCloseSuffixStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mCloseSuffixSpan.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                switchOpenClose();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        }, 0, mCloseSuffixStr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public OpenAndCloseCallback mOpenCloseCallback;

    public void setOpenAndCloseCallback(OpenAndCloseCallback callback) {
        this.mOpenCloseCallback = callback;
    }

    public interface OpenAndCloseCallback {
        void onOpen();

        void onClose();
    }

    /**
     * 设置文本内容处理
     *
     * @param handler
     */
    public void setCharSequenceToSpannableHandler(CharSequenceToSpannableHandler handler) {
        mCharSequenceToSpannableHandler = handler;
    }

    public interface CharSequenceToSpannableHandler {
        @NonNull
        SpannableStringBuilder charSequenceToSpannable(CharSequence charSequence);
    }

    private static class ExpandCollapseAnimation extends Animation {
        private final View mTargetView;//动画执行view
        private final int mStartHeight;//动画执行的开始高度
        private final int mEndHeight;//动画结束后的高度

        ExpandCollapseAnimation(View target, int startHeight, int endHeight) {
            mTargetView = target;
            mStartHeight = startHeight;
            mEndHeight = endHeight;
            setDuration(400);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            mTargetView.setScrollY(0);
            //计算出每次应该显示的高度,改变执行view的高度，实现动画
            mTargetView.getLayoutParams().height = (int) ((mEndHeight - mStartHeight) * interpolatedTime + mStartHeight);
            mTargetView.requestLayout();
        }
    }
}
