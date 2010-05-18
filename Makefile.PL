use strict;
use warnings;

mkdir 'bin' unless -d 'bin';

system qw{javac -d bin -cp src -extdirs lib src/dareka/Nadeko.java} and die "make error: $?";
system qw{jar cfm Nadeko.jar MANIFEST -C bin .} and die "jar create failed: $?";
system qw{jar uf Nadeko.jar -C src .} and die "jar create failed: $?";

print "created Nadeko.jar\n";
