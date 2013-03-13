jp2view
=======

first version, tested on [openjpeg2.0](http://code.google.com/p/openjpeg/downloads/detail?name=openjpeg-2.0.0.tar.gz&can=2&q=) with openjdk1.7

Build and install openjpeg
-----

1. cd /your/path/to/openjpeg2.0

2. sudo apt get install cmake make

3. cmake .

4. make

5. sudo make install

6. sudo ldconfig (alternatively set LD_LIBRARY_PATH to location of libopenjp2.so)

Build this project
----

(prerequisites: make and maven2+)

1. cd /your/path/to/jp2view

2. export OPJ_INC=/your/includepath/openjpeg2.0 (i.e.: /usr/local/include/openjpeg-2.0/)

3. export JAVA_HOME=/your/jdkpath (i.e.: /usr/lib/jvm/java-1.7.0-openjdk-amd64)

4. make

5. cd target

6. java -jar jp2-0.1.0-jar-with-dependencies.jar nl.kb.JP2Reader ../res/balloon.jp2 4

If all went well, there are now 5 jpeg files in your target directory:
- test.jpg file (full res balloon)
- test_region.jpg
- test_region_scaled_up.jpg
- test_region_scaled_down.jpg
- test_full_scaled.jpg
