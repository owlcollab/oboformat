package org.obolibrary.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyFormat;

public class OBORunnerConfiguration {

	private final List<Variable<?>> registeredVariables = new ArrayList<Variable<?>>();

	public final Variable<String> outFile = new StringParamterVariable("Output File", null, "-o", "--out");
	public final Variable<String> buildDir = new StringParamterVariable("Build Directory", null, "-b", "--build");
	public final Variable<String> defaultOnt = new StringParamterVariable("Default Ontology", null,
			"--default-ontology");
	public final Variable<Boolean> isOboToOwl = new ParamterVariable<Boolean>("Set owl to obo conversion direction",
			true, "--owl2obo") {

		@Override
		public boolean setValue(String value) {
			this.value = false;
			return true;
		}
	};
	public final Variable<Boolean> isExpandMacros = new ParamterVariable<Boolean>("Expand OWL Macros",
			false, "-x","--expand-macros") {

		@Override
		public boolean setValue(String value) {
			this.value = true;
			return true;
		}
	};
	public final Variable<Boolean> allowDangling = new ParamterVariable<Boolean>("Allow Dangling", false,
			"--allowdangling", "--allow-dangling") {

		@Override
		public boolean setValue(String value) {
			this.value = true;
			return true;
		}
	};
	
	protected final Variable<Boolean> showHelp = new ParamterVariable<Boolean>("show help",false,"-h","--help"){

		@Override
		public boolean setValue(String value) {
			this.value = true;
			return true;
		}
	};
	
	public final Variable<String> outputdir = new StringParamterVariable("Output Directory", ".", "--outdir");
	public final Variable<String> version = new StringParamterVariable("OWL version", null, "--owlversion");

	public final Variable<OWLOntologyFormat> format = new ParamterVariable<OWLOntologyFormat>("OWL ontology format",
			new OWLXMLOntologyFormat(), "-t", "--to") {

		private String failureMessage = null;

		@Override
		public boolean setValue(String to) {
			if (to.equals("owlxml")) {
				this.value = new OWLXMLOntologyFormat();
			} else if (to.contains("manchester")) {
				this.value = new ManchesterOWLSyntaxOntologyFormat();
			} else if (to.contains("RDF")) {
				this.value = new RDFXMLOntologyFormat();
			} else {
				failureMessage = "don't know format '" + to + "' -- reverting to default: " + value;
				return false;
			}
			return true;
		}

		@Override
		public String getSetValueFailure() {
			return failureMessage;
		}
	};

	public final Variable<Set<String>> ontsToDownload = new StringSetParamterVariable("Ontologies to download",
			"--download");
	public final Variable<Set<String>> omitOntsToDownload = new StringSetParamterVariable(
			"Ontologies to be ommited for download", "--omit-download");

	/*
	 * Always put this as last into the list. This is required to keep the
	 * semantics, that any unknown string is treated as a path
	 */
	public final Variable<Collection<String>> paths = new Variable<Collection<String>>("paths", new Vector<String>()) {

		@Override
		public boolean setValue(String value) {
			this.value.add(value);
			return true;
		}
	};

	public Iterable<Variable<?>> getVariables() {
		return Collections.unmodifiableList(registeredVariables);
	}

	public abstract class Variable<T> {
		private final String name;
		protected T value;

		protected Variable(String name, T defaultValue) {
			this.name = name;
			this.value = defaultValue;
			registeredVariables.add(this);
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return null;
		}

		public Iterable<String> getParameters() {
			return null;
		}

		public abstract boolean setValue(String value);
		
		public void setRealValue(T value) {
			this.value = value;
		}

		public String getSetValueFailure() {
			return null;
		}

		public T getValue() {
			return value;
		}

		public boolean doesReadValue() {
			return false;
		}
		
		public boolean isEmpty() {
			return value == null;
		}

		@Override
		public String toString() {
			return getName()+":"+getValue();
		}
	}

	private abstract class ParamterVariable<T> extends Variable<T> {

		private final String[] parameters;

		protected ParamterVariable(String name, T defaultValue, String... parameters) {
			super(name, defaultValue);
			this.parameters = parameters;
		}

		@Override
		public Iterable<String> getParameters() {
			return Arrays.asList(parameters);
		}
	}

	private class StringParamterVariable extends ParamterVariable<String> {

		protected StringParamterVariable(String name, String defaultValue, String... parameters) {
			super(name, defaultValue, parameters);
		}

		@Override
		public boolean setValue(String value) {
			this.value = value;
			return true;
		}

		@Override
		public boolean doesReadValue() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			return value == null || value.length() == 0;
		}
	}

	private final class StringSetParamterVariable extends ParamterVariable<Set<String>> {
		private StringSetParamterVariable(String name, String...parameters) {
			super(name, new HashSet<String>(), parameters);
		}

		@Override
		public boolean setValue(String value) {
			this.value.add(value);
			return true;
		}

		@Override
		public boolean isEmpty() {
			return value.isEmpty();
		}
	}
}