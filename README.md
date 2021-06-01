# JDart #
JDart is a tool for performing *concolic execution* on a Java program. It is written as an extension to NASA Java Pathfinder (JPF).  The aim
of concolic execution is to explore additional behavior in the program by generating
input values which will result in a different path being taken through a program
(or method).

To cite JDart, please use the most recent paper that was accepted at TACAS 2016:

* Kasper Luckow, Marko Dimjasevic, Dimitra Giannakopoulou, Falk Howar, Malte Isberner, Temesghen Kahsai, Zvonimir Rakamaric, Vishwanath Raman, **JDart: A Dynamic Symbolic Analysis Framework**, 22nd International Conference on Tools and Algorithms for the Construction and Analysis of Systems (TACAS 2016), \[[pdf](http://soarlab.org/publications/tacas2016-ldghikrr.pdf)\] \[[bibtex](http://soarlab.org/publications/tacas2016-ldghikrr.bib)\].

If you want to repeat experiments reported in the paper, use a
[reproducible research environment in Aptlab][4].

## Installation ##

JDart currently works as a shell plug-in to [JPF-core](https://github.com/javapathfinder/jpf-core).
In addition it depends on the [jConstraints library](https://github.com/tudo-aqua/jconstraints).

We invested into the simplification of the build process. At the moment, dependencies
are loaded from jitpack.io.
It should be sufficient to run:
`./gradlew jar exampleClasses`
to build the requried jars.

Next, in the current state of the Gradle build, it is required to adapt the jpf.properties file.
Make sure the following part:
```
jpf-jdart.classpath=\
  ${jpf-jdart}/build/libs/jdart-classes-0.1.0-ecb5455.jar;\
  ${jpf-jdart}/build/libs/jdart-annotations-0.1.0-ecb5455.jar;\
  ${jpf-jdart}/build/classes/java/examples
```

matches the version currently produced in the build folder. The autmation to fix it is still missing.

## Using JDart
The analysis configuration is specified in a jpf application properties file. The minimum configuration required is:
```
@using = jpf-jdart

# Specify the analysis shell. JDart includes a couple of those in addition to the standard JDart shell, e.g., the MethodSummarizer
shell=gov.nasa.jpf.jdart.JDart

# Specify the constraint solver. Can be any of the jConstraints solver plugins
symbolic.dp=z3

# Provide the fully qualified class name of the entry point of the SUT
target=features.simple.Input

# Set up the concolic method with symbolic/concrete parameters. See the wiki for more details
concolic.method.bar=features.simple.Input.bar(d:double)

# Specify the concolic method configuration object to use
concolic.method=bar

```

For an example of how to configure JDart, please have a look at the `test_xxx.jpf` files
in `src/examples/features/simple`. JDart can be run on these examples using the `jpf` binary in jpf-core:
```bash
$ java -jar build/libs/RunJdart*.jar src/examples/features/simple/test_foo.jpf
```

The documentation for the concolic execution configuration can be found in the wiki.


[0]: http://babelfish.arc.nasa.gov/trac/jpf/wiki/projects/jpf-core
[1]: https://github.com/psycopaths/jConstraints
[3]: https://github.com/psycopaths
[4]: https://www.aptlab.net/p/CAVA/jdart-tacas-2016-v4
[5]: https://github.com/psycopaths/jConstraints-z3
[6]: https://www.vagrantup.com/
[7]: https://www.virtualbox.org/
[8]: https://libvirt.org/
[9]: https://www.docker.com/
