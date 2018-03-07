
package android.databinding;
import com.andrewtakao.alight.BR;
class DataBinderMapper  {
    final static int TARGET_MIN_SDK = 17;
    public DataBinderMapper() {
    }
    public android.databinding.ViewDataBinding getDataBinder(android.databinding.DataBindingComponent bindingComponent, android.view.View view, int layoutId) {
        switch(layoutId) {
                case com.andrewtakao.alight.R.layout.activity_main:
                    return com.andrewtakao.alight.databinding.ActivityMainBinding.bind(view, bindingComponent);
                case com.andrewtakao.alight.R.layout.activity_changing_tour:
                    return com.andrewtakao.alight.databinding.ActivityChangingTourBinding.bind(view, bindingComponent);
                case com.andrewtakao.alight.R.layout.activity_ordered_tour:
                    return com.andrewtakao.alight.databinding.ActivityOrderedTourBinding.bind(view, bindingComponent);
        }
        return null;
    }
    android.databinding.ViewDataBinding getDataBinder(android.databinding.DataBindingComponent bindingComponent, android.view.View[] views, int layoutId) {
        switch(layoutId) {
        }
        return null;
    }
    int getLayoutId(String tag) {
        if (tag == null) {
            return 0;
        }
        final int code = tag.hashCode();
        switch(code) {
            case 423753077: {
                if(tag.equals("layout/activity_main_0")) {
                    return com.andrewtakao.alight.R.layout.activity_main;
                }
                break;
            }
            case -548517328: {
                if(tag.equals("layout/activity_changing_tour_0")) {
                    return com.andrewtakao.alight.R.layout.activity_changing_tour;
                }
                break;
            }
            case -1762045082: {
                if(tag.equals("layout/activity_ordered_tour_0")) {
                    return com.andrewtakao.alight.R.layout.activity_ordered_tour;
                }
                break;
            }
        }
        return 0;
    }
    String convertBrIdToString(int id) {
        if (id < 0 || id >= InnerBrLookup.sKeys.length) {
            return null;
        }
        return InnerBrLookup.sKeys[id];
    }
    private static class InnerBrLookup {
        static String[] sKeys = new String[]{
            "_all"};
    }
}