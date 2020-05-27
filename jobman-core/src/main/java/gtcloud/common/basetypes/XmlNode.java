package gtcloud.common.basetypes;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XmlNode
{
    private String _name = "";

    private String _text = "";

    private PropertiesEx  _props = new PropertiesEx();

    private XmlNode _parent = null;

    private ArrayList<XmlNode> _children = new ArrayList<XmlNode>();

    public XmlNode(String name) {
        if (name != null) {
            _name = name;
        }
    }

    public XmlNode() {
        // noop
    }

    // 获得节点的名字。如对于<wml>...</wml>，节点名字为“wml”
    public String getName() {
        return _name;
    }

    // 设置节点名字。
    public void setName(String name) {
        _name = name;
    }

    // 获得节点的文本。如对于<wml>http/1.0</wml>，节点文本为“http/1.0”
    public String getText() {
        return _text;
    }

    // 设置节点的文本。
    public void setText(String text) {
        _text = text;
    }

    // 获得当前节点的属性集。如对于<wml version="1.0" vendor="IBM">...</wml>，
    // 属性集将包含：
    // version --> 1.0
    // vendor --> IBM
    public PropertiesEx getProperties() {
        return _props;
    }

    // 获得儿子节点的个数
    public int getChildCount() {
        return _children.size();
    }

    // 获得指定索引处的儿子节点
    public Collection<XmlNode> getChildren() {
        return _children;
    }

    // 获得父节点。
    public XmlNode getParent() {
        return _parent;
    }

    // 增加一个子节点
    public void addChild(XmlNode child) {
        _children.add(child);
        child._parent = this;
    }

    // 根据名称查找儿子节点。
    //
    // @return 返回找到的儿子节点；若不存在返回null。
    //
    public XmlNode findChildByName(String name) {
        for (XmlNode ch : _children) {
            if (ch._name.equals(name)) {
                return ch;
            }
        }

        return null;
    }

    // 移除一个子节点，但并不释放节点对象。
    public boolean removeChild(XmlNode child) {
        return _children.remove(child);
    }

    // 解析XML文件，返回整个文档树的根节点。调用者应负责释放返回的节点对象。
    //
    // @param xmlFile XML文件名。
    //
    // @throws Exception 若解析失败将抛出异常Exception
    //
    public static XmlNode parseXmlFile(String xmlFile) throws Exception {
        FileInputStream fis = new FileInputStream(xmlFile);
        return parseXmlStream(fis);
    }

    public static XmlNode parseXmlStream(InputStream is) throws Exception {
        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = bf.newDocumentBuilder();
        Document doc = db.parse(is);
        doc.normalizeDocument();
        Element root = doc.getDocumentElement();

        XmlNode result = new XmlNode();

        visit_tree(result, root, 0);

        return result;
    }

    private static void visit_tree(XmlNode to_parent, Node from_parent, int level) {
        String tag = from_parent.getNodeName();
        to_parent.setName(tag);

        // 属性
        NamedNodeMap nnm = from_parent.getAttributes();
        for (int i = 0; i < nnm.getLength(); ++i) {
            Node nd = nnm.item(i);
            String nm = nd.getNodeName();
            String val = nd.getNodeValue();
            to_parent.getProperties().setProperty(nm, val);
        }

        NodeList nlist = from_parent.getChildNodes();
        final int N = nlist.getLength();
        for (int i = 0; i < N; ++ i) {
            Node nd = nlist.item(i);

            if (nd.getNodeType() == Node.ELEMENT_NODE) {
                XmlNode to_child = new XmlNode();
                to_parent.addChild(to_child);
                visit_tree(to_child, nd, level + 1);
                continue;
            }

            if (nd.getNodeType() == Node.TEXT_NODE) {
                String text = nd.getNodeValue().trim();
                if (text.length() > 0) {
                    to_parent._text += text;

                }
                continue;
            }
        }
    }

	// 将以下结构的XML片段转化成properties结构。
    //    <params>
    //        <param name="ncFactoryName" value="default" />
    //        <param name="ncFactoryThreadPoolSize" value="2" />
    //        <param name="ncAcceptorTableName" value="default" />
    //    </params>
	//
	public static void xmlParamsToProperties(XmlNode paramsNode, PropertiesEx outProps) {
		if (paramsNode == null) {
			return;
		}

		for (XmlNode ch : paramsNode.getChildren()) {
	        if (!ch.getName().equals("param")) {
	            continue;
	        }

			String name = ch.getProperties().getProperty("name");
			String valu = ch.getProperties().getProperty("value");			
			if (name != null && valu != null) {
				outProps.setProperty(name, valu);
			}
		}
	}
}
