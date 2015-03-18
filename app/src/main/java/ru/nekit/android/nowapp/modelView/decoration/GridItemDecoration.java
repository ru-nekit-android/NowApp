package ru.nekit.android.nowapp.modelView.decoration;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class GridItemDecoration extends RecyclerView.ItemDecoration {

    private int insetHorizontal;
    private int insetVertical;

    public GridItemDecoration(int insetVertical, int insetHorizontal) {
        this.insetHorizontal = insetHorizontal;
        this.insetVertical = insetVertical;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        GridLayoutManager.LayoutParams layoutParams
                = (GridLayoutManager.LayoutParams) view.getLayoutParams();

        int position = layoutParams.getViewPosition();
        if (position == RecyclerView.NO_POSITION) {
            outRect.set(0, 0, 0, 0);
            return;
        }

        outRect.right = insetVertical / 2;
        outRect.left = insetVertical / 2;
        outRect.bottom = outRect.top = insetHorizontal / 2;
    }
}