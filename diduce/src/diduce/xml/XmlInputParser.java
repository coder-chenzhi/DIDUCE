package diduce.xml;

import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import java.util.*;

public class XmlInputParser {

    static final String CTLINFO_TAG = "ControlInfo";

    static final String EQUIVSET_TAG = "Equivalent";
    //static final String DESCR_TAG = "Descriptor";
    //static final String SPEC_TAG = "Spec";
    static final String POSITIVE_TAG = "Watch";
    static final String NEGETIVE_TAG = "DoNotWatch";
    static final String INVARIANT_TAG = "Invariant";

    static final String PROGRAM_POINT_TAG = "ProgramPoint";
    static final String METHOD_TAG = "Method";
    static final String TARGET_TAG = "Target";
    static final String JAVA_TYPE_TAG = "JavaType";

    // Program Point attributes
    static final String ATTR_TYPE = "type";
    static final String ATTR_NUM = "num";
    static final String ATTR_LINE = "line";
    static final String ATTR_WRITE = "write";
    static final String ATTR_STATIC = "static";
    static final String ATTR_OP = "op";

    // method attributes
    static final String ATTR_METHOD_NAME = "name";
    static final String ATTR_METHOD_PARAMS = "params";
    static final String ATTR_METHOD_RETURNS = "returns";

    // target attributes
    static final String ATTR_TARGET_FIELD = "field";

    private static final boolean debug = false;
    public static boolean isParsed = false;

    Document doc;

    /**
     * This info is static for time being. If there
     * are future enhancements to run many instances of diduce.run
     * that use multiple user control specifications, this data
     * will be non-static.
     */
    public static Collection equivSets = new ArrayList();
    public static List constraints = new ArrayList();
    public static List invariantSpecs = new ArrayList();

    public static String userSpecFile;   
 
    public XmlInputParser(String filename) throws Exception {

	// Create a builder factory
	DocumentBuilderFactory factory = DocumentBuilderFactory.
						newInstance();
	//factory.setValidating(false);
    
	// Create the builder and parse the file
	this.doc = factory.newDocumentBuilder().
				parse(new File(filename));
	XmlInputParser.userSpecFile = filename;
    }

    public void parse() {
	isParsed = true;
	Node root = getFirstElement(doc);	
	if (root.getNodeName().equals(CTLINFO_TAG)) {	
	    parseCtlInfo(root);
	} else {
	    // Error
	    tagError(CTLINFO_TAG);
	}
    }

    private void parseCtlInfo(Node ctlNode) {  
	if (!ctlNode.hasChildNodes()) {
	   stopWithError("No elements in the controlInfo block");
	}
	NodeList terms = ctlNode.getChildNodes();
	Node ctlTypeNode;

        for (int i = 0; i < terms.getLength(); i++) {
	    ctlTypeNode = terms.item(i);
	    if (ctlTypeNode.getNodeType() == Node.ELEMENT_NODE) {
		String ctlName = ctlTypeNode.getNodeName();
		
		// Equiv sets
		if (ctlName.equals(EQUIVSET_TAG)) {
        	    NodeList equivPoints = ctlTypeNode.getChildNodes();	
		    if (equivPoints.getLength() < 2) {
			stopWithError(
			"Minimum of two Watch points are required in an equivalent set");
		    }
		    List equivList = new ArrayList();
		    for (int j = 0; j < equivPoints.getLength(); j++) {
			Node ePoint = equivPoints.item(j);
	    		if (ePoint.getNodeType() == Node.ELEMENT_NODE) {
	        	    ControlInfo ci = new ControlInfo();	
			    ci.isWatch = true;
			    fillInProgPointSpec(getProgramPoint(ePoint), ci);
			    equivList.add(ci);
			}
		    }
		    equivSets.add(equivList);
		
		} else if (ctlName.equals(INVARIANT_TAG)) { // invariant block
		    if (!ctlTypeNode.hasChildNodes()) {
	   		stopWithError("No elements in the Invariant block");
		    }
		    ControlInfo cInfo = new ControlInfo();	
		    NodeList invariantNodes = ctlTypeNode.getChildNodes();
		    Node invariantNode;
		    Node programPoint = null;
        	    for (int j = 0; j < invariantNodes.getLength(); j++) {
	    		invariantNode = invariantNodes.item(j);
	    		if (invariantNode.getNodeType() == Node.ELEMENT_NODE) {
			    String invariantName = invariantNode.getNodeName();
			    if (invariantName.equals(PROGRAM_POINT_TAG)) {
			        programPoint = invariantNode; 
			    } else {
			        cInfo.invariants.add(invariantName);
			    }
			}
		    }
		    if (programPoint == null) {
	   		stopWithError("No Program Point in the Invariant block");
	            }
		    fillInProgPointSpec(programPoint, cInfo);
	            invariantSpecs.add(cInfo);
		} else {				// watch/doNotWatch blocks
		    ControlInfo cInfo = new ControlInfo();	
		    if (ctlName.equals(POSITIVE_TAG)) {
		        cInfo.isWatch = true;
		    } else if (ctlName.equals(NEGETIVE_TAG)) {
		        cInfo.isWatch = false;
		    } else {
			stopWithError("Invalid tag:" + ctlName);
		    }
		    fillInProgPointSpec(getProgramPoint(ctlTypeNode), cInfo);
	            constraints.add(cInfo);
		}
	    }
	}
    }

    private Node getProgramPoint(Node node) {
	NodeList points = node.getChildNodes();
	Node progPoint;
        for (int i = 0; i < points.getLength(); i++) {
	    progPoint = points.item(i);
	    if (progPoint.getNodeType() == Node.ELEMENT_NODE) {
		if (progPoint.getNodeName().equals(PROGRAM_POINT_TAG)) {
		    return progPoint;
		} else {
		    stopWithError("Invalid tag:" + progPoint.getNodeName());
		}
	    }
	}
	stopWithError("No elements in the ProgramPoint block");
	return null;
    }
   
    private ControlInfo fillInProgPointSpec(
			Node progPoint, ControlInfo cInfo) { 

	// read all the attributes
	NamedNodeMap attrs;
	if ((attrs = progPoint.getAttributes()) != null) {
	    if (debug) {
	         printAttrs(attrs);
	    }
            for (int i = 0; i < attrs.getLength(); i++) {
		Node attr = attrs.item(i);
		String name = attr.getNodeName();
		String value = attr.getNodeValue();

	        if (name.equals(ATTR_TYPE)) {
		    cInfo.type = value;
		    /* isValidType doesn't handle '|' yet
		    if (isValidType(value)) {
		        cInfo.type = value;
		    } else {
		        stopWithError("Invalid attribute value: " + value);
		    }*/
		} else if (name.equals(ATTR_NUM)) {
		    cInfo.num = value;
		} else if (name.equals(ATTR_LINE)) {
		    cInfo.lineno = value;
		} else if (name.equals(ATTR_WRITE)) {
		    if (isValidBoolean(value)) {
		        cInfo.write = value;
		    } else { 
		        stopWithError("Invalid attribute value: " + value);
		    }
		} else if (name.equals(ATTR_STATIC)) {
		    if (isValidBoolean(value)) {
		        cInfo.staticPoint = value;
		    } else { 
		        stopWithError("Invalid attribute value: " + value);
		    }
		} else if (name.equals(ATTR_OP)) {
		    if (isValidOp(value)) {
		        cInfo.op = value;
		    } else { 
		        stopWithError("Invalid attribute value: " + value);
		    }
		} else {
		    stopWithError("Invalid attribute type: " + name);
		}
            }
        }
	if (!progPoint.hasChildNodes()) {
	    return cInfo;
        }
        NodeList morePoints = progPoint.getChildNodes();	
        for (int i = 0; i < morePoints.getLength(); i++) {
	    Node pPoint = morePoints.item(i);

	    if (pPoint.getNodeType() == Node.ELEMENT_NODE) {

		String nodeName = pPoint.getNodeName();

		// check for method specification
		if (nodeName.equals(METHOD_TAG)) {
		    cInfo.method = getMethodInfo(pPoint);
		} else if (nodeName.equals(TARGET_TAG)) {
		    cInfo.target = getTargetInfo(pPoint);
		} else {
		    stopWithError("Invalid Node name: " + nodeName);
		}
	    }
	}
	if (debug) {
	    System.out.println("XML parser output:" + cInfo);
	}
	return cInfo;
    }


    private boolean isValidType(String type) {
	return (type.equals("int") || 
		type.equals("char") || 
		type.equals("byte") || 
		type.equals("boolean") || 
		type.equals("short") || 
		type.equals("long") || 
		type.equals("float") || 
		type.equals("double") || 
		type.equals("object") || 
		type.equals("array"));
    }


    private boolean isValidBoolean(String bool) {
	return (bool.equals("true") || bool.equals("false"));
    }


    private boolean isValidOp(String op) {
	return (op.equals("param") ||
		op.equals("retval") ||
		op.equals("field") ||
		op.equals("array"));
    }


    // Unused now.
    private String getMethodSignature(Node methodPoint) {
	NamedNodeMap attrs;
	String sign = null;
	if ((attrs = methodPoint.getAttributes()) != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
	        Node attr = attrs.item(i);

	        // FIX, only one attr node is present
	        if (attr.getNodeName().equals(ATTR_METHOD_NAME)) {
		    sign = attr.getNodeValue();
	        } else {
	            tagError(ATTR_METHOD_NAME);
	        }
	    }
	} else {
	    tagError(ATTR_METHOD_NAME);
	}
	return sign;
    }

    private String getMethodInfo(Node methodPoint) {
	NamedNodeMap attrs;
	String name = null;
	String params = null;
	String returns = null;

	if ((attrs = methodPoint.getAttributes()) != null) {
	    if (debug) {
	         printAttrs(attrs);
	    }
            for (int i = 0; i < attrs.getLength(); i++) {
		Node attr = attrs.item(i);
	        if (attr.getNodeName().equals(ATTR_METHOD_NAME)) {
		    name = attr.getNodeValue();
		}
	        if (attr.getNodeName().equals(ATTR_METHOD_PARAMS)) {
		    params = convertToJVMFormat(attr.getNodeValue());
		}
	        if (attr.getNodeName().equals(ATTR_METHOD_RETURNS)) {
		    returns = attr.getNodeValue();
		    if (returns.equals("void")) {
			returns = "V";
		    } else {
			returns = convertToJVMFormat(returns);
		    }
		}
	    }
	}

	String signature = ((params == null) ? "(*" : ("(" + params)) +
		((returns == null) ? ")*" : (")" + returns));

	return (((name == null) ? "*" : name) + signature);
		//(signature.equals("") ? "*" : signature);
    }

    static String convertToJVMFormat(String typeList) {

	StringBuffer formatted = new StringBuffer("");
	StringTokenizer tokens = new StringTokenizer(typeList, ",");

	while (tokens.countTokens() > 0) {
	    String typeStr = tokens.nextToken().trim();
	    if (debug) {
	        System.out.println("next token:" + typeStr);
	    }

	    if (typeStr.indexOf("[") != -1) {
		int nextIndex;
		for (nextIndex = typeStr.indexOf("[");
			(nextIndex < typeStr.length()) &&
			    (typeStr.charAt(nextIndex) == '[')
			;nextIndex++) {
		    nextIndex++; // move to ']'
		    if ((nextIndex >= typeStr.length()) ||
			    (typeStr.charAt(nextIndex) != ']')) {
			tagError(ATTR_METHOD_PARAMS);
		    }
		    formatted.append("[");
		}
		if (nextIndex < typeStr.length()) { 
		    tagError(ATTR_METHOD_PARAMS);
		}
		typeStr = typeStr.substring(0, typeStr.indexOf("["));
		if (typeStr.length() == 0) {
		    stopWithError("No type for the array subscript");
		}
	    }

	    if (typeStr.equals("byte")) {
		formatted.append('B');	
	    } else if (typeStr.equals("char")) {
		formatted.append('C');
	    } else if (typeStr.equals("double")) {
		formatted.append('D');
	    } else if (typeStr.equals("float")) {
		formatted.append('F');
	    } else if (typeStr.equals("int")) {
		formatted.append('I');
	    } else if (typeStr.equals("long")) {
		formatted.append('J');
	    } else if (typeStr.equals("short")) {
		formatted.append('S');
	    } else if (typeStr.equals("boolean")) {
		formatted.append('Z');
	    } else if (typeStr.equals("*")) {
		formatted.append('*');
	    } else if (!typeStr.equals("")) { 
		
		// could be any Object
		formatted.append('L');
		formatted.append(typeStr.replace('.', '/'));	
		formatted.append(';');
	    }
	}
	if (debug) {
	    System.out.println("JVM formatter returns:" + formatted);
	}
	return formatted.toString();
    }

    private String getTargetInfo(Node targetPoint) {
	NamedNodeMap attrs;
	String fieldName;

	if ((attrs = targetPoint.getAttributes()) != null) {
	    if (debug) {
	         printAttrs(attrs);
	    }
            for (int i = 0; i < attrs.getLength(); i++) {
		Node attr = attrs.item(i);
	        if (attr.getNodeName().equals(ATTR_TARGET_FIELD)) {
		    return attr.getNodeValue();
		}
	    }
	}

	// may contain a method
   
    	Node methodTarget = getFirstElement(targetPoint);
	return getMethodInfo(methodTarget);
    }


    // Unused now
    private int getNumber(String number, String tag) {
	int val;
	try {
	    val = Integer.parseInt(number);
	} catch (NumberFormatException e) {
	    throw new IllegalArgumentException("Illegal value for tag:" +
			tag + " exception:" + e);
	}
	return val;
    }

    // Unused now
    private boolean getBoolean(String boolStr) {
	return Boolean.valueOf(boolStr).booleanValue();
    }

    private Node getFirstElement(Node node) {
	Node next = node.getFirstChild();
	while (next.getNodeType() != Node.ELEMENT_NODE) {
	    next = next.getNextSibling();
	}
	if (debug) {
	    System.out.println("getFirstElement, returns:" +
				next.getNodeName());
	}
	return next;
    }		

    static void tagError(String tag) {	
	System.out.println("Error: stopWithError in parsing the tag: " + tag);
	System.exit(1);
    }

    static void stopWithError(String msg) {	
	System.out.println("Error:" + msg);
	System.exit(1);
    }

    public static void main(String args[]) throws Exception {
	    XmlInputParser parser = new XmlInputParser(args[0]);
            parser.parse();
            System.out.println("Points:" + XmlInputParser.constraints);
            Collection sets = XmlInputParser.equivSets;
            System.out.println("size:" + sets.size());
            System.out.println("Set:" + sets);
    }

    static void printAttrs(NamedNodeMap attrs) {
	System.out.print("    attrs: ");
        for (int i = 0; i < attrs.getLength(); i++) {
	    Node attr = attrs.item(i);
	    System.out.print("name:" + attr.getNodeName());
	    System.out.println(", value: " + attr.getNodeValue());
        }
    }
}
