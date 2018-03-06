package com.andrewtakao.alight.databinding;
import com.andrewtakao.alight.R;
import com.andrewtakao.alight.BR;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityOrderedTourBinding extends android.databinding.ViewDataBinding  {

    @Nullable
    private static final android.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.toolbar, 1);
        sViewsWithIds.put(R.id.toolbar_title, 2);
        sViewsWithIds.put(R.id.closest_poi_toolbar, 3);
        sViewsWithIds.put(R.id.direction_toolbar, 4);
        sViewsWithIds.put(R.id.rv_pois, 5);
        sViewsWithIds.put(R.id.tv_time, 6);
        sViewsWithIds.put(R.id.sb_song, 7);
        sViewsWithIds.put(R.id.tv_duration, 8);
        sViewsWithIds.put(R.id.ib_rewind, 9);
        sViewsWithIds.put(R.id.ib_start, 10);
        sViewsWithIds.put(R.id.ib_forward, 11);
        sViewsWithIds.put(R.id.location, 12);
        sViewsWithIds.put(R.id.closest_poi, 13);
    }
    // views
    @NonNull
    public final android.widget.TextView closestPoi;
    @NonNull
    public final android.widget.TextView closestPoiToolbar;
    @NonNull
    public final android.widget.TextView directionToolbar;
    @NonNull
    public final android.widget.ImageButton ibForward;
    @NonNull
    public final android.widget.ImageButton ibRewind;
    @NonNull
    public final android.widget.ImageButton ibStart;
    @NonNull
    public final android.widget.TextView location;
    @NonNull
    private final android.widget.LinearLayout mboundView0;
    @NonNull
    public final android.support.v7.widget.RecyclerView rvPois;
    @NonNull
    public final android.widget.SeekBar sbSong;
    @NonNull
    public final android.support.v7.widget.Toolbar toolbar;
    @NonNull
    public final android.widget.TextView toolbarTitle;
    @NonNull
    public final android.widget.TextView tvDuration;
    @NonNull
    public final android.widget.TextView tvTime;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityOrderedTourBinding(@NonNull android.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        super(bindingComponent, root, 0);
        final Object[] bindings = mapBindings(bindingComponent, root, 14, sIncludes, sViewsWithIds);
        this.closestPoi = (android.widget.TextView) bindings[13];
        this.closestPoiToolbar = (android.widget.TextView) bindings[3];
        this.directionToolbar = (android.widget.TextView) bindings[4];
        this.ibForward = (android.widget.ImageButton) bindings[11];
        this.ibRewind = (android.widget.ImageButton) bindings[9];
        this.ibStart = (android.widget.ImageButton) bindings[10];
        this.location = (android.widget.TextView) bindings[12];
        this.mboundView0 = (android.widget.LinearLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.rvPois = (android.support.v7.widget.RecyclerView) bindings[5];
        this.sbSong = (android.widget.SeekBar) bindings[7];
        this.toolbar = (android.support.v7.widget.Toolbar) bindings[1];
        this.toolbarTitle = (android.widget.TextView) bindings[2];
        this.tvDuration = (android.widget.TextView) bindings[8];
        this.tvTime = (android.widget.TextView) bindings[6];
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x1L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
            return variableSet;
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        // batch finished
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;

    @NonNull
    public static ActivityOrderedTourBinding inflate(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup root, boolean attachToRoot) {
        return inflate(inflater, root, attachToRoot, android.databinding.DataBindingUtil.getDefaultComponent());
    }
    @NonNull
    public static ActivityOrderedTourBinding inflate(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup root, boolean attachToRoot, @Nullable android.databinding.DataBindingComponent bindingComponent) {
        return android.databinding.DataBindingUtil.<ActivityOrderedTourBinding>inflate(inflater, com.andrewtakao.alight.R.layout.activity_ordered_tour, root, attachToRoot, bindingComponent);
    }
    @NonNull
    public static ActivityOrderedTourBinding inflate(@NonNull android.view.LayoutInflater inflater) {
        return inflate(inflater, android.databinding.DataBindingUtil.getDefaultComponent());
    }
    @NonNull
    public static ActivityOrderedTourBinding inflate(@NonNull android.view.LayoutInflater inflater, @Nullable android.databinding.DataBindingComponent bindingComponent) {
        return bind(inflater.inflate(com.andrewtakao.alight.R.layout.activity_ordered_tour, null, false), bindingComponent);
    }
    @NonNull
    public static ActivityOrderedTourBinding bind(@NonNull android.view.View view) {
        return bind(view, android.databinding.DataBindingUtil.getDefaultComponent());
    }
    @NonNull
    public static ActivityOrderedTourBinding bind(@NonNull android.view.View view, @Nullable android.databinding.DataBindingComponent bindingComponent) {
        if (!"layout/activity_ordered_tour_0".equals(view.getTag())) {
            throw new RuntimeException("view tag isn't correct on view:" + view.getTag());
        }
        return new ActivityOrderedTourBinding(bindingComponent, view);
    }
    /* flag mapping
        flag 0 (0x1L): null
    flag mapping end*/
    //end
}