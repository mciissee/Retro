rm -r asmifiers 2> /dev/null
rm -r asmtextifiers 2> /dev/null
rm -r javac 2> /dev/null
mkdir asmifiers
mkdir asmtextifiers
mkdir javac

#/Library/Java/JavaVirtualMachines/jdk-14.jdk/Contents/Home/bin/javac -d javac --release 14 --enable-preview *.java
javac -d javac *.java

cd javac
for f in *.class;
do
    java -cp .:../asm.jar:../asm-util.jar org.objectweb.asm.util.ASMifier $f > "../asmifiers/$f.txt"
    java -cp .:../asm.jar:../asm-util.jar org.objectweb.asm.util.Textifier $f > "../asmtextifiers/$f.txt"
    javap -c -p $f > "./$f.txt"
done
jar cf TEST-ALL.jar *.class
cd ../