package com.johnanderson.pinewoodderbyapp.binding;

import android.databinding.BindingAdapter;
import android.view.View;

/**
 * Created by johnanderson on 12/4/17.
 */

public class BindingAdapters {
    @BindingAdapter("show")
    public static void showHide(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
