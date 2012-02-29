package org.obolibrary.obo2owl;

import static junit.framework.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class CHEBITest extends OboFormatTestBasics {

	@Test
	public void testConvertXPs() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		OWLOntology owlOnt = 
			convertOBOFile("ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/chebi.obo");
		assertNotNull(owlOnt);
	}
	
	private OWLOntology convertOBOFile(String fn) throws IOException, OWLOntologyCreationException, OWLOntologyStorageException {
		return convert(parseOBOURL(fn), "chebi");
	}

}