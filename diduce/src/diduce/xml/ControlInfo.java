package diduce.xml;

import java.io.*;
import java.util.*;

public class ControlInfo implements Serializable {

    private static final long serialVersionUID = -1255408464482336979L;

    // remove public and have getters instead

    // default value for all the program points is '*'
    public boolean isWatch = true;
	
    public List invariants;
    public String type = "*";
    public String num = "*";
    public String lineno = "*";
    public String write = "*";
    public String staticPoint = "*";
    public String op = "*"; 
    public String method = "*";
    public String target = "*";

    public ControlInfo() {
	invariants = new ArrayList();
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	//sb.append(isWatch);
	//sb.append(",");
	sb.append(invariants);
	sb.append(",type=");
	sb.append(type);
	sb.append(",num=");
	sb.append(num);
	sb.append(",lineno=");
	sb.append(lineno);
	sb.append(",write=");
	sb.append(write);
	sb.append(",static=");
	sb.append(staticPoint);
	sb.append(",op=");
	sb.append(op);
	sb.append(",method=");
	sb.append(method);
	sb.append(",target=");
	sb.append(target);
	return sb.toString();
    }
}
