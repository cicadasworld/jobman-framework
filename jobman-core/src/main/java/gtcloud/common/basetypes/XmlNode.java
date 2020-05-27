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

    // ��ýڵ�����֡������<wml>...</wml>���ڵ�����Ϊ��wml��
    public String getName() {
        return _name;
    }

    // ���ýڵ����֡�
    public void setName(String name) {
        _name = name;
    }

    // ��ýڵ���ı��������<wml>http/1.0</wml>���ڵ��ı�Ϊ��http/1.0��
    public String getText() {
        return _text;
    }

    // ���ýڵ���ı���
    public void setText(String text) {
        _text = text;
    }

    // ��õ�ǰ�ڵ�����Լ��������<wml version="1.0" vendor="IBM">...</wml>��
    // ���Լ���������
    // version --> 1.0
    // vendor --> IBM
    public PropertiesEx getProperties() {
        return _props;
    }

    // ��ö��ӽڵ�ĸ���
    public int getChildCount() {
        return _children.size();
    }

    // ���ָ���������Ķ��ӽڵ�
    public Collection<XmlNode> getChildren() {
        return _children;
    }

    // ��ø��ڵ㡣
    public XmlNode getParent() {
        return _parent;
    }

    // ����һ���ӽڵ�
    public void addChild(XmlNode child) {
        _children.add(child);
        child._parent = this;
    }

    // �������Ʋ��Ҷ��ӽڵ㡣
    //
    // @return �����ҵ��Ķ��ӽڵ㣻�������ڷ���null��
    //
    public XmlNode findChildByName(String name) {
        for (XmlNode ch : _children) {
            if (ch._name.equals(name)) {
                return ch;
            }
        }

        return null;
    }

    // �Ƴ�һ���ӽڵ㣬�������ͷŽڵ����
    public boolean removeChild(XmlNode child) {
        return _children.remove(child);
    }

    // ����XML�ļ������������ĵ����ĸ��ڵ㡣������Ӧ�����ͷŷ��صĽڵ����
    //
    // @param xmlFile XML�ļ�����
    //
    // @throws Exception ������ʧ�ܽ��׳��쳣Exception
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

        // ����
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

	// �����½ṹ��XMLƬ��ת����properties�ṹ��
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
