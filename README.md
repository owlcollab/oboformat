[![Build Status](https://travis-ci.org/owlcollab/oboformat.svg?branch=master)](https://travis-ci.org/owlcollab/oboformat)
[![DOI](https://zenodo.org/badge/13996/owlcollab/oboformat.svg)](https://zenodo.org/badge/latestdoi/13996/owlcollab/oboformat)

This project contains documentation for OBO Format

## Specification

  * obof1.4 spec
    * [current version](http://owlcollab.github.io/oboformat/doc/obo-syntax.html)
  * [obof1.4 guide](http://owlcollab.github.io/oboformat/doc/GO.format.obo-1_4.html)
  * [obof1.2 guide](http://owlcollab.github.io/oboformat/doc/GO.format.obo-1_2.html)

Note that the spec is currently hosted on
http://owlcollab.github.io/oboformat/doc/obo-syntax.html -- however,
you should be sure to bookmark the permanent URL, which is
http://purl.obolibrary.org/obo/oboformat/spec.html

Use this URL:

 * [http://owlcollab.github.io/oboformat/doc/obo-syntax.html](http://owlcollab.github.io/oboformat/doc/obo-syntax.html)

## Code

The code is now integrated into the [OWLAPI](https://github.com/owlcs/owlapi/), version 3.5.x and higher

## How to convert

We recommend the use of [ROBOT](https://github.com/ontodev/robot/) to convert back and forth

See [Conversion example](https://github.com/ontodev/robot/blob/master/examples/README.md#converting):

```
robot convert --input foo.owl --output foo.obo
```

[OWLTools Command Line Interface](https://github.com/owlcollab/owltools/) can also be used:

```
owltools foo.owl -o -f obo foo.obo
```

The use of owltools will be phased out for this purpose in favor of ROBOT

## See Also

 * [From OBO to OWL and back: Building Scalable Ontologies from OBO to OWL and back](http://www.slideshare.net/dosumis/from-obo-to-owl-and-back-building-scalable-ontologies)
 * [Macros](http://precedings.nature.com/documents/5292/version/2)

## Legacy documentation

 * https://github.com/owlcollab/oboformat/blob/wiki/ProjectHome.md

