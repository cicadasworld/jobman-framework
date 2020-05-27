package platon;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonNode {

    public enum ValueType {
        NULL,
        INT64,
        DOUBLE,
        STRING,
        BOOL,
        BINARY,
        ARRAY,
        OBJECT
    }

    private ValueType _valueType = ValueType.NULL;

    // when _valueType is [NULL, INT64, DOUBLE, STRING, BOOL, BINARY]
    private Object _value = null;

    // when _valueType is [ARRAY]
    private ArrayList<JsonNode> _array = null;

    // when _valueType is [OBJECT]
    private HashMap<String, JsonNode> _dict = null;

    public static final JsonNode NULL_NODE = new JsonNode(ValueType.NULL);

    public static final JsonNode TRUE_NODE = new JsonNode(true);

    public static final JsonNode FALSE_NODE = new JsonNode(false);

    private JsonNode(ValueType vt) {
        _valueType = vt;
        if (vt == ValueType.NULL) {
            _value = null;
        }
        else if (vt == ValueType.ARRAY) {
            _array = new ArrayList<JsonNode>();
        }
        else if (vt == ValueType.OBJECT) {
            _dict = new HashMap<String, JsonNode>();
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public JsonNode(long val) {
        _valueType = ValueType.INT64;
        _value = Long.valueOf(val);
    }

    public JsonNode(double val) {
        _valueType = ValueType.DOUBLE;
        _value = Double.valueOf(val);
    }

    public JsonNode(String val) {
        _valueType = ValueType.STRING;
        _value = val;
    }

    public JsonNode(boolean val) {
        _valueType = ValueType.BOOL;
        _value = val ? Boolean.TRUE : Boolean.FALSE;
    }

    public JsonNode(byte[] val, int offset, int len) {
        _valueType = ValueType.BINARY;
        byte[] hex = base16Encode(val, offset, len);
        _value = new String(hex);
    }

    public static JsonNode createJsonNull() {
        return new JsonNode(ValueType.NULL);
    }

    public static JsonNode createJsonArray() {
        return new JsonNode(ValueType.ARRAY);
    }

    public static JsonNode createJsonObject() {
        return new JsonNode(ValueType.OBJECT);
    }

    public ValueType getValueType() {
        return _valueType;
    }

    public boolean isNull() {
        return _valueType == ValueType.NULL;
    }

    public boolean isBool() {
        return _valueType == ValueType.BOOL;
    }

    public boolean isInt64() {
        return _valueType == ValueType.INT64;
    }

    public boolean isBinary() {
        return _valueType == ValueType.BINARY
               ||
               (_valueType == ValueType.STRING && isBase16((String)_value));
    }

    public boolean isIntegral() {
        return _valueType == ValueType.INT64;
    }

    public boolean isDouble() {
        return _valueType == ValueType.DOUBLE;
    }

    public boolean isNumeric() {
        return isIntegral() || isDouble();
    }

    public boolean isString() {
        return _valueType == ValueType.STRING || _valueType == ValueType.BINARY;
    }

    public boolean isArray() {
        return _valueType == ValueType.ARRAY;
    }

    public boolean isObject() {
        return _valueType == ValueType.OBJECT;
    }

    /**
     * 返回ARRAY中的元素个数，或OBJECT中的名值对个数。
     *
     * @return 若值类型为ARRAY则返回其中的元素个数，若值类型为OBJECT则返回其中的名值对个数，其它值类型返回0。
     */
    public int size() {
        if (_valueType == ValueType.ARRAY) {
            return _array.size();
        }

        if (_valueType == ValueType.OBJECT) {
            return _dict.size();
        }

        return 0;
    }

    /**
     * 判断是否为null、空ARRAY或空OBJECT。
     *
     * @return 若当前值为null、空ARRAY或空OBJECT返回true，否则返回false。
     */
    public boolean isEmpty() {
        if (_valueType == ValueType.NULL) {
            return true;
        }

        if (_valueType == ValueType.ARRAY || _valueType == ValueType.OBJECT) {
            return size() == 0;
        }

        return false;
    }

    /**
     * 若值类型为ARRAY则清空其中的元素，若值类型为OBJECT则清空其中的名值对。
     */
    public void clear() {
        if (_valueType == ValueType.ARRAY) {
            _array.clear();
        }
        if (_valueType == ValueType.ARRAY || _valueType == ValueType.OBJECT) {
            _dict.clear();
        }
    }

    public long asInt64() {
        if (_valueType == ValueType.INT64) {
            return ((Long)_value).longValue();
        }
        if (_valueType == ValueType.DOUBLE) {
            return ((Double)_value).longValue();
        }
        return 0;
    }

    public double asDouble() {
        if (_valueType == ValueType.DOUBLE) {
            return ((Double)_value).doubleValue();
        }
        if (_valueType == ValueType.INT64) {
            return ((Long)_value).doubleValue();
        }
        return 0;
    }

    public boolean asBool() {
        if (_valueType == ValueType.BOOL) {
            return _value == Boolean.TRUE;
        }
        return false;
    }

    public String asString() {
        if (_valueType == ValueType.STRING || _valueType == ValueType.BINARY) {
            return (String)_value;
        }
        return null;
    }

    public byte[] asBinary() {
        if (_valueType == ValueType.STRING) {
            String s = (String)_value;
            if (isBase16(s)) {
                byte[] v = s.getBytes();
                return base16Decode(v, 0, v.length);
            }
        }
        if (_valueType == ValueType.BINARY) {
            String s = (String)_value;
            byte[] v = s.getBytes();
            return base16Decode(v, 0, v.length);
        }
        return null;
    }

    /**
     * 在ARRAY中追加一个元素。
     *
     * @param value 待追加的值。
     *
     * @return 返回this
     */
    public JsonNode append(JsonNode value) {
        if (value != null && _valueType == ValueType.ARRAY) {
            _array.add(value);
        }
        return this;
    }

    /**
     * 返回ARRAY中给定索引处的元素。
     *
     * 返回ARRAY中给定索引处的元素。
     *
     * @param index 索引值。
     *
     * @return 若索引有效则返回该索引处的元素，否则返回null。
     */
    public JsonNode get(int index) {
        if (_valueType == ValueType.ARRAY) {
            return _array.get(index);
        }
        return null;
    }

    /**
     * 在OBJECT中添加一个名值对。
     *
     * @param name 待添加的名字；
     * @param value 待添加的值。
     *
     * @return 返回this.
     */
    public JsonNode put(String name, JsonNode value) {
        if (value != null && _valueType == ValueType.OBJECT) {
            _dict.put(name, value);
        }
        return this;
    }

    /**
     * 从OBJECT中获得给定名字对应的值。
     *
     * @param name 名字。
     *
     * @return 若给定的名字存在则返回对应的值对象，否则返回null。
     */
    public JsonNode get(String name) {
        if (_valueType == ValueType.OBJECT) {
            return _dict.get(name);
        }
        return null;
    }

    /**
     * 从OBJECT中获得所有名字的列表。
     *
     * @return 若给定的名字存在返回true，否则返回false。
     */
    public Set<String> getNames() {
        if (_valueType == ValueType.OBJECT) {
            return Collections.unmodifiableSet(_dict.keySet());
        }
        return new HashSet<String>();
    }

    public static JsonNode parseJsonFile(String fileName) throws Exception {
        FileInputStream fis = new FileInputStream(fileName);
        try {
            return parseJsonDoc(fis);
        }
        finally {
            fis.close();
        }
    }

    public static JsonNode parseJsonDoc(Reader reader) throws Exception {
        JSONTokener tokener = new JSONTokener(reader);
        final char c = tokener.nextClean();
        tokener.back();
        if (c == '{') {
            JSONObject jobj = new JSONObject(tokener);
            JsonNode node = new JsonNode(ValueType.OBJECT);
            JSONObjectToJsonNode(jobj, node);
            return node;
        }
        else if (c == '[') {
            JSONArray jarray = new JSONArray(tokener);
            JsonNode node = new JsonNode(ValueType.ARRAY);
            JSONArrayToJsonNode(jarray, node);
            return node;
        }
        else {
            throw new Exception("neither JSON-Object nor JSON-Array");
        }
    }

    public static JsonNode parseJsonDoc(InputStream is) throws Exception {
        return parseJsonDoc(new InputStreamReader(is));
    }

    public static JsonNode parseJsonDoc(InputStream is, String charsetName) throws Exception {
        if (charsetName == null) {
            return parseJsonDoc(new InputStreamReader(is));
        } else {
            return parseJsonDoc(new InputStreamReader(is, charsetName));
        }
    }

    public static JsonNode parseJsonDoc(String s) throws Exception {
        return parseJsonDoc(new StringReader(s));
    }

    /**
     * Write the contents of the JsonNode as JSON text to a writer. For
     * compactness, no whitespace is added.
     *
     * @param writer Writes the serialized JSON
     * @param indentFactor The number of spaces to add to each level of indentation.
     * @param indent The indention of the top level.
     *
     * @return The writer.
     *
     * @throws Exception exception will be raised if failure
     */
    public Writer printJsonDoc(Writer writer, int indentFactor, int indent) throws Exception {
        if (_valueType == ValueType.OBJECT) {
            JSONObject jobj = new JSONObject();
            JsonNodeToJSONObject(this, jobj);
            return jobj.write(writer, indentFactor, indent);
        }

        if (_valueType == ValueType.ARRAY) {
            JSONArray jarray = new JSONArray();
            JsonNodeToJSONArray(this, jarray);
            return jarray.write(writer, indentFactor, indent);
        }

        return writer;
    }

    /**
     * Write the contents of the JsonNode as JSON text with no whitespace.
     *
     * @param indentFactor The number of spaces to add to each level of indentation.
     * @param indent The indention of the top level.
     *
     * @return JSON text.
     *
     * @throws Exception exception will be raised if failure
     */
    public String toPrettyString(int indentFactor, int indent) throws Exception {
        Writer w = printJsonDoc(new StringWriter(), indentFactor, indent);
        return w.toString();
    }

    /**
     * Write the contents of the JsonNode as JSON text with no whitespace
     * which is suitable for transfer over HTTP.
     *
     * @return JSON text.
     *
     * @throws Exception exception will be raised if failure
     */
    public String toCompactString() throws Exception {
        return toPrettyString(0, 0);
    }

    private static void JSONArrayToJsonNode(JSONArray jarray, JsonNode node) {
        final int N = jarray.length();
        for (int i=0; i<N; ++i) {
            Object val = jarray.get(i);
            addNode(node, null, val);
        }
    }

    private static void JSONObjectToJsonNode(JSONObject jobj, JsonNode node) {
        for (String key : jobj.keySet()) {
            Object val = jobj.get(key);
            addNode(node, key, val);
        }
    }

    private static void addNode(JsonNode parent, String key, Object val) {
        if (val == JSONObject.NULL) {
            addNode(parent, key, NULL_NODE);
        }
        else if (val == Boolean.TRUE) {
            addNode(parent, key, TRUE_NODE);
        }
        else if (val == Boolean.FALSE) {
            addNode(parent, key, FALSE_NODE);
        }
        else if (val instanceof String) {
            String s = (String)val;
            addNode(parent, key, new JsonNode(s));
        }
        else if (val instanceof Integer) {
            Integer n = (Integer)val;
            addNode(parent, key, new JsonNode(n.intValue()));
        }
        else if (val instanceof Long) {
            Long l = (Long)val;
            addNode(parent, key, new JsonNode(l.longValue()));
        }
        else if (val instanceof Double) {
            Double d = (Double)val;
            addNode(parent, key, new JsonNode(d.doubleValue()));
        }
        else if (val instanceof JSONObject) {
            JSONObject x = (JSONObject)val;
            JsonNode child = new JsonNode(ValueType.OBJECT);
            JSONObjectToJsonNode(x, child);
            addNode(parent, key, child);
        }
        else if (val instanceof JSONArray) {
            JSONArray x = (JSONArray)val;
            JsonNode child = new JsonNode(ValueType.ARRAY);
            JSONArrayToJsonNode(x, child);
            addNode(parent, key, child);
        }
        else {
            assert false : "unhandled type";
        }
    }

    private static void addNode(JsonNode parent, String key, JsonNode child) {
        if (parent._valueType == ValueType.ARRAY) {
            parent.append(child);
        } else {
            parent.put(key, child);
        }
    }

    private static void JsonNodeToJSONArray(JsonNode node, JSONArray jarray) {
        for (JsonNode child : node._array) {
            putElemToJSONArray(jarray, child);
        }
    }

    private static void putElemToJSONArray(JSONArray jarray, JsonNode child) {
        if (child == NULL_NODE || child._valueType == ValueType.NULL) {
            jarray.put(JSONObject.NULL);
        }
        else if (child == TRUE_NODE) {
            jarray.put(true);
        }
        else if (child == FALSE_NODE) {
            jarray.put(false);
        }
        else if (child._valueType == ValueType.BOOL) {
            Boolean b = (Boolean)child._value;
            jarray.put(b == Boolean.TRUE);
        }
        else if (child._valueType == ValueType.INT64) {
            Long l = (Long)child._value;
            jarray.put(l.longValue());
        }
        else if (child._valueType == ValueType.DOUBLE) {
            Double d = (Double)child._value;
            jarray.put(d.doubleValue());
        }
        else if (child._valueType == ValueType.STRING || child._valueType == ValueType.BINARY) {
            String s = (String)child._value;
            jarray.put(s);
        }
        else if (child._valueType == ValueType.ARRAY) {
            JSONArray a = new JSONArray();
            JsonNodeToJSONArray(child, a);
            jarray.put(a);
        }
        else if (child._valueType == ValueType.OBJECT) {
            JSONObject o = new JSONObject();
            JsonNodeToJSONObject(child, o);
            jarray.put(o);
        }
        else {
            assert false : "unhandled type";
        }
    }

    private static void JsonNodeToJSONObject(JsonNode node, JSONObject jobj) {
        for (Map.Entry<String, JsonNode> e : node._dict.entrySet()) {
            String key = e.getKey();
            JsonNode child = e.getValue();
            putElemToJSONObject(jobj, key, child);
        }
    }

    private static void putElemToJSONObject(JSONObject jobj, String key, JsonNode child) {

        if (child == NULL_NODE || child._valueType == ValueType.NULL) {
            jobj.put(key, JSONObject.NULL);
        }
        else if (child == TRUE_NODE) {
            jobj.put(key, true);
        }
        else if (child == FALSE_NODE) {
            jobj.put(key, false);
        }
        else if (child._valueType == ValueType.BOOL) {
            Boolean b = (Boolean)child._value;
            jobj.put(key, b == Boolean.TRUE);
        }
        else if (child._valueType == ValueType.INT64) {
            Long l = (Long)child._value;
            jobj.put(key, l.longValue());
        }
        else if (child._valueType == ValueType.DOUBLE) {
            Double d = (Double)child._value;
            jobj.put(key, d.doubleValue());
        }
        else if (child._valueType == ValueType.STRING || child._valueType == ValueType.BINARY) {
            String s = (String)child._value;
            jobj.put(key, s);
        }
        else if (child._valueType == ValueType.ARRAY) {
            JSONArray a = new JSONArray();
            JsonNodeToJSONArray(child, a);
            jobj.put(key, a);
        }
        else if (child._valueType == ValueType.OBJECT) {
            JSONObject o = new JSONObject();
            JsonNodeToJSONObject(child, o);
            jobj.put(key, o);
        }
        else {
            assert false : "unhandled type";
        }
    }

    // 在input[begin..end)中搜寻JSON-Object或JSON-Array, 返回JSON-Object或JSON-Array
    // 对应的区间[position[0]..position[1]).
    public static void scanJsonEntity(byte[] input, int begin, int end, int[] position) throws Exception {
        // 寻找输入流中json的开始边界-->
        int entityBegin = 0;
        char open_delimiter = 0;
        char close_delimiter = 0;
        char ch = 0;

        int p = begin;
        for (; p < end; ++ p) {
            ch = (char)input[p];
            if (!Character.isWhitespace(ch)) {
                break;
            }
        }

        if (ch == '{' || ch == '[') {
            open_delimiter = ch;
            close_delimiter = (ch == '{') ? '}' : ']';
            entityBegin = p;
            p ++;
        }
        else {
            throw new Exception("no json object or array");
        }
        //<-- 寻找输入流中json的开始边界

        // 寻找输入流中json的结束边界-->
        int entityEnd = 0;
        boolean isInQuotedStr = false;
        int nestedLevel = 1;
        for (; p < end; p ++) {
            ch = (char)input[p];
            if (ch == '"') {
                if (isInQuotedStr) {
                    char prev = (char)input[p-1];
                    if (prev == '\\') {
                        // 转义的双引号
                    }
                    else {
                        isInQuotedStr = false; //退出"..."
                    }
                }
                else {
                    isInQuotedStr = true; //进入"..."
                }
            }
            else if (!isInQuotedStr && ch == open_delimiter) {
                nestedLevel ++;
            }
            else if (!isInQuotedStr && ch == close_delimiter) {
                nestedLevel --;
                if (nestedLevel == 0) {
                    entityEnd = p + 1;
                    break;
                }
            }
        }

        if (entityEnd == 0) {
            throw new Exception("invalid json doc");
        }
        //<-- 寻找输入流中json的结束边界

        position[0] = entityBegin;
        position[1] = entityEnd;
    }

    private static boolean isBase16(byte octect) {
        int c = octect;
        return ('0' <= c && c <= '9') || ('a' <= c && c <= 'f')
                || ('A' <= c && c <= 'F');
    }

    private static boolean isBase16(final String str) {
        byte[] arrayOctect = str.getBytes();
        int length = arrayOctect.length;
        if (length == 0)
            return false;
        for (int i = 0; i < length; i++) {
            if (isBase16(arrayOctect[i]) == false)
                return false;
        }
        return true;
    }

    private static int intToHexChar(int n) {
        if (0 <= n && n <= 9)
            return '0' + n;
        if (10 <= n && n <= 15)
            return 'A' - 10 + n;
        return -1;
    }

    private static int hexCharToInt(int c) {
        if ('0' <= c && c <= '9')
            return c - '0';
        if ('a' <= c && c <= 'f')
            return c - 'a' + 10;
        if ('A' <= c && c <= 'F')
            return c - 'A' + 10;
        return -1;
    }

    private static byte[] base16Encode(byte[] binaryData, int offset, int nlen) {
        int nlen16 = 2 * nlen;
        byte[] b16 = new byte[nlen16];

        int j = 0;
        int end = offset + nlen;
        int hi, lo;

        for (int i = offset; i < end; ++i) {
            hi = (binaryData[i] >> 4) & 0x0F;
            lo = (binaryData[i]) & 0x0F;
            b16[j + 0] = (byte) intToHexChar(hi);
            b16[j + 1] = (byte) intToHexChar(lo);
            j += 2;
        }

        return b16;
    }

    public static byte[] base16Decode(byte[] base16Data, int offset, int nlen) {

        int binaryLen = nlen / 2;
        byte[] binaryData = new byte[binaryLen];

        int j = 0;
        int end = offset + nlen;
        int hi, lo;

        for (int i = offset; i < end;) {
            hi = hexCharToInt(base16Data[i + 0]);
            lo = hexCharToInt(base16Data[i + 1]);
            binaryData[j] = (byte) (((hi << 4) & 0xF0) | (lo & 0x0F));
            i += 2;
            j += 1;
        }

        return binaryData;
    }
}
