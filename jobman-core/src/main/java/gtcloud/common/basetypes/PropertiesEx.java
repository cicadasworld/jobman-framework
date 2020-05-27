package gtcloud.common.basetypes;

import java.io.FileInputStream;
import java.util.Properties;

@SuppressWarnings("serial")
public class PropertiesEx extends Properties {

    public static final PropertiesEx GLOBAL = new PropertiesEx();

    private final Options _optional = new Options();

    public interface SymbolReplacer {
        // 替换给定的符号. 若能替换, 返回新值; 否则返回null.
        String replaceSymbol(String symbol);
    }

    public PropertiesEx() {
        // Auto-generated constructor stub
    }

    public PropertiesEx(Properties defaults) {
        super(defaults);
        // Auto-generated constructor stub
    }

    public SymbolReplacer getSymbolReplacer() {
        final PropertiesEx self = this;
        return new SymbolReplacer() {
            @Override
            public String replaceSymbol(String symbol) {
                // 判断name是否为已经定义的符号
                String v = self.getProperty(symbol);
                if (v == null) {
                    v = System.getProperty(symbol);
                }
                if (v == null) {
                    v = System.getenv(symbol);
                }
                return v;
            }
        };
    }

    // 将字符串中形如${name}的宏定义进行求值
    public static String replaceSymbol(String val, SymbolReplacer replacer) {

        if (val.indexOf('$') < 0) {
            return val;
        }

        StringBuilder sb = new StringBuilder(val.length() + 100);
        int fromIndex = 0;
        int nextAppendIndex = 0;

        for (;;) {
            int p = val.indexOf("${", fromIndex);
            if (p < 0) {
                break;
            }

            int q = val.indexOf('}', p + 2);
            if (q < 0) {
                break;
            }

            String name = val.substring(p + 2, q);
            String v = replacer.replaceSymbol(name);
            if (v == null) {
                fromIndex = q + 1;
                continue;
            }

            if (nextAppendIndex < p) {
                String s = val.substring(nextAppendIndex, p);
                sb.append(s);
            }
            sb.append(v);

            fromIndex = q + 1;
            nextAppendIndex = fromIndex;
        }

        if (nextAppendIndex < val.length()) {
            String s = val.substring(nextAppendIndex);
            sb.append(s);
        }

        return sb.toString();
    }

    // 对val中的符号求值, 然后加入属性表中.
    public String replaceSymbolAndSetProperty(String nm, String val) {
        SymbolReplacer r = this.getSymbolReplacer();
        val = replaceSymbol(val, r);
        super.setProperty(nm, val);
        return val;
    }

    // get the specified 'name' as BOOL
    public boolean getProperty_Bool(String name) {
        String v = this.getProperty(name, "0");
        return v.equals("1") || v.equalsIgnoreCase("true");
    }

    // get the specified 'name' as Integer
    public int getProperty_Int(String name, int defValue) {
        String v = this.getProperty(name);
        if (v == null) {
            return defValue;
        }

        return Integer.parseInt(v);
    }

    public void setProperty_Int(String name, int nval) {
        super.setProperty(name, nval + "");
    }

    // get the specified 'name' as FLOAT
    public double getProperty_Float(String name, double defValue) {
        String v = this.getProperty(name);
        if (v == null) {
            return defValue;
        }

        return Double.parseDouble(v);
    }

    public void setProperty_Float(String name, double dval) {
        super.setProperty(name, dval + "");
    }

    // get the specified 'name' as Integer64
    public long getProperty_Long(String name, long defValue) {
        String v = this.getProperty(name);
        if (v == null) {
            return defValue;
        }
        return Long.parseLong(v);
    }

    public void setProperty_Long(String name, long nval) {
        super.setProperty(name, nval + "");
    }

    public Object getProperty_Object(String name) {
        return _optional.getObject(name);
    }

    public void setProperty_Object(String name, Object obj) {
        _optional.setObject(name, obj);
    }

    public void load(String fname, final SymbolReplacer...replacers) throws Exception {
        FileInputStream fis = new FileInputStream(fname);
        Properties props = new Properties();
        try {
            props.load(fis);
        }
        finally {
            fis.close();
        }

        SymbolReplacer combinedReplacer = new SymbolReplacer() {
            @Override
            public String replaceSymbol(String symbol) {
                for (int i=0; i<replacers.length; ++i) {
                    SymbolReplacer r = replacers[i];
                    if (r == null) {
                        continue;
                    }
                    String v = r.replaceSymbol(symbol);
                    if (v != null) {
                        return v;
                    }
                }
                return null;
            }
        };

        for (String key : props.stringPropertyNames()) {
            String val = props.getProperty(key);
            val = replaceSymbol(val, combinedReplacer);
            super.setProperty(key, val);
        }
    }
}
