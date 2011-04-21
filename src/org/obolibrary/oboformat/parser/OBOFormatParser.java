package org.obolibrary.oboformat.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Frame.FrameType;
import org.obolibrary.oboformat.model.FrameMergeException;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.QualifierValue;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;

/**
 * implements the OBO Format 1.4 specification
 *
 */
public class OBOFormatParser {
	
	static final Logger LOG = Logger.getLogger(OBOFormatParser.class); 
	
	//final String DATA_VERSION = "data-version";
	//final String ID = "id";
	//final String NAME = "name";
    
    public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
	
	SimpleDateFormat headerDateFormat = new SimpleDateFormat("dd:MM:yyyy HH:mm");
	SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");

	protected enum ParseState {
		HEADER, BODY
	}
	
	/*protected enum Tag {
		ID,
		DATA_VERSION,
		NAMESPACE,
		NAME
	}
	
	public HashMap<String,Tag> tagMap = new HashMap<String,Tag>();
	
	protected void initTagMap() {
		tagMap.put("id",Tag.ID);
		
	}*/
	
	protected class MyStream {
		int pos=0;
		String line;
		int lineNo = 0;
		BufferedReader reader;
		
		public MyStream() {
			pos = 0;
			
		}

		public MyStream(BufferedReader r) {
			reader = r;
		}
		
		public char peekChar() {
			prepare();
			return line.charAt(pos);
		}
		
		public char nextChar() {
			pos++;
			return line.charAt(pos-1);
		}
		
		public String rest() {
			prepare();
			if (line == null)
				return null;
			if (pos >= line.length())
				return "";
			return line.substring(pos);
		}
		
		public void advance(int dist) {
			pos += dist;
		}
		
		public void prepare() {
			if (line == null)
				advanceLine();
		}
		
		public void advanceLine() {
			try {
				line = reader.readLine();
				lineNo++;
				pos = 0;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void forceEol() {
			if (line == null) {
				return;
			}
			pos = line.length();
		}

		
		public boolean eol() {
			prepare();
			if (line == null) {
				return false;
			}
			return pos >= line.length();
		}
		
		public boolean eof() {
			prepare();
			if (line == null) {
				return true;
			}
			return false;
		}
		
		public String getTag() {
			return "";
		}
		
		public boolean consume(String s) {
			String r = rest();
			if (r==null)
				return false;
			if (r.startsWith(s)) {
				pos += s.length();
				return true;
			}
			return false;
		}

		public int indexOf(char c) {
			prepare();
			if (line == null)
				return -1;
			return line.substring(pos).indexOf(c);
		}
		
		@Override
        public String toString() {
			return line + "//" + pos+" LINE:"+lineNo;
		}

		public boolean peekCharIs(char c) {
			if (eol() || eof())
				return false;
			return peekChar() == c;
		}

		public int getLineNo() {
			return lineNo;
		}
	}
	
	protected MyStream s;
	
	public OBOFormatParser() {
		super();
		this.s = new MyStream();
	}
	
	public OBOFormatParser(MyStream s) {
		super();
		this.s = s;
	}
	
	public void setReader(BufferedReader r) {
		this.s.reader = r;
	}

	/**
	 * Parses a local file to an OBODoc
	 * 
	 * @param fn
	 * @return parsed obo document
	 * @throws IOException
	 */
	public OBODoc parse(String fn) throws IOException {
		if (fn.startsWith("http:"))
			return parse(new URL(fn));
		 BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fn), DEFAULT_CHARACTER_ENCODING));
		 return parse(in);
	}	

	/**
	 * Parses a remote URL to an OBODoc
	 * 
	 * @param url
	 * @return parsed obo document
	 * @throws IOException
	 */
	public OBODoc parse(URL url) throws IOException {
	    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), DEFAULT_CHARACTER_ENCODING));
	    return parse(in);
	}	

	/**
	 * Parses a remote URL to an OBODoc
	 * 
	 * @param urlstr
	 * @return parsed obo document
	 * @throws IOException
	 */
	public OBODoc parseURL(String urlstr) throws IOException {
		URL url = new URL(urlstr);
		return parse(url);
	}	

	
	/**
	 * 
	 * 
	 * @param reader
	 * @return parsed obo document
	 * @throws IOException
	 */
	public OBODoc parse(BufferedReader reader) throws IOException {
		setReader(reader);

		OBODoc obodoc = new OBODoc();
		try {
			if (parseOBODoc(obodoc))
				return obodoc;
		}
		catch (Exception e) {
			LOG.error("Line:"+s.getLineNo(), e);
		}
		return null;
	}
	
	// ----------------------------------------
	// GRAMMAR
	// ----------------------------------------


	public boolean parseOBODoc(OBODoc obodoc) {
		Frame h = new Frame(FrameType.HEADER);
		obodoc.setHeaderFrame(h);
		parseHeaderFrame(h);
		while (parseEntityFrame(obodoc)) {
			
		}
		parseZeroOrMoreWsOptCmtNl();
		if (s.eof()) {
			return true;
		}
		else {
			LOG.warn("UNPARSED:"+s);
			return false;
		}
		
	}
	
	public void checkDanglingReferences(OBODoc doc) throws OBOFormatDanglingReferenceException{
		
		//check term frames 
		for(Frame f: doc.getTermFrames()){
			for(String tag: f.getTags()){
				OboFormatTag _tag = OBOFormatConstants.getTag(tag);
				
				Clause c = f.getClause(tag);
				if(_tag == OboFormatTag.TAG_INTERSECTION_OF 
						||_tag == OboFormatTag.TAG_UNION_OF
						||_tag == OboFormatTag.TAG_EQUIVALENT_TO
						||_tag == OboFormatTag.TAG_DISJOINT_FROM
						||_tag == OboFormatTag.TAG_RELATIONSHIP
						||_tag == OboFormatTag.TAG_IS_A){
					
					if(c.getValues().size() >1){
						checkRelation(c.getValue().toString(), tag, f.getId(), doc);
						checkClassReference(c.getValue2().toString(), tag, f.getId(), doc);
					}else
						checkClassReference(c.getValue().toString(), tag, f.getId(), doc);
				}
			}
		}
		
		
		//check typedef frames
		for(Frame f: doc.getTypedefFrames()){
			for(String tag: f.getTags()){
				OboFormatTag _tag = OBOFormatConstants.getTag(tag);
				
				Clause c = f.getClause(tag);
				if(_tag == OboFormatTag.TAG_IS_A
						||_tag == OboFormatTag.TAG_INTERSECTION_OF
						||_tag == OboFormatTag.TAG_UNION_OF
						||_tag == OboFormatTag.TAG_EQUIVALENT_TO
						||_tag == OboFormatTag.TAG_DISJOINT_FROM
						||_tag == OboFormatTag.TAG_INVERSE_OF
						||_tag == OboFormatTag.TAG_TRANSITIVE_OVER
						||_tag == OboFormatTag.TAG_DISJOINT_OVER
						){
					
					checkRelation(c.getValue().toString(),tag, f.getId(), doc);
				}else if(_tag == OboFormatTag.TAG_HOLDS_OVER_CHAIN
								|| _tag == OboFormatTag.TAG_EQUIVALENT_TO_CHAIN
								|| _tag == OboFormatTag.TAG_RELATIONSHIP
					){
					checkRelation(c.getValue().toString(),tag, f.getId(), doc);
					checkRelation(c.getValue2().toString(),tag, f.getId(), doc);
				}else if(_tag == OboFormatTag.TAG_DOMAIN 
						||_tag == OboFormatTag.TAG_RANGE
						){
						checkClassReference(c.getValue().toString(), tag, f.getId(), doc);
				}
			}
		}
		
		
	}

	private void checkRelation(String relId, String tag, String frameId, OBODoc doc) throws OBOFormatDanglingReferenceException{
		if(doc.getTypedefFrame(relId) == null){
			throw new OBOFormatDanglingReferenceException("The relation '" + relId+ "' reference in" +
					" the tag '" + tag +" ' in the frame of id '" + frameId +"' is not delclared");
		}
	}
	
	private void checkClassReference(String classId, String tag, String frameId, OBODoc doc)
		throws OBOFormatDanglingReferenceException{
		if(doc.getTermFrame(classId) == null){
			throw new OBOFormatDanglingReferenceException("The class '" + classId+ "' reference in" +
					" the tag '" + tag +" ' in the frame of id '" +  frameId +"'is not delclared");
		}
	}
	
	public boolean parseHeaderFrame(Frame h) {
		if (s.peekChar() == '[') 
			return false;
		while (parseHeaderClauseNl(h)) {
			
		}
		return true;
	}

	/**
	 * header-clause ::= format-version-TVP | ... | ...
	 */
	protected boolean parseHeaderClauseNl(Frame h) {
		parseZeroOrMoreWsOptCmtNl();
		if (s.peekChar() == '[') 
			return false;

		if (parseHeaderClause(h)) {
			//System.out.println("hdr// "+h);
			return forceParseNlOrEof();
		}
		else {
			return false;
		}
	}
	
	protected boolean parseHeaderClause(Frame h) {
		Clause cl = new Clause();
		String t = getParseTag();
		if (t == null)
			return false;
		cl.setTag(t);
		
		OboFormatTag tag = OBOFormatConstants.getTag(t);
		
		h.addClause(cl);
		if (tag == OboFormatTag.TAG_DATA_VERSION) {
			return parseUnquotedString(cl);
		}
		if (tag == OboFormatTag.TAG_FORMAT_VERSION) {
			return parseUnquotedString(cl);
		}
		if (tag == OboFormatTag.TAG_SYNONYMTYPEDEF) {
			return parseSynonymTypedef(cl);
		}
		if (tag == OboFormatTag.TAG_SUBSETDEF) {
			return parseSubsetdef(cl);
		}

		
		
		
		// default
		return parseUnquotedString(cl);
	}
	
	public boolean parseEntityFrame(OBODoc obodoc) {
		return parseTermFrame(obodoc) || parseTypedefFrame(obodoc);
	}
	
	// ----------------------------------------
	// [Term] Frames
	// ----------------------------------------


	/**
	 * term-frame ::= nl*  '[Term]' nl  id-Tag Class-ID EOL  { term-frame-clause EOL } 
	 */
	public boolean parseTermFrame(OBODoc obodoc)  {
		Frame f = new Frame(FrameType.TERM);
		parseZeroOrMoreWsOptCmtNl();
		if (s.consume("[Term]")) {
			forceParseNlOrEof();
			parseIdLine(f);
			while (parseTermFrameClauseEOL(f)) {
				
			}
			try {
				obodoc.addFrame(f);
			} catch (FrameMergeException e) {
				// this should never happen
				e.printStackTrace();
			}
			//System.out.println("parsed: "+f);
			return true;
		}
		return false;
	}

	/**
	 * 
	 */
	protected boolean parseTermFrameClauseEOL(Frame f) {
		// comment line:
		if (s.peekCharIs('!'))
			return parseHiddenComment() && forceParseNlOrEof();
		Clause cl = new Clause();
		if (parseTermFrameClause(cl)) {
			f.addClause(cl);
			return parseEOL(cl);
		}
		else {
			if (cl.getTag() != null) {
				LOG.warn("problem parsing tag:"+s);
			}
				
		}
		return false;
	}
	
	public boolean parseTermFrameClause(Clause cl) {
		
		String t = getParseTag();
		if (t == null)
			return false;
		
		OboFormatTag tag = OBOFormatConstants.getTag(t);
		
		cl.setTag(t);
		if (tag == OboFormatTag.TAG_IS_ANONYMOUS) {
			return parseBoolean(cl);
		}
		
		
		if (tag == OboFormatTag.TAG_NAME) {
			return parseUnquotedString(cl);
		}
		if (tag == OboFormatTag.TAG_NAMESPACE) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_ALT_ID) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_DEF) {
			return parseDef(cl);
		}
		if (tag == OboFormatTag.TAG_COMMENT) {
			return parseUnquotedString(cl);
		}
		if (tag == OboFormatTag.TAG_SUBSET) {
			// in the obof1.4 spec, subsets may not contain spaces.
			// unfortunately OE does not prohibit this, so subsets with spaces
			// frequently escape. We should either allow spaces in the spec
			// (with complicates parsing) or forbid them and reject all obo documents
			// that do not conform. Unfortunately that would limit the utility of
			// this parser, so for now we allow spaces. maybe make it strict again
			// when community is sufficiently forewarned.
			// (alternatively add smarts to OE to translate the spaces to underscores,
			// so it's a one-off translation)
			//
			//return parseIdRef(cl);
			return parseUnquotedString(cl);
		}
		if (tag == OboFormatTag.TAG_SYNONYM) {
			return parseSynonym(cl);
		}
		if (parseDeprecatedSynonym(t,cl)) {
			return true;
		}
		if (tag == OboFormatTag.TAG_XREF) {
			return parseDirectXref(cl);
		}
		if (tag == OboFormatTag.TAG_BUILTIN) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_PROPERTY_VALUE) {
			return parsePropertyValue(cl);
		}
		if (tag == OboFormatTag.TAG_IS_A) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_INTERSECTION_OF) {
			return parseTermIntersectionOf(cl);
		}
		if (tag == OboFormatTag.TAG_UNION_OF) {
			return parseIdRef(cl);
		}
		if (tag== OboFormatTag.TAG_EQUIVALENT_TO) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_DISJOINT_FROM) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_RELATIONSHIP) {
			return parseRelationship(cl);
		}
		if (tag == OboFormatTag.TAG_CREATED_BY) {
			return parsePerson(cl);
		}
		if (tag == OboFormatTag.TAG_CREATION_DATE) {
			return parseISODate(cl);
		}
		if (tag == OboFormatTag.TAG_IS_OBSELETE) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_REPLACED_BY) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_CONSIDER) {
			return parseIdRef(cl);
		}

		return false;
	}
	
	// ----------------------------------------
	// [Typedef] Frames
	// ----------------------------------------


	/**
	 * Typedef-frame ::= nl*  '[Typedef]' nl  id-Tag Class-ID EOL  { Typedef-frame-clause EOL } 
	 * @throws FrameMergeException 
	 */
	public boolean parseTypedefFrame(OBODoc obodoc)  {
		Frame f = new Frame(FrameType.TYPEDEF);
		parseZeroOrMoreWsOptCmtNl();
		if (s.consume("[Typedef]")) {
			forceParseNlOrEof();
			parseIdLine(f);
			while (parseTypedefFrameClauseEOL(f)) {
				
			}
			try {
				obodoc.addFrame(f);
			} catch (FrameMergeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("parsed: "+f);
			return true;
		}
		return false;
	}

	/**
	 * 
	 */
	protected boolean parseTypedefFrameClauseEOL(Frame f) {
		// comment line:
		if (s.peekCharIs('!'))
			return parseHiddenComment() && forceParseNlOrEof();

		Clause cl = new Clause();
	
		if (parseTypedefFrameClause(cl)) {
			f.addClause(cl);
			return parseEOL(cl);
		}
		return false;
	}
	
	public boolean parseTypedefFrameClause(Clause cl) {
		String t= getParseTag();
		if (t == null)
			return false;
		
		if (t.equals("is_metadata")) {
			LOG.info("is_metadata DEPRECATED; switching to is_metadata_tag");
			t = OboFormatTag.TAG_IS_METADATA_TAG.getTag();
		}
		
		OboFormatTag tag = OBOFormatConstants.getTag(t);

		cl.setTag(t);
		
		if (tag == OboFormatTag.TAG_IS_ANONYMOUS) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_NAME) {
			return parseUnquotedString(cl);
		}
		if (tag == OboFormatTag.TAG_NAMESPACE) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_ALT_ID) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_DEF) {
			return parseDef(cl);
		}
		if (tag == OboFormatTag.TAG_COMMENT) {
			return parseUnquotedString(cl);
		}
		if (tag == OboFormatTag.TAG_SUBSET) {
			return parseIdRef(cl);
		}
		if (tag== OboFormatTag.TAG_SYNONYM) {
			return parseSynonym(cl);
		}
		if (parseDeprecatedSynonym(t,cl)) {
			return true;
		}
		if (tag == OboFormatTag.TAG_XREF) {
			return parseDirectXref(cl);
		}
		if (tag == OboFormatTag.TAG_PROPERTY_VALUE) {
			return parsePropertyValue(cl);
		}
		if (tag == OboFormatTag.TAG_DOMAIN) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_RANGE) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_BUILTIN) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_ANTI_SYMMETRIC) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_CYCLIC) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_REFLEXIVE) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_SYMMETRIC) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_ASYMMETRIC) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_TRANSITIVE) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_FUNCTIONAL) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_INVERSE_FUNCTIONAL) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_A) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_INTERSECTION_OF) {
			return parseTypedefIntersectionOf(cl);
		}
		if (tag == OboFormatTag.TAG_UNION_OF) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_EQUIVALENT_TO) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_DISJOINT_FROM) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_INVERSE_OF) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_TRANSITIVE_OVER) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_HOLDS_OVER_CHAIN) {
			return parseIdRefPair(cl);
		}
		if (tag == OboFormatTag.TAG_EQUIVALENT_TO_CHAIN) {
			return parseIdRefPair(cl);
		}
		if (tag == OboFormatTag.TAG_DISJOINT_OVER) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_RELATIONSHIP) {
			return parseRelationship(cl);
		}
		if (tag == OboFormatTag.TAG_CREATED_BY) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_CREATION_DATE) {
			return parseISODate(cl);
		}
		if (tag == OboFormatTag.TAG_IS_OBSELETE) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_REPLACED_BY) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_CONSIDER) {
			return parseIdRef(cl);
		}
		if (tag == OboFormatTag.TAG_IS_METADATA_TAG) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_IS_CLASS_LEVEL_TAG) {
			return parseBoolean(cl);
		}
		if (tag == OboFormatTag.TAG_EXPAND_ASSERTION_TO) {
			return parseOwlDef(cl);
		}
		if (tag == OboFormatTag.TAG_EXPAND_EXPRESSION_TO) {
			return parseOwlDef(cl);
		}

		

		return false;
	}
	
	// ----------------------------------------
	// [Instance] Frames - TODO 
	// ----------------------------------------

	// ----------------------------------------
	// [Annotation] Frames - TODO 
	// ----------------------------------------


	// ----------------------------------------
	// TVP
	// ----------------------------------------

	
	private String getParseTag() {
		int i = s.indexOf(':');
		if (i == -1) {
			return null;
		}
		String tag = s.rest().substring(0, i);
		s.advance(i+1);
		parseWs();
		parseZeroOrMoreWs();
		return mapDeprecatedTag(tag);
	}
	
	private boolean parseId(Clause cl) {
		return parseIdRef(cl);
	}
	
	private boolean parseIdRef(Clause cl) {
		String id = getParseUntil(" !{");
		if (id == null || id.equals(""))
			return false;
		cl.addValue(id);
		
		return true;
	}
	
	private boolean parseIdRefPair(Clause cl) {
		if (parseIdRef(cl)) {
			if (parseOneOrMoreWs()) {
				return parseIdRef(cl);
			}
		}
		return false;
	}

	
	private boolean parsePerson(Clause cl) {
		return parseUnquotedString(cl);
	}


	private boolean parseISODate(Clause cl) {
		String dateStr = getParseUntil(" !{");
		Date date;
		try {
			date = isoDateFormat.parse(dateStr);
			cl.setValue(date);
			return true;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	
	private boolean parseSubsetdef(Clause cl) {
		parseIdRef(cl);
		parseOneOrMoreWs();
		
		if (s.consume("\"")) {
			String desc = getParseUntilAdv("\"");
			cl.addValue(desc);
		}else
			return false;
			
		return true;
	}
	
	
	private boolean parseSynonymTypedef(Clause cl) {
		parseIdRef(cl);
		parseOneOrMoreWs();
		
		if (s.consume("\"")) {
			String desc = getParseUntilAdv("\"");
			cl.addValue(desc);
			if (s.peekCharIs(' ')) {
				parseOneOrMoreWs();
				if (parseIdRef(cl)) {
					
				}
			}
			
		}
		return true;
	}


	private boolean parseRelationship(Clause cl) {
		return parseIdRef(cl) && parseOneOrMoreWs() && parseIdRef(cl);
	}

	private boolean parsePropertyValue(Clause cl) {
		return parseIdRef(cl) && parseOneOrMoreWs() && parseIdRef(cl);
	}

	/**
	 * intersection_of-Tag Class-ID | intersection_of-Tag Relation-ID Class-ID 
	 */
	private boolean parseTermIntersectionOf(Clause cl) {
		if (parseIdRef(cl)) {
			// consumed the first ID
			if (s.peekCharIs(' ')) {
				parseOneOrMoreWs();
				if (parseIdRef(cl)) {
					// differentia
				}
				
			}
			return true;
		}
		return false;
	}
	
	private boolean parseTypedefIntersectionOf(Clause cl) {
		// single values only
		return parseIdRef(cl);
	}
	
	// ----------------------------------------
	// Synonyms
	// ----------------------------------------

	private boolean parseDeprecatedSynonym(String tag, Clause cl) {
		String scope = null;
		if (tag.equals("exact_synonym")) {
			scope = "EXACT";
		}
		else if (tag.equals("narrow_synonym")) {
			scope = "NARROW";		
		}
		else if (tag.equals("broad_synonym")) {
			scope = "BROAD";
		}
		else if (tag.equals("related_synonym")) {
			scope = "RELATED";
		}
		else {
			return false;
		}
		cl.setTag(OboFormatTag.TAG_SYNONYM.getTag());
		if (s.consume("\"")) {
			String syn = getParseUntilAdv("\"");
			cl.setValue(syn);
			parseZeroOrMoreWs();
			return parseXrefList(cl);
		}
		return false;
	}

	private boolean parseSynonym(Clause cl) {
		if (s.consume("\"")) {
			String syn = getParseUntilAdv("\"");
			cl.setValue(syn);
			parseZeroOrMoreWs();
			if (s.peekCharIs('[')) {
				// neither scope nor type specified
				return parseXrefList(cl);
			}
			else if (parseSynonymScope(cl)) {
				parseZeroOrMoreWs();
				if (s.peekCharIs('[')) {
					return parseXrefList(cl);
				}
				else if (parseSynontmType(cl)) {
					parseZeroOrMoreWs();
					return parseXrefList(cl);
				}
			}
		}
		return false;
	}

	private boolean parseSynontmType(Clause cl) {
		return parseIdRef(cl);
	}

	private boolean parseSynonymScope(Clause cl) {
		return parseIdRef(cl);
	}

	// ----------------------------------------
	// Definitions
	// ----------------------------------------

	private boolean parseDef(Clause cl) {
		if (s.consume("\"")) {
			String def = getParseUntilAdv("\"");
			cl.setValue(def);
			parseZeroOrMoreWs();
			return parseXrefList(cl);
		}
		return false;
	}
	
	private boolean parseOwlDef(Clause cl) {
		if (s.consume("\"")) {
			String def = getParseUntilAdv("\"");
			cl.setValue(def);
			parseZeroOrMoreWs();
			return parseXrefList(cl);
		}
		return false;
	}

	// ----------------------------------------
	// XrefLists - e.g. [A:1, B:2, ... ]
	// ----------------------------------------

	private boolean parseXrefList(Clause cl) {
		if (s.consume("[")) {
			parseZeroOrMoreXrefs(cl);
			parseZeroOrMoreWs();
			return s.consume("]");
		}		
		return false;
	}

	private boolean parseZeroOrMoreXrefs(Clause cl) {
		if (parseXref(cl)) {
			while (s.consume(",") && parseXref(cl)) {
			
			}
		}
		return true;	
	}

	// an xref that supports a value of values in a clause
	private boolean parseXref(Clause cl) {
		parseZeroOrMoreWs();
		String id = getParseUntil("\",]!{");
		if (id != null) {
			id = id.replaceAll(" *$", "");
			if (id.contains(" ")) {
				// TODO
				LOG.warn("accepting bad xref with spaces:"+id);
			}
			Xref xref = new Xref(id);
			cl.addXref(xref);
			parseZeroOrMoreWs();
			if (s.peekCharIs('"')) {
				s.consume("\"");
				xref.setAnnotation(this.getParseUntilAdv("\""));
			}
			return true;
		}
		return false;
	}
	
	// an xref that is a direct value of a clause
	private boolean parseDirectXref(Clause cl) {
		parseZeroOrMoreWs();
		String id = getParseUntil("\",]!{");
		if (id != null) {
			id = id.replaceAll(" +\\Z", "");
			if (id.contains(" ")) {
				LOG.warn("accepting bad xref with spaces:<"+id+">");
			}
			Xref xref = new Xref(id);
			//cl.addXref(xref);
			cl.addValue(xref);
			parseZeroOrMoreWs();
			if (s.peekCharIs('"')) {
				s.consume("\"");
				xref.setAnnotation(this.getParseUntilAdv("\""));
			}
			return true;
		}
		return false;
	}

	// ----------------------------------------
	// Qualifier Value blocks - e.g. {a="1",b="foo", ...}
	// ----------------------------------------

	private boolean parseQualifierBlock(Clause cl) {
		if (s.consume("{")) {
			parseZeroOrMoreQuals(cl);
			parseZeroOrMoreWs();
			return s.consume("}");
			
		}
		return false;
	}

	private boolean parseZeroOrMoreQuals(Clause cl) {
		if (parseQual(cl)) {
			while (s.consume(",") && parseQual(cl)) {
			
			}
		}
		return true;	
	}

	private boolean parseQual(Clause cl) {
		parseZeroOrMoreWs();
		String q = getParseUntilAdv("=");
		if (q != null) {
			parseZeroOrMoreWs();
			String v;
			
			if (s.consume("\"")) {
				 v = getParseUntilAdv("\"");
			}
			else {
				LOG.warn("qualifier values should be enclosed in quotes. You have: "+q+"="+s.rest());
				// TODO - warn
				v = getParseUntil(" ,}");
			}
			QualifierValue qv = new QualifierValue(q,v);
			cl.addQualifierValue(qv);
			parseZeroOrMoreWs();
			return true;
		}
		return false;
	}

	// ----------------------------------------
	// Other
	// ----------------------------------------


	private boolean parseBoolean(Clause cl) {
		if (s.consume("true")) {
			cl.setValue(true);
			return true;
		}
		if (s.consume("false")) {
			cl.setValue(false);
			return true;
		}
		// throw
		return false;
	}

	private boolean parseNamespace(Clause cl) {
		return parseIdRef(cl);	
	}

	protected boolean parseIdLine(Frame f) {
		String t = getParseTag();
		
		
		OboFormatTag tag = OBOFormatConstants.getTag(t);
		
		
		if (tag != OboFormatTag.TAG_ID) {
			return false;
		}
		Clause cl = new Clause();
		cl.setTag(t);
		f.addClause(cl);
		String id = getParseUntil(" !{");
		if (id == null)
			return false;
		cl.addValue(id);
		f.setId(id);
		return parseEOL(cl);
	}
	
	// ----------------------------------------
	// End-of-line matter
	// ----------------------------------------

	
	private boolean parseEOL(Clause cl) {
		while (parseWs()) {
			
		}
		parseQualifierBlock(cl);
		parseHiddenComment();
		return forceParseNlOrEof();
	}

	private boolean parseHiddenComment() {
		while (parseWs()) {
			
		}
		if (s.eol() || s.eof())
			return false;
		if (s.peekChar() == '!') {
			s.forceEol();
			return true;
		}
		return false;
		
	}


	
	//
	protected boolean parseUnquotedString(Clause cl) {
		parseZeroOrMoreWs();
		String v = getParseUntil("!{");

		cl.setValue(v);
		//s.advanceLine();
		return true;
	}
	
	// Newlines, whitespace
	
	protected boolean forceParseNlOrEof() {
		while (parseWs()) {
			
		}
		if ( parseNlChar() ) 
			return true;
		if (s.eof())
			return true;
		throw new Error("expected newline instead of: "+s.rest()+" on line "+s.lineNo);
	}
	
	protected boolean parseZeroOrMoreWsOptCmtNl() {
		while (parseWsOptCmtNl()) {
			
		}
		return true;
	}
	
	protected boolean parseWsOptCmtNl() {
		parseZeroOrMoreWs();
		parseHiddenComment();
		if ( parseNlChar() ) 
			return true;
		return false;
	}


	// non-newline
	protected boolean parseWs() {
		if (s.eol()) {
			return false;
		}
		while (!s.eof() && s.peekChar() == ' ') {
			s.advance(1);
			return true;
		}
		return false;
	}
	
	protected boolean parseOneOrMoreWs() {
		if (s.eol() || s.eof()) {
			return false;
		}
		int n = 0;
		while (s.peekCharIs(' ')) {
			s.advance(1);
			n++;
		}
		return n>0;
	}

	protected boolean parseZeroOrMoreWs() {
		if (s.eol() || s.eof()) {
			return true;
		}
		
		while (s.peekCharIs(' ')) {
			s.advance(1);
		}
		return true;
	}


	protected boolean parseNlChar() {
		if (s.eol()) {
			s.advanceLine();
			return true;
		}
		return false;
	}
	
	private String getParseUntilAdv(String compl) {
		String ret = getParseUntil(compl);
		s.advance(1);
		return ret;
	}

	private String getParseUntil(String compl) {
		String r = s.rest();
		int i = 0;
		boolean hasEscapedChars = false;
		while (i < r.length()) {
			if (r.charAt(i) == '\\') {
				hasEscapedChars = true;
				i+=2; // Escape
				continue;
			}
			if (compl.contains(r.subSequence(i, i+1))) {
				break;
			}
			i++;
		}
		if (i==0)
			return "";
		String ret = r.substring(0, i);
		// TODO - replace escaped characters
		s.advance(i);
		return ret;
	}
	
	private String mapDeprecatedTag(String tag) {
		if (tag.equals("inverse_of_on_instance_level")) {
			return OboFormatTag.TAG_INVERSE_OF.getTag();
		}
		if (tag.equals("xref_analog")) {
			return OboFormatTag.TAG_XREF.getTag();
		}
		if (tag.equals("xref_unknown")) {
			return OboFormatTag.TAG_XREF.getTag();
		}
		if (tag.equals("instance_level_is_transitive")) {
			return OboFormatTag.TAG_IS_TRANSITIVE.getTag();
		}
		return tag;
	}

}