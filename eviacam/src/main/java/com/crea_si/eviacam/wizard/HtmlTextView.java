package com.crea_si.eviacam.wizard;

import android.annotation.TargetApi;
import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.TextView;

public class HtmlTextView extends TextView {
    public HtmlTextView(Context context) {
        super(context);
    }

    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public HtmlTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setText(CharSequence s, BufferType b){
        super.setText(Html.fromHtml(s.toString()), b);
    }
}
