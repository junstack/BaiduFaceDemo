package com.das.face.util;

import android.content.Context;

/**
 * created by jun on 2020/6/28
 * describe: 公共方法
 */
public class PublicMethods {
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
