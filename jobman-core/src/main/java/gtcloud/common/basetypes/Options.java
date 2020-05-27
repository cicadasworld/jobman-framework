package gtcloud.common.basetypes;

import java.util.HashMap;
import java.util.Set;

// optional key-value pairs
public class Options {

    private enum ValueType {
        BOOL,
        INT,
        LONG,
        DOUBLE,
        STRING,
        OBJECT
    }

    private static class Entry {
        ValueType vt;
        Object value;
    }

    private HashMap<String, Entry> _kvps = new HashMap<String, Entry>(4);

    public boolean hasKey(String key) {
        return _kvps.containsKey(key);
    }
    
    public void removeKey(String key) {
    	_kvps.remove(key);
    }

    public void setBool(String key, boolean val) {
        Entry e = new Entry();
        e.vt = ValueType.BOOL;
        e.value = val ? Boolean.TRUE : Boolean.FALSE;
        _kvps.put(key, e);
    }

    public boolean getBool(String key, boolean defValue) {
        Entry e = _kvps.get(key);
        if (e == null) {
            return defValue;
        }

        if (e.vt == ValueType.BOOL) {
            return e.value == Boolean.TRUE;
        }
        if (e.vt == ValueType.INT || e.vt == ValueType.LONG || e.vt == ValueType.DOUBLE) {
            return ((Number)e.value).intValue() != 0;
        }
        if (e.vt == ValueType.STRING) {
            String s = (String)e.value;
            return Boolean.parseBoolean(s) || s.equals("1") || s.equalsIgnoreCase("on");
        }

        return defValue;
    }

    public void setInt(String key, int val) {
        Entry e = new Entry();
        e.vt = ValueType.INT;
        e.value = Integer.valueOf(val);
        _kvps.put(key, e);
    }

    public int getInt(String key, int defValue) {
        Entry e = _kvps.get(key);
        if (e == null) {
            return defValue;
        }

        if (e.vt == ValueType.INT || e.vt == ValueType.LONG || e.vt == ValueType.DOUBLE) {
            return ((Number)e.value).intValue();
        }
        if (e.vt == ValueType.STRING) {
            String s = (String)e.value;
            return Integer.parseInt(s);
        }

        return defValue;
    }

    public void setLong(String key, long val) {
        Entry e = new Entry();
        e.vt = ValueType.LONG;
        e.value = Long.valueOf(val);
        _kvps.put(key, e);
    }

    public long getLong(String key, long defValue) {
        Entry e = _kvps.get(key);
        if (e == null) {
            return defValue;
        }

        if (e.vt == ValueType.INT || e.vt == ValueType.LONG || e.vt == ValueType.DOUBLE) {
            return ((Number)e.value).longValue();
        }
        if (e.vt == ValueType.STRING) {
            String s = (String)e.value;
            return Long.parseLong(s);
        }

        return defValue;
    }

    public void setDouble(String key, double val) {
        Entry e = new Entry();
        e.vt = ValueType.DOUBLE;
        e.value = Double.valueOf(val);
        _kvps.put(key, e);
    }

    public double getDouble(String key, double defValue) {
        Entry e = _kvps.get(key);
        if (e == null) {
            return defValue;
        }

        if (e.vt == ValueType.INT || e.vt == ValueType.LONG || e.vt == ValueType.DOUBLE) {
            return ((Number)e.value).doubleValue();
        }
        if (e.vt == ValueType.STRING) {
            String s = (String)e.value;
            return Double.parseDouble(s);
        }

        return defValue;
    }

    public void setString(String key, String val) {
        Entry e = new Entry();
        e.vt = ValueType.STRING;
        e.value = val;
        _kvps.put(key, e);
    }

    public String getString(String key, String defValue) {
        Entry e = _kvps.get(key);
        if (e == null) {
            return defValue;
        }

        if (e.vt == ValueType.STRING) {
            return (String)e.value;
        }

        if (e.vt == ValueType.INT || e.vt == ValueType.LONG || e.vt == ValueType.DOUBLE) {
            return ((Number)e.value).toString();
        }

        if (e.vt == ValueType.BOOL) {
            return e.value + "";
        }
        
        return defValue;
    }

    public void setObject(String key, Object val) {
        Entry e = new Entry();
        e.vt = ValueType.OBJECT;
        e.value = val;
        _kvps.put(key, e);
    }

    public Object getObject(String key) {
        Entry e = _kvps.get(key);
        if (e == null) {
            return null;
        }
        return e.value;
    }

    public Set<String> getAllKeys() {
        return _kvps.keySet();
    }
}
