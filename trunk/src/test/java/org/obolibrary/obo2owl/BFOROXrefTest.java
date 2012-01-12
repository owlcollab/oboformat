package org.obolibrary.obo2owl;

import static junit.framework.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class BFOROXrefTest extends OboFormatTestBasics {

	@Test
	public void testRelationXrefConversion() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		OWLOntology owlOnt = convertOBOFile("rel_xref_test.obo");

		// test initial conversion
		Set<OWLObjectProperty> ops =  owlOnt.getObjectPropertiesInSignature();
		for (OWLObjectProperty op : ops) {
			//System.out.println("OP:"+op);
		}
		assertTrue(ops.size() == 4);
		Set<OWLAnnotationAssertionAxiom> aaas = owlOnt.getAnnotationAssertionAxioms(IRI.create("http://purl.obolibrary.org/obo/BFO_0000051"));
		boolean ok = false;
		for (OWLAnnotationAssertionAxiom a : aaas) {
			System.out.println(a);
			if (a.getProperty().getIRI().toString().equals("http://www.geneontology.org/formats/oboInOwl#shorthand")) {
				OWLLiteral v = (OWLLiteral) a.getValue();
				if (v.getLiteral().equals("has_part")) {
					ok = true;
				}
			}
		}
		assertTrue(aaas.size() > 0);
		assertTrue(ok);
	
		aaas = owlOnt.getAnnotationAssertionAxioms(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050"));
		assertTrue(aaas.size() > 0);

		aaas = owlOnt.getAnnotationAssertionAxioms(IRI.create("http://purl.obolibrary.org/obo/RO_0002111"));
		assertTrue(aaas.size() > 0);

		aaas = owlOnt.getAnnotationAssertionAxioms(IRI.create("http://purl.obolibrary.org/obo/BAR_0000001"));
		assertTrue(aaas.size() > 0);

		Owl2Obo revbridge = new Owl2Obo();
		OBODoc d2 = revbridge.convert(owlOnt);
		
		Frame part_of = d2.getTypedefFrame("part_of");
		Collection<Clause> xrcs = part_of.getClauses(OboFormatTag.TAG_XREF);
		boolean okBfo = false;
		boolean okOboRel = false;
		
		for (Clause c : xrcs) {
			Xref value = c.getValue(Xref.class);
			if (value.getIdref().equals("BFO:0000050")) {
				okBfo = true;
			}
			if (value.getIdref().equals("OBO_REL:part_of")) {
				okOboRel = true;
			}
		}
		assertTrue(okBfo);
		assertTrue(okOboRel);
		
		Frame a = d2.getTermFrame("TEST:a");
		Clause rc = a.getClause(OboFormatTag.TAG_RELATIONSHIP);
		assertTrue(rc.getValue().equals("part_of"));
		assertTrue(rc.getValue2().equals("TEST:b"));

	}
	
	private OWLOntology convertOBOFile(String fn) throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		return convert(parseOBOFile(fn), fn);
	}
}
