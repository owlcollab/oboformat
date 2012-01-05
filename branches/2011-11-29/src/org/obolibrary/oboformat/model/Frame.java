package org.obolibrary.oboformat.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;

public class Frame {
	
	public enum FrameType {
		HEADER, TERM, TYPEDEF, INSTANCE, ANNOTATION
	}
	
	
	protected Collection<Clause> clauses;
	protected String id;
	protected FrameType type;
	
	
	public Frame() {
		super();
		init();
	}
	
	public Frame(FrameType type) {
		super();
		init();
		this.type = type;
	}

	protected void init() {
		clauses = new ArrayList<Clause>();
	}


	public FrameType getType() {
		return type;
	}

	public void setType(FrameType type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}



	public Collection<Clause> getClauses() {
		return clauses;
	}
	
	public Collection<Clause> getClauses(String tag) {
		Collection<Clause> cls = new ArrayList<Clause>();
		for (Clause cl: clauses) {
			if (cl.getTag().equals(tag)) {
				cls.add(cl);
			}
		}
		return cls;
	}
	
	public Collection<Clause> getClauses(OboFormatTag tag) {
		return getClauses(tag.getTag());
	}

	
	/**
	 * @param tag
	 * @return null if no value set, otherwise first value
	 */
	public Clause getClause(String tag) {
		Collection<Clause> tagClauses = getClauses(tag);
		if (tagClauses.size() == 0)
			return null;
		return tagClauses.iterator().next(); // TODO - throw if > 1
	}

	public Clause getClause(OboFormatTag tag) {
		return getClause(tag.getTag());
	}

	public void setClauses(Collection<Clause> clauses) {
		this.clauses = clauses;
	}

	public void addClause(Clause cl) {
		clauses.add(cl);	
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Frame(");
		sb.append(id);
		sb.append(' ');
		for (Clause cl: clauses) {
			sb.append(cl.toString());
		}
		sb.append(')');
		return sb.toString();
	}

	public Object getTagValue(String tag) {
		if (getClause(tag) == null)
			return null;
		return getClause(tag).getValue();
	}
	
	public Object getTagValue(OboFormatTag tag) {
		return getTagValue(tag.getTag());
	}

	public <T> T getTagValue(String tag, Class<T> cls) {
		if (getClause(tag) == null)
			return null;
		Object value = getClause(tag).getValue();
		if (value != null && value.getClass().isAssignableFrom(cls)) {
			return cls.cast(value);
		}
		return null;
	}
	
	public <T> T getTagValue(OboFormatTag tag, Class<T> cls) {
		return getTagValue(tag.getTag(), cls);
	}
	
	public Collection<Object> getTagValues(OboFormatTag tag) {
		return getTagValues(tag.getTag());
	}
	
	public Collection<Object> getTagValues(String tag) {
		Collection<Object> vals = new Vector<Object>();
		for (Clause c : getClauses(tag)) {
			vals.add(c.getValue());
		}
		return vals;
	}
	
	public <T> Collection<T> getTagValues(OboFormatTag tag, Class<T> cls) {
		return getTagValues(tag.getTag(), cls);
	}
	
	public <T> Collection<T> getTagValues(String tag, Class<T> cls) {
		Collection<T> vals = new Vector<T>();
		for (Clause c : getClauses(tag)) {
			vals.add(c.getValue(cls));
		}
		return vals;
	}

	public Collection<Xref> getTagXrefs(String tag) {
		Collection<Xref> xrefs = new Vector<Xref>();
		for (Object ob : getClause(tag).getValues()) {
			if (ob instanceof Xref) {
				xrefs.add((Xref)ob);
			}
		}
		return xrefs;
	}

	public Set<String> getTags() {
		Set<String> tags = new HashSet<String>();
		for (Clause cl : getClauses()) {
			tags.add(cl.getTag());
		}
		return tags;
	}

	public void merge(Frame extFrame) throws FrameMergeException {
		
		if(this == extFrame)
			return;
		
		if (!extFrame.getId().equals(getId())) {
			throw new FrameMergeException("ids do not match");
		}
		if (!extFrame.getType().equals(getType())) {
			throw new FrameMergeException("frame types do not match");
		}
		for (Clause c : extFrame.getClauses()) {
			addClause(c);
		}
		// note we do not perform a document structure check at this point
	}
	
	public void check() throws FrameStructureException {
		Collection<Clause> iClauses = getClauses(OboFormatTag.TAG_INTERSECTION_OF);
		if (iClauses.size() == 1) {
			throw new FrameStructureException(this, "single intersection_of tags are not allowed");
		}
		if (getClauses(OboFormatTag.TAG_DEF).size() > 1) {
			throw new FrameStructureException(this, "multiple def tags not allowed");	
		}
		
	}


}