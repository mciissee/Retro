javac *.java
rm -r asmifiers
rm -r asmtextifiers
rm -r javac
mkdir asmifiers
mkdir asmtextifiers
mkdir javac
for f in *.class;
do
    java -cp .:asm.jar:asm-util.jar org.objectweb.asm.util.ASMifier $f > "./asmifiers/$f.txt"
    java -cp .:asm.jar:asm-util.jar org.objectweb.asm.util.Textifier $f > "./asmtextifiers/$f.txt"
    javap -c -p $f > "./javac/$f.txt"
done
