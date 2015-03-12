A java library for converting obo format documents to OWL, and for converting (a subset of) OWL to obo format. This project is also the current home of the macro expansion library [described here](http://precedings.nature.com/documents/5292/version/1).

See the [README](http://code.google.com/p/oboformat/source/browse/trunk/README)

See also [From OBO to OWL and back: Building Scalable Ontologies from OBO to OWL and back](http://www.slideshare.net/dosumis/from-obo-to-owl-and-back-building-scalable-ontologies)

# OBO Format Specification #

  * obof1.4 spec
    * [current version](http://oboformat.googlecode.com/svn/trunk/doc/obo-syntax.html)
  * [obof1.4 guide](http://oboformat.googlecode.com/svn/trunk/doc/GO.format.obo-1_4.html)
  * [obof1.2 guide](http://oboformat.googlecode.com/svn/trunk/doc/GO.format.obo-1_2.html)

# Instructions for Java Programmers #

You can use oboformat.jar within your applications.

  * See [The API Guide](APIGuide.md)
  * See [Example Code](http://oboformat.googlecode.com/svn/trunk/src/main/java/org/obolibrary/examples/ExportLabelsAsOWL.java)
  * Consult the javadocs for:
    * [org.obolibrary.obo2owl.Obo2Owl](http://oboformat.googlecode.com/svn/trunk/src/main/java/org/obolibrary/obo2owl/Obo2Owl.java)
    * [org.obolibrary.obo2owl.Owl2Obo](http://oboformat.googlecode.com/svn/trunk/src/main/java/org/obolibrary/obo2owl/Owl2Obo.java)
    * [MacroExpansionVisitor](http://oboformat.googlecode.com/svn/trunk/src/main/java/org/obolibrary/macro/MacroExpansionVisitor.java)

See also [OWLTools](http://owltools.googlecode.com), which provides "OBO-format friendly" methods on top of the OWL API, and transparent access to the converter.

# Command Line Usage #

A simple command line interface is available. First obtain the code and set up your PATH to point to the oboformat directory. For example:

```
cd
svn checkout http://oboformat.googlecode.com/svn/trunk/ oboformat-read-only
export PATH="$PATH:$HOME/oboformat-read-only"
```

Type the following for instructions:

```
obolib-obo2owl -h
```

Converting from obo to owl:

```
obolib-obo2owl myont.obo -o myont.owl
```

The -o argument can be ommitted, in which case it will save a file **myont.obo.owl**

Converting from owl2obo:

```
obolib-owl2obo myont.owl -o myont.obo
```

# Graphical User Interface #

A graphical user interface is available via the [Oort](http://code.google.com/p/owltools/wiki/OortToc). Oort provides obo2owl and owl2obo capabilities via this codebase. It also provides simple access to more powerful capabilities.

See [Graphical User Interface Documentation](http://code.google.com/p/owltools/wiki/OBOReleaseManagerGUIDocumentation)

## Installation Instructions ##

  1. Download the Oort installer for your platform from [the OWLTools download site](http://code.google.com/p/owltools/downloads/list)
  1. Double click on the installer and follow the instructions
  1. You should now have an application called "obo-owl-converter". This provides a GUI for bidirectional conversion

Note that you should also have the full Oort application as well. This extends the capabilities of the basic converter. See the [documentation](http://code.google.com/p/owltools/wiki/OBOReleaseManagerGUIDocumentation)

# Mail List #

The mailing list "obo-format" is for users and those involved in the specification and development of the code. It is hosted on sourceforge and mirrored on google. You can subscribe to / post from either:

  * [subscribe via sourceforge](https://lists.sourceforge.net/lists/listinfo/obo-format)
  * [subscribe via google groups](https://groups.google.com/group/obo-format)

For oboformat developers, the https://groups.google.com/group/oboformat-issue-tracker list contains notifications from the [issue tracker](http://code.google.com/p/oboformat/issues/list)