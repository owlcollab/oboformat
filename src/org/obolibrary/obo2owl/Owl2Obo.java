package org.obolibrary.obo2owl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.obolibrary.oboformat.model.*;
import org.obolibrary.oboformat.model.Frame.FrameType;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

public class Owl2Obo {

	private static Logger LOG = Logger.getLogger(Owl2Obo.class);

	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLOntology owlOntology;
	OWLDataFactory fac;
	OBODoc obodoc;
	Map<String, String> idSpaceMap;
	// Map<String,IRI> annotationPropertyMap;
	public static Map<String, String> annotationPropertyMap = initAnnotationPropertyMap();
	Set<OWLAnnotationProperty> apToDeclare;

	//private String ontologyId;
	
	
	private void init() {
		idSpaceMap = new HashMap<String, String>();
		manager = OWLManager.createOWLOntologyManager();
		fac = manager.getOWLDataFactory();
		apToDeclare = new HashSet<OWLAnnotationProperty>();
	}

	private static HashMap<String, String>  initAnnotationPropertyMap() {
		/*annotationPropertyMap = new HashMap<String, String>();
		annotationPropertyMap.put(OWLRDFVocabulary.RDFS_LABEL.getIRI()
				.toString(), "name");
		annotationPropertyMap.put(OWLRDFVocabulary.RDFS_COMMENT.getIRI()
				.toString(), "comment");
		annotationPropertyMap.put(Obo2Owl.DEFAULT_IRI_PREFIX + "IAO_0000424",
				"expand_expression_to");
		annotationPropertyMap.put(Obo2Owl.DEFAULT_IRI_PREFIX + "IAO_0000425",
				"expand_assertion_to");
		annotationPropertyMap.put(Obo2Owl.DEFAULT_IRI_PREFIX + "IAO_0000115", "def");
		annotationPropertyMap
				.put(Obo2Owl.DEFAULT_IRI_PREFIX + "IAO_0000118", "synonym");
		annotationPropertyMap.put(Obo2Owl.DEFAULT_IRI_PREFIX + "IAO_0000427",
				"is_anti_symmetric");
		annotationPropertyMap.put(OBO2 DEFAULT_IRI_PREFIX + "IAO_0100001",
				"replaced_by");
		annotationPropertyMap
				.put(DEFAULT_IRI_PREFIX + "remark", "data-version");*/
		
		HashMap<String, String> map = new HashMap<String, String>();
		for(String key: Obo2Owl.annotationPropertyMap.keySet()){
			IRI propIRI =Obo2Owl.annotationPropertyMap.get(key);
			map.put(propIRI.toString(), key);
		}
		
		return map;

	}

	public OWLOntologyManager getManager() {
		return manager;
	}

	public void setManager(OWLOntologyManager manager) {
		this.manager = manager;
	}

	public OBODoc getObodoc() {
		return obodoc;
	}

	public void setObodoc(OBODoc obodoc) {
		this.obodoc = obodoc;
	}

	public OBODoc convert(OWLOntology ont) throws OWLOntologyCreationException {
		this.owlOntology = ont;
		init();
		return tr();
	}

	private OBODoc tr() throws OWLOntologyCreationException {
		obodoc = new OBODoc();

		tr(owlOntology);

		for (OWLAxiom ax : owlOntology.getAxioms()) {
			if (ax instanceof OWLDeclarationAxiom) {
				tr((OWLDeclarationAxiom) ax);
			} else if (ax instanceof OWLSubClassOfAxiom) {
				tr((OWLSubClassOfAxiom) ax);
			} else if (ax instanceof OWLDisjointClassesAxiom) {
				tr((OWLDisjointClassesAxiom) ax);
			} else if (ax instanceof OWLEquivalentClassesAxiom) {
				tr((OWLEquivalentClassesAxiom) ax);
			}else if (ax instanceof OWLClassAssertionAxiom){
				tr((OWLClassAssertionAxiom)ax);
			}else {
				LOG.warn("Cann't Translate axiom: " + ax);
			}
			// tr(ax);
		}
		return obodoc;
	}

	private void add(Frame f) {
		if (f != null) {
			try {
				this.obodoc.addFrame(f);
			} catch (Exception ex) {
				LOG.error("", ex);
			}
		}
	}

	private void tr(OWLAnnotationAssertionAxiom aanAx, Frame frame) {

		OWLAnnotationProperty prop = aanAx.getProperty();
		String tag = owlObjectToTag(prop);

		if (tag != null) {
			String value = ((OWLLiteral) aanAx.getValue()).getLiteral();
			if ("id".equals(tag))
				frame.setId(value);

			Clause clause = new Clause();
			clause.setTag(tag);
			clause.addValue(value);
			frame.addClause(clause);
			
			if("def".equals(tag)){
				for(OWLAnnotation aan: aanAx.getAnnotations()){
					String propId = owlObjectToTag(aan.getProperty());
					
					if("xref".equals(propId)){
						String xrefValue = ((OWLLiteral) aan.getValue()).getLiteral();
						Xref xref = new Xref(xrefValue);
						clause.addXref(xref);
					}
				}
			}else if("synonym".equals(tag)){
				String scope = null;
				String type = null;
				for(OWLAnnotation aan: aanAx.getAnnotations()){
					String propId = owlObjectToTag(aan.getProperty());
					
					if("xref".equals(propId)){
						String xrefValue = ((OWLLiteral) aan.getValue()).getLiteral();
						Xref xref = new Xref(xrefValue);
						clause.addXref(xref);
					}else if("scope".equals(propId)){
						scope = ((OWLLiteral) aan.getValue()).getLiteral();
					}else if("type".equals(propId)){
						type = ((OWLLiteral) aan.getValue()).getLiteral();
					}
				}
				
				
				if(scope != null){
					clause.addValue(scope);
					
					if(type != null){
						clause.addValue(type);
					}
				}
				
			}

			
			
		}

	}
	
	public static String getOntologyId(OWLOntology ontology){
		String id = getIdentifier(ontology.getOntologyID().getOntologyIRI());

		int index = id.lastIndexOf(".owl");
		if(index>0){
			id = id.substring(0, index);
		}
		
		return id;
	}

	private void tr(OWLOntology ontology) {
		Frame f = new Frame(FrameType.HEADER);

		this.obodoc.setHeaderFrame(f);

		String id = getOntologyId(this.owlOntology);
		//this.ontologyId = id;
		
		Clause c = new Clause();
		c.setTag("ontology");
		c.setValue(id);
		f.addClause(c);

		for (OWLAnnotationAssertionAxiom aanAx : ontology
				.getAnnotationAssertionAxioms(ontology.getOntologyID()
						.getOntologyIRI())) {
			tr(aanAx, f);
		}

	}

	private void tr(OWLEquivalentClassesAxiom ax) {

		List<OWLClassExpression> list = ax.getClassExpressionsAsList();

		OWLClassExpression ce1 = list.get(0);
		OWLClassExpression ce2 = list.get(1);

		String cls2 = getIdentifier(ce2);

		Frame f = getTermFrame((OWLEntity) ce1);

		if (f == null) {
			LOG.warn("Cann't Translate axion: " + ax);
			return;
		}

		if (cls2 != null) {
			Clause c = new Clause();
			c.setTag("equivalent_to");
			c.setValue(cls2);
			f.addClause(c);
		} else if (ax instanceof OWLObjectUnionOf) {
			List<OWLClassExpression> list2 = ((OWLObjectUnionOf) ax)
					.getOperandsAsList();
			Clause c = new Clause();
			c.setTag("union_of");
			c.setValue(getIdentifier(list2.get(0)));
			f.addClause(c);
		} else if (ax instanceof OWLObjectIntersectionOf) {

			List<OWLClassExpression> list2 = ((OWLObjectIntersectionOf) ax).getOperandsAsList();
			Clause c = new Clause();
			c.setTag("intersection_of");
			OWLClassExpression ce = list2.get(0);
			String r = null;
			cls2 = getIdentifier(list.get(0));
			if(ce instanceof OWLObjectSomeValuesFrom ){
				OWLObjectSomeValuesFrom ristriction = (OWLObjectSomeValuesFrom)ce;
				r = getIdentifier(ristriction.getProperty());
				cls2 = getIdentifier(ristriction.getFiller());
			}
			
			if(r != null)
				c.addValue(r);
			
			c.addValue(cls2);
			f.addClause(c);
		}

	}

	private void tr(OWLDisjointClassesAxiom ax) {
		List<OWLClassExpression> list = ax.getClassExpressionsAsList();
		String cls2 = getIdentifier(list.get(1));

		Frame f = getTermFrame((OWLEntity) list.get(0));
		Clause c = new Clause();
		c.setTag("disjoint_from");
		c.setValue(cls2);
		f.addClause(c);
	}

	private void tr(OWLDeclarationAxiom axiom) {
		OWLEntity entity = axiom.getEntity();

		Set<OWLAnnotationAssertionAxiom> set  = entity.getAnnotationAssertionAxioms(this.owlOntology);

		if(set.isEmpty())
			return;
		
		Frame f = null;
		if (entity instanceof OWLClass) {
			f = new Frame(FrameType.TERM);
		} else if (entity instanceof OWLObjectProperty) {
			f = new Frame(FrameType.TYPEDEF);
		}

		if (f != null) {
			for (OWLAnnotationAssertionAxiom aanAx : set) {
				
				tr(aanAx, f);
			}

			add(f);
		
		}

	}

	
	public static String getIdentifier(OWLObject obj) {
		if(obj instanceof OWLEntity)
			return getIdentifier(((OWLEntity)obj).getIRI());
		
		return null;
	}

	public static String getIdentifier(IRI iriId) {
		String iri = iriId.toString();
		if (iri.startsWith("http://purl.obolibrary.org/obo/")) {
			iri = iri.replace("http://purl.obolibrary.org/obo/", "");
			int p = iri.indexOf('_');

			if (p >= 0) {
				iri = iri.substring(0, p) + ":" + iri.substring(p + 1);
			}
		}

		return iri;

	}
	
	
	public static String owlObjectToTag(OWLObject obj){
		
		if(!(obj instanceof OWLNamedObject)){
			return null;
		}
		
		String iri = ((OWLNamedObject) obj).getIRI().toString();
	
		String tag = annotationPropertyMap.get(iri);

		if (tag == null) {
			String prefix = Obo2OWLConstants.DEFAULT_IRI_PREFIX + "IAO_";
			if (iri.startsWith(prefix)) {
				tag = iri.substring(prefix.length());
				if(!OBOFormatConstants.TAGS.contains(tag))
					tag = null;
			}
			
			
		}
		return tag;
	}
	
	/*
	public static String propToTag(OWLAnnotationProperty prop) {
		String iri = prop.getIRI().toString();
		String tag = annotationPropertyMap.get(iri);

		if (tag == null) {
			String prefix = Obo2Owl.DEFAULT_IRI_PREFIX + "IAO_";
			if (iri.startsWith(prefix)) {
				tag = iri.substring(prefix.length());
			}
		}
		return tag;
	}*/

	private Frame getTermFrame(OWLEntity entity) {
		String id = getIdentifier(entity.getIRI());
		Frame f = this.obodoc.getTermFrame(id);

		if (f == null) {
			f = new Frame(FrameType.TERM);
			f.setId(id);
			add(f);
		}

		return f;
	}

	
	private void tr(OWLClassAssertionAxiom ax){
		OWLObject cls = ax.getClassExpression();
		
		if(!(cls instanceof OWLClass))
			return;
		
		String clsIRI = ((OWLClass) cls).getIRI().toString();
		
		
		if(Obo2Owl.IRI_CLASS_SYNONYMTYPEDEF.equals(clsIRI)){
			Frame f = this.obodoc.getHeaderFrame();
			Clause c = new Clause();
			c.setTag("synonymtypedef");

			OWLNamedIndividual indv =(OWLNamedIndividual) ax.getIndividual();
			String indvId = getIdentifier(indv);
			c.addValue(indvId);
			
			String nameValue = "";
			String scopeValue = null;
			for(OWLAnnotation ann: indv.getAnnotations(owlOntology)){
				String propId = ann.getProperty().getIRI().toString();
				String value = ((OWLLiteral) ann.getValue()).getLiteral();

				if(OWLRDFVocabulary.RDFS_LABEL.getIRI().toString().equals(propId)){
					nameValue = "\"" +value + "\"";
				}else
					scopeValue = value;
			}
			
				c.addValue(nameValue);
				
				if(scopeValue != null){
					c.addValue(scopeValue);
				}
			
			f.addClause(c);
		}else if(Obo2Owl.IRI_CLASS_SUBSETDEF.equals(clsIRI)){
			Frame f = this.obodoc.getHeaderFrame();
			Clause c = new Clause();
			c.setTag("subsetdef");

			OWLNamedIndividual indv =(OWLNamedIndividual) ax.getIndividual();
			String indvId = getIdentifier(indv);
			c.addValue(indvId);
			
			String nameValue = "";
			for(OWLAnnotation ann: indv.getAnnotations(owlOntology)){
				String propId = ann.getProperty().getIRI().toString();
				String value = ((OWLLiteral) ann.getValue()).getLiteral();

				if(OWLRDFVocabulary.RDFS_LABEL.getIRI().toString().equals(propId)){
					nameValue = "\"" +value + "\"";
				}
			}
			
				c.addValue(nameValue);
				
			f.addClause(c);
		}else{
			//TODO: individual
		}
			
	}
	
	private void tr(OWLSubClassOfAxiom ax) {
		OWLSubClassOfAxiom a = (OWLSubClassOfAxiom) ax;
		OWLClassExpression sub = a.getSubClass();
		OWLClassExpression sup = a.getSuperClass();
		if (sub instanceof OWLClass) {
			Frame f = getTermFrame((OWLEntity) sub);

			if (sup instanceof OWLClass) {
				Clause c = new Clause();
				c.setTag("is_a");
				c.setValue(getIdentifier(sup));
				f.addClause(c);
			} else if (sup instanceof OWLObjectSomeValuesFrom) {
				OWLObjectSomeValuesFrom r = (OWLObjectSomeValuesFrom) sup;

				Clause c = new Clause();
				c.setTag("relationship");
				c.addValue(getIdentifier(r.getProperty()));
				;
				c.addValue(getIdentifier(r.getFiller()));

				f.addClause(c);
			} else {
				LOG.warn("Cann't translate axiom: " + ax);
			}
		} else {
			LOG.warn("Cann't translate axiom: " + ax);
		}
	}



}