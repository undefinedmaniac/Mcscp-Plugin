package com.gmail.undifinedmaniac.mcscpplugin.table;

import java.util.HashMap;
import java.util.Map;

abstract public class McscpTableBase<T> {
    private Map<T, String> mData = new HashMap<>();

    abstract void updateKeys();

    public Map<T, String> getAllData() {
        return mData;
    }

    protected boolean updateKey(T key, String value) {
        boolean replace = !value.equals(mData.get(key));

        if (replace)
            mData.put(key, value);

        return replace;
    }
}
