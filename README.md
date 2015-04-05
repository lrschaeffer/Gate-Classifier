# Gate-Classifier
A program to classify reversible transformations based on their truth tables.

## Compilation
The source code is under `src/Classifier.java`. Compile it with any Java compiler, and run the program with the command `java Classifier` (with Classifier.class in the classpath or somewhere Java can see it).

## Input Format
The program will read transformations from standard input (until it reaches the end of file) in the following format. A gate is represented as a series of input/output pairs. For example,
```
0 -> 1
1 -> 0
```
is the representation of a NOT gate. The first bit string in the first line determines the number of input bits in the transformation, and all other bit strings must have the same length. The program ignores all lines that do not contain two bit strings, separated by (and possibly surrounded by) characters other than 0 and 1. So
```
0 -> 1
input 0 goes to output 1
021
```
are all equivalent, and lines like
```
NOT
gate 1
```
are ignored completely. When the program finishes one gate, it begins reading the next, until it reaches the end of file. The file `basic_tests.txt` contains many examples.

## Limitations and Performance

The program is designed to run in time linear in the size of the input, so we may consider quite large gates. It has been tested on gates of up to 24 bits. On the other hand, it stores the entire truthtable as an array of integers, so it will likely fail while allocating the multi-gigabyte array necessary to store a gate of more than about 28 bits.

