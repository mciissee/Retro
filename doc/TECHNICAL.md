# Technical Documentation - Retro

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [Transformation Pipeline](#transformation-pipeline)
4. [Feature Implementation Pattern](#feature-implementation-pattern)
5. [Design Patterns](#design-patterns)
6. [Package Structure](#package-structure)
7. [Bytecode Transformation Details](#bytecode-transformation-details)
8. [Extension Guide](#extension-guide)

---

## Architecture Overview

Retro follows a **modular, plugin-based architecture** for bytecode transformation. The system is built around the **ASM framework** and uses the **Visitor pattern** extensively for bytecode analysis and modification.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Entry Points Layer                       │
│  ┌─────────────┐           ┌──────────────────────────┐   │
│  │   App.java  │           │  API (REST Endpoints)    │   │
│  │  (CLI Mode) │           │  - Quarkus Framework     │   │
│  └──────┬──────┘           └───────────┬──────────────┘   │
└─────────┼──────────────────────────────┼──────────────────┘
          │                               │
          └───────────────┬───────────────┘
                          │
┌─────────────────────────▼─────────────────────────────────┐
│                    Facade Layer                            │
│  ┌──────────────────────────────────────────────────┐    │
│  │             Retro.java (Facade)                   │    │
│  │  - Orchestrates transformation pipeline           │    │
│  │  - Manages feature visitors                       │    │
│  │  - Handles file I/O operations                    │    │
│  └──────────────────────────────────────────────────┘    │
└────────────────────────┬───────────────────────────────────┘
                         │
┌────────────────────────▼───────────────────────────────────┐
│                 Transformation Layer                        │
│  ┌──────────────────────────────────────────────────┐    │
│  │         ClassTransformer (ASM ClassVisitor)       │    │
│  │  - Coordinates feature visitors                   │    │
│  │  - Delegates to appropriate rewriters             │    │
│  └──────────────────────────────────────────────────┘    │
└────────────────────────┬───────────────────────────────────┘
                         │
┌────────────────────────▼───────────────────────────────────┐
│                  Feature Visitors Layer                     │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────┐   │
│  │  Lambda    │  │  Concat    │  │    NestMates     │   │
│  │  Visitor   │  │  Visitor   │  │     Visitor      │   │
│  └────────────┘  └────────────┘  └──────────────────┘   │
│  ┌────────────┐  ┌────────────┐                          │
│  │  Record    │  │   Mocker   │                          │
│  │  Visitor   │  │  Visitor   │                          │
│  └────────────┘  └────────────┘                          │
└────────────────────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Separation of Concerns**: Each feature has its own package with isolated detection, description, and rewriting logic
2. **Visitor Pattern**: Leverages ASM's visitor pattern for non-intrusive bytecode analysis
3. **Strategy Pattern**: Features are pluggable strategies that can be enabled/disabled
4. **Facade Pattern**: `Retro` class provides simplified interface to complex subsystem
5. **Immutability**: Core models (Options, ClassInfo, MethodInfo) are immutable

---

## Core Components

### 1. **App.java** - CLI Entry Point

**Responsibility**: Command-line interface entry point

```java
public class App {
    public static void main(String[] args) {
        // Parse command line arguments
        // Invoke Retro.exec()
    }
}
```

**Key Features**:
- Parses command-line arguments using CommandLineParser
- Validates input paths and options
- Handles help display and error reporting
- Delegates execution to Retro facade

### 2. **Retro.java** - System Facade

**Responsibility**: Main orchestration and facade for the transformation system

**Key Methods**:
```java
// Main execution entry points
public static boolean exec(Path path, Options options, Logger logger)
public static boolean exec(Path[] paths, Options options, Logger logger)
public static boolean exec(CommandLine commandLine)

// Feature management
public boolean hasFeature(Features feature)
public void visitFeatures(Consumer<FeatureVisitor[]> consumer)

// File operations
public void write(Path path, String className, byte[] bytes)
public void bytecode(Path path, String clazz, BiConsumer<Path, byte[]> consumer)

// Logging
public void logInfo(String... messages)
public void logWarning(String... messages)
public void logError(String... messages)
```

**Configuration**:
- ASM API Version: ASM7 (Opcodes.ASM7)
- Target JDK version (from Options)
- Feature set to transform
- Force mode flag

### 3. **ClassTransformer** - ASM ClassVisitor

**Responsibility**: Coordinates bytecode transformation by delegating to feature visitors

**Inheritance**: `extends ClassVisitor implements Opcodes`

**Workflow**:
1. Receives class file structure through ASM visitor callbacks
2. Wraps class visitor with feature-specific visitors (chain of responsibility)
3. Each feature visitor intercepts relevant bytecode instructions
4. Transformations are applied in pipeline fashion

```java
ClassVisitor chain = classWriter;
chain = new RecordRewriter(retro, classInfo, chain);
chain = new NestMateRewriter(retro, classInfo, chain);
chain = new LambdaRewriter(retro, classInfo, chain);
chain = new ConcatRewriter(retro, classInfo, chain);
```

### 4. **FileSystem** - File Management

**Responsibility**: Handles file system operations (reading/writing class files and JARs)

**Key Operations**:
- Recursively traverse directories
- Read and extract JAR files
- Write transformed bytecode to `retro-output/` directory
- Preserve directory structure
- Handle both individual `.class` files and `.jar` archives

---

## Transformation Pipeline

### Pipeline Flow

```
Input Files (.class, .jar)
         │
         ▼
┌─────────────────────┐
│  FileSystem.walk()  │  ← Discover all class files
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Read Bytecode      │  ← Load class file bytes
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  ClassReader        │  ← ASM: Parse bytecode
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ ClassTransformer    │  ← Create visitor chain
└──────────┬──────────┘
           │
           ▼
┌─────────────────────────────────────────┐
│      Feature Visitors (Chain)           │
│  ┌─────────────────────────────┐       │
│  │ 1. Detect Phase             │       │
│  │    - Scan for features      │       │
│  │    - Collect metadata       │       │
│  └─────────────────────────────┘       │
│  ┌─────────────────────────────┐       │
│  │ 2. Rewrite Phase            │       │
│  │    - Transform instructions │       │
│  │    - Modify bytecode        │       │
│  └─────────────────────────────┘       │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────┐
│   ClassWriter       │  ← ASM: Generate bytecode
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Write to Output    │  ← Save to retro-output/
└─────────────────────┘
```

### Step-by-Step Execution

#### Phase 1: Initialization
1. Parse command-line options → `Options` object
2. Initialize `Retro` facade with options and logger
3. Create feature visitor instances for each enabled feature

#### Phase 2: File Discovery
1. `FileSystem.walk()` recursively finds all `.class` and `.jar` files
2. For JARs: Extract entries and process each class individually
3. Build list of paths to process

#### Phase 3: Bytecode Analysis (Detect)
1. Create `ClassReader` with class file bytes
2. First pass: Detection visitors scan for features
3. Populate `FeatureDescriber` objects with detected occurrences
4. Log detected features if `-info` flag enabled

#### Phase 4: Bytecode Transformation (Rewrite)
1. Create visitor chain in reverse order (innermost to outermost)
2. Second pass: Rewriter visitors modify bytecode
3. Each rewriter intercepts specific instructions:
   - **LambdaRewriter**: Converts `invokedynamic` to anonymous inner classes
   - **ConcatRewriter**: Replaces `invokedynamic` concat with `StringBuilder`
   - **NestMateRewriter**: Generates bridge methods for nest access
   - **RecordRewriter**: Converts record syntax to regular class

#### Phase 5: Output
1. Write transformed bytecode to `retro-output/<original-path>/`
2. For JARs: Reconstruct JAR with transformed classes
3. Preserve original file structure and metadata

---

## Feature Implementation Pattern

Each feature follows a consistent 4-component pattern:

### Feature Package Structure

```
fr.umlv.retro.<feature>/
├── <Feature>Visitor.java      # Implements FeatureVisitor interface
├── <Feature>Detector.java     # Detects feature usage (extends MethodVisitor)
├── <Feature>Describer.java    # Describes detected occurrences
└── <Feature>Rewriter.java     # Transforms bytecode (extends ClassVisitor)
```

### Component Roles

#### 1. **FeatureVisitor Interface**
```java
public interface FeatureVisitor {
    MethodVisitor visitMethod(int access, String name, String descriptor,
                               String signature, String[] exceptions,
                               MethodInfo methodInfo, ClassInfo classInfo);
    
    FeatureDescriber describer();
}
```

**Purpose**: Contract for feature detection and description

#### 2. **Detector (extends MethodVisitor)**
```java
public class LambdaDetector extends MethodVisitor {
    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor,
                                        Handle bootstrapMethodHandle,
                                        Object... bootstrapMethodArguments) {
        // Detect lambda invokedynamic calls
        // Record occurrence in describer
    }
}
```

**Purpose**: 
- Intercepts relevant bytecode instructions during first pass
- Identifies feature usage patterns
- Collects metadata (locations, parameters, etc.)

#### 3. **Describer (implements FeatureDescriber)**
```java
public class LambdaDescriber implements FeatureDescriber {
    private final List<LambdaInfo> lambdas = new ArrayList<>();
    
    public void describe() {
        // Log detected lambda expressions
    }
}
```

**Purpose**:
- Stores detected occurrences
- Provides human-readable descriptions
- Supports logging and debugging

#### 4. **Rewriter (extends ClassVisitor)**
```java
public class LambdaRewriter extends ClassVisitor {
    @Override
    public MethodVisitor visitMethod(int access, String name, ...) {
        return new MethodVisitor(api, super.visitMethod(...)) {
            @Override
            public void visitInvokeDynamicInsn(...) {
                // Transform lambda to anonymous inner class
            }
        };
    }
}
```

**Purpose**:
- Performs actual bytecode transformation during second pass
- Replaces modern constructs with older equivalents
- Generates additional synthetic methods/classes if needed

### Example: Lambda Feature

```java
// 1. Visitor - Orchestrates detection and rewriting
public class LambdaVisitor implements FeatureVisitor {
    private final LambdaDescriber describer = new LambdaDescriber();
    
    public MethodVisitor visitMethod(...) {
        return new LambdaDetector(api, mv, describer, methodInfo, classInfo);
    }
    
    public FeatureDescriber describer() {
        return describer;
    }
}

// 2. Detector - Identifies lambda invokedynamic calls
public class LambdaDetector extends MethodVisitor {
    public void visitInvokeDynamicInsn(String name, String descriptor, ...) {
        if (isLambdaMetafactory(bootstrapMethodHandle)) {
            describer.addLambda(new LambdaInfo(...));
        }
        super.visitInvokeDynamicInsn(...);
    }
}

// 3. Describer - Stores and describes lambdas
public class LambdaDescriber implements FeatureDescriber {
    private List<LambdaInfo> lambdas = new ArrayList<>();
    
    public void describe() {
        for (LambdaInfo lambda : lambdas) {
            System.out.println("Lambda at " + lambda.location());
        }
    }
}

// 4. Rewriter - Transforms lambda to anonymous class
public class LambdaRewriter extends ClassVisitor {
    public MethodVisitor visitMethod(...) {
        return new MethodVisitor(api, mv) {
            public void visitInvokeDynamicInsn(...) {
                // Generate anonymous inner class
                // Replace invokedynamic with invokespecial
            }
        };
    }
}
```

---

## Design Patterns

### 1. Visitor Pattern (ASM Framework)

**Usage**: Core pattern for bytecode traversal and modification

```java
ClassReader → ClassVisitor → MethodVisitor → InstructionVisitor
```

**Benefits**:
- Non-intrusive bytecode analysis
- Separation of traversal and operation logic
- Easy to add new transformations

### 2. Strategy Pattern (Features)

**Usage**: Features are interchangeable strategies

```java
interface FeatureVisitor {
    MethodVisitor visitMethod(...);
    FeatureDescriber describer();
}
```

**Benefits**:
- Features can be enabled/disabled at runtime
- Easy to add new features
- Testable in isolation

### 3. Facade Pattern (Retro Class)

**Usage**: Simplifies complex transformation subsystem

```java
Retro.exec(path, options, logger); // Single entry point
```

**Benefits**:
- Hides internal complexity
- Provides unified interface
- Easier for clients to use

### 4. Chain of Responsibility (Visitor Chain)

**Usage**: Feature visitors form a processing chain

```java
ClassVisitor cv = classWriter;
cv = new RecordRewriter(retro, classInfo, cv);
cv = new LambdaRewriter(retro, classInfo, cv);
cv = new ConcatRewriter(retro, classInfo, cv);
```

**Benefits**:
- Each visitor handles specific instructions
- Flexible composition
- Order-dependent transformations possible

### 5. Factory Pattern (Mocker Package)

**Usage**: Generates mock implementations of unsupported APIs

```java
MockerVisitor generates synthetic classes for:
- java.util.function.Function
- java.util.function.Consumer
- etc.
```

**Benefits**:
- Runtime API compatibility
- Minimal dependencies in output

### 6. Immutable Objects (Models)

**Usage**: Core data models are immutable

```java
public class Options {
    private final int target;
    private final boolean force;
    private final EnumSet<Features> features;
    // No setters - immutable
}
```

**Benefits**:
- Thread-safe
- Predictable state
- Easy to reason about

---

## Package Structure

### Core Packages

#### `fr.umlv.retro` (Root)
- **Purpose**: Main facade and transformation orchestration
- **Key Classes**:
  - `App`: CLI entry point
  - `Retro`: System facade
  - `ClassTransformer`: ASM class visitor coordinator
  - `FileSystem`: File I/O operations

#### `fr.umlv.retro.models`
- **Purpose**: Core data models and abstractions
- **Key Components**:
  - `Features`: Enum of transformable features
  - `Options`: Configuration (target version, force mode, features)
  - `ClassInfo`: Metadata about class being transformed
  - `MethodInfo`: Metadata about method being analyzed
  - `FeatureVisitor`: Interface for feature detection/transformation
  - `FeatureDescriber`: Interface for describing detected features
  - `Logger`: Logging facade

#### `fr.umlv.retro.utils`
- **Purpose**: Utility classes and helpers
- **Key Classes**:
  - `VersionUtils`: JDK version ↔ bytecode version mapping
  - `TypeUtils`: ASM Type manipulation utilities
  - `InstUtils`: Bytecode instruction generation helpers
  - `Contracts`: Precondition checking utilities

#### `fr.umlv.retro.cli`
- **Purpose**: Command-line interface
- **Key Classes**:
  - `CommandLineParser`: Parses CLI arguments
  - `CommandLine`: Represents parsed command
  - `CommandLineOption`: Individual option metadata

#### `fr.umlv.retro.api`
- **Purpose**: REST API endpoints (Quarkus)
- **Key Classes**:
  - `Api`: Main REST resource
  - `EnvService`: Environment management
  - `Capability`: Feature capability detection

### Feature Packages

#### `fr.umlv.retro.lambdas`
- **Purpose**: Transform lambda expressions (Java 8+)
- **Transformation**: `invokedynamic` → anonymous inner classes
- **Components**: LambdaVisitor, LambdaDetector, LambdaDescriber, LambdaRewriter

#### `fr.umlv.retro.concats`
- **Purpose**: Transform string concatenation (Java 9+)
- **Transformation**: `invokedynamic` → `StringBuilder` calls
- **Components**: ConcatVisitor, ConcatDetector, ConcatDescriber, ConcatRewriter

#### `fr.umlv.retro.nestmates`
- **Purpose**: Transform nest-based access control (Java 11+)
- **Transformation**: Nest access → bridge methods
- **Components**: NestMateVisitor, NestMateDetector, NestMateHostDescriber, NestMateMemberDescriber, NestMateRewriter

#### `fr.umlv.retro.records`
- **Purpose**: Transform record classes (Java 14+)
- **Transformation**: Record → regular class with accessors, equals, hashCode, toString
- **Components**: RecordVisitor, RecordDetector, RecordDescriber, RecordRewriter

#### `fr.umlv.retro.mocker`
- **Purpose**: Generate mock implementations for unsupported APIs
- **Use Case**: Provide minimal implementations of java.util.function interfaces
- **Components**: MockerVisitor, MockerMethodDetector

---

## Bytecode Transformation Details

### Java 8: Lambda Expressions

**Original (Java 8+)**:
```java
Function<String, Integer> f = s -> s.length();
```

**Bytecode (Java 8)**:
```
INVOKEDYNAMIC apply()Ljava/util/function/Function; [
  // Bootstrap method: LambdaMetafactory.metafactory
  // Implementation method: lambda$0(Ljava/lang/String;)Ljava/lang/Integer;
]
```

**Transformed (Java 7)**:
```java
// Generated anonymous inner class
class $Lambda$1 implements Function<String, Integer> {
    public Integer apply(String s) {
        return s.length();
    }
}
Function<String, Integer> f = new $Lambda$1();
```

**Implementation Details**:
- Detect `invokedynamic` with LambdaMetafactory bootstrap
- Extract target method reference from bootstrap arguments
- Generate synthetic inner class implementing functional interface
- Replace `invokedynamic` with `new` + constructor call

### Java 9: String Concatenation

**Original (Java 9+)**:
```java
String result = "Hello " + name + "!";
```

**Bytecode (Java 9)**:
```
INVOKEDYNAMIC makeConcatWithConstants(Ljava/lang/String;)Ljava/lang/String; [
  // Bootstrap method: StringConcatFactory.makeConcatWithConstants
  // Constant: "Hello \u0001!"
]
```

**Transformed (Java 8)**:
```java
StringBuilder sb = new StringBuilder();
sb.append("Hello ");
sb.append(name);
sb.append("!");
String result = sb.toString();
```

**Implementation Details**:
- Detect `invokedynamic` with StringConcatFactory bootstrap
- Parse constant recipe string (contains `\u0001` placeholders)
- Generate explicit `StringBuilder` operations
- Replace `invokedynamic` with builder pattern

### Java 11: Nest-Based Access Control

**Original (Java 11+)**:
```java
class Outer {
    private int value;
    
    class Inner {
        void access() {
            System.out.println(value); // Direct access via nest
        }
    }
}
```

**Bytecode (Java 11)**:
```
GETFIELD Outer.value:I  // Direct access, no bridge
NestHost: Outer
NestMembers: Outer$Inner
```

**Transformed (Java 10)**:
```java
class Outer {
    private int value;
    
    // Generated bridge method
    static int access$000(Outer outer) {
        return outer.value;
    }
    
    class Inner {
        void access() {
            System.out.println(Outer.access$000(Outer.this));
        }
    }
}
```

**Implementation Details**:
- Detect nest membership attributes
- Generate synthetic bridge methods for private field/method access
- Replace direct access with bridge method calls
- Remove nest attributes from class file

### Java 14: Record Classes

**Original (Java 14+)**:
```java
record Point(int x, int y) {}
```

**Bytecode (Java 14)**:
```
Record attribute:
  Component: x:I
  Component: y:I
```

**Transformed (Java 13)**:
```java
final class Point {
    private final int x;
    private final int y;
    
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int x() { return x; }
    public int y() { return y; }
    
    public boolean equals(Object o) { /* generated */ }
    public int hashCode() { /* generated */ }
    public String toString() { /* generated */ }
}
```

**Implementation Details**:
- Detect Record attribute in class file
- Generate explicit fields for components
- Generate canonical constructor
- Generate accessor methods
- Generate `equals()`, `hashCode()`, `toString()` implementations
- Remove Record attribute

---

## Extension Guide

### Adding a New Feature

To add a new bytecode feature transformation:

#### Step 1: Define Feature Enum
```java
// In Features.java
public enum Features {
    Lambda, Concat, NestMates, Record,
    NewFeature // Add your feature
}
```

#### Step 2: Create Feature Package
```
src/main/java/fr/umlv/retro/newfeature/
├── NewFeatureVisitor.java
├── NewFeatureDetector.java
├── NewFeatureDescriber.java
└── NewFeatureRewriter.java
```

#### Step 3: Implement FeatureVisitor
```java
package fr.umlv.retro.newfeature;

public class NewFeatureVisitor implements FeatureVisitor {
    private final NewFeatureDescriber describer = new NewFeatureDescriber();
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                      String signature, String[] exceptions,
                                      MethodInfo methodInfo, ClassInfo classInfo) {
        return new NewFeatureDetector(api, methodVisitor, describer, 
                                       methodInfo, classInfo);
    }
    
    @Override
    public FeatureDescriber describer() {
        return describer;
    }
}
```

#### Step 4: Implement Detector
```java
public class NewFeatureDetector extends MethodVisitor {
    private final NewFeatureDescriber describer;
    
    @Override
    public void visitXXXInsn(...) {
        // Detect your feature's bytecode pattern
        if (isNewFeature()) {
            describer.addOccurrence(...);
        }
        super.visitXXXInsn(...);
    }
}
```

#### Step 5: Implement Describer
```java
public class NewFeatureDescriber implements FeatureDescriber {
    private final List<NewFeatureInfo> occurrences = new ArrayList<>();
    
    public void addOccurrence(NewFeatureInfo info) {
        occurrences.add(info);
    }
    
    @Override
    public void describe() {
        // Log detected occurrences
    }
}
```

#### Step 6: Implement Rewriter
```java
public class NewFeatureRewriter extends ClassVisitor {
    public NewFeatureRewriter(Retro retro, ClassInfo classInfo, ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
        // ...
    }
    
    @Override
    public MethodVisitor visitMethod(...) {
        return new MethodVisitor(api, super.visitMethod(...)) {
            @Override
            public void visitXXXInsn(...) {
                // Transform bytecode
                // Generate alternative instructions
            }
        };
    }
}
```

#### Step 7: Register in ClassTransformer
```java
// In ClassTransformer.java
ClassVisitor cv = classWriter;
cv = new NewFeatureRewriter(retro, classInfo, cv);
cv = new RecordRewriter(retro, classInfo, cv);
// ... existing chain
```

#### Step 8: Update Documentation
- Add feature to README.md features table
- Document transformation in TECHNICAL.md
- Add examples to examples/ directory

### Testing New Features

1. **Create test case**: Add example Java file to `examples/`
2. **Compile with modern JDK**: Use `scripts/compile.sh`
3. **Run transformation**: `java -jar target/retro.jar -target 8 -features NewFeature examples/javac/YourTest.class`
4. **Verify output**: Check `examples/javac/retro-output/` for transformed bytecode
5. **Validate**: Use `javap -v` to inspect generated bytecode

---

## Performance Considerations

### Optimization Strategies

1. **Single-Pass Detection**: Features are detected in one pass through bytecode
2. **Lazy Rewriting**: Only rewrite classes that contain detected features
3. **Visitor Chain**: Efficient pipeline avoids multiple class file reads
4. **Streaming JAR Processing**: Process JAR entries without full extraction

### Memory Management

- **ClassInfo/MethodInfo**: Short-lived objects, created per-class
- **Feature Describers**: Accumulate data only during detection phase
- **Bytecode Buffers**: Released immediately after writing

### Scalability

- **Large Codebases**: Process files individually, not all in memory
- **Multi-threading**: Not currently implemented (could be added)
- **Incremental Processing**: Supports processing subsets of files

---

## Dependencies

### Core Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| **ASM** | 9.6 | Bytecode manipulation framework |
| **Quarkus** | 1.2.0.Final | Web framework for REST API |
| **Commons IO** | 2.6 | File utilities |

### ASM Modules Used

- `asm.jar`: Core bytecode reading/writing
- `asm-tree.jar`: Tree API for complex transformations
- `asm-util.jar`: ASMifier, Textifier utilities
- `asm-commons.jar`: Common transformation utilities

---

## Testing Strategy

### Test Approach

1. **Example-Based Testing**: Use `examples/` directory as test corpus
2. **Bytecode Verification**: Compare transformed bytecode against expected output
3. **Runtime Testing**: Execute transformed classes to verify behavior
4. **Regression Testing**: Ensure transformations don't break existing code

### Test Files Structure

```
examples/
├── TestLambda.java         # Lambda expression examples
├── TestConcat.java         # String concatenation examples
├── TestNestMate.java       # Nested class access examples
├── TestRecord.java         # Record class examples
└── TestTryWithResource.java # Try-with-resources examples
```

### Validation Process

1. Compile examples with target JDK
2. Generate bytecode representations (javap, ASMifier, Textifier)
3. Run Retro transformation
4. Compare output with expected transformations
5. Verify runtime behavior

---

## Troubleshooting

### Common Issues

#### 1. UnsupportedClassVersionError
**Cause**: ASM library version doesn't support target bytecode version  
**Solution**: Upgrade ASM to version supporting your Java version

#### 2. VerifyError after Transformation
**Cause**: Invalid bytecode generated by rewriter  
**Solution**: Use `javap -v` to inspect bytecode, check stack map frames

#### 3. Missing Mock Classes
**Cause**: Mocker visitor not generating required API mocks  
**Solution**: Ensure mocker visitor is in transformation chain

#### 4. Nest Access Errors
**Cause**: Bridge methods not generated correctly  
**Solution**: Verify NestMateRewriter is creating synthetic methods

---

## Future Enhancements

### Potential Improvements

1. **Multi-threading**: Parallel processing of independent class files
2. **Incremental Transformation**: Only transform changed classes
3. **Caching**: Cache transformation results for identical inputs
4. **Additional Features**: 
   - Pattern matching (Java 16+)
   - Sealed classes (Java 17+)
   - Virtual threads (Java 21+)
5. **IDE Integration**: Eclipse/IntelliJ plugins
6. **Build Tool Plugins**: Maven/Gradle plugins
7. **Validation Framework**: Automated correctness checking

---

## References

### ASM Framework
- **Documentation**: https://asm.ow2.io/
- **API Javadoc**: https://asm.ow2.io/javadoc/
- **User Guide**: https://asm.ow2.io/asm4-guide.pdf

### Java Language Specification
- **Lambda Expressions**: JLS §15.27
- **String Concatenation**: JLS §15.18.1
- **Nest-Based Access**: JVMS §5.4.4
- **Record Classes**: JLS §8.10

### Bytecode Specification
- **JVMS**: https://docs.oracle.com/javase/specs/jvms/se17/html/
- **invokedynamic**: JVMS §6.5.invokedynamic
- **Bootstrap Methods**: JVMS §4.7.23

---

## Glossary

| Term | Definition |
|------|------------|
| **ASM** | Java bytecode manipulation and analysis framework |
| **Visitor Pattern** | Design pattern for traversing object structures |
| **ClassVisitor** | ASM interface for visiting class file elements |
| **MethodVisitor** | ASM interface for visiting method bytecode |
| **Bytecode** | Low-level instructions executed by JVM |
| **invokedynamic** | Dynamic method invocation instruction (Java 7+) |
| **Bootstrap Method** | Method invoked to resolve invokedynamic call site |
| **Bridge Method** | Synthetic method for compatibility/access |
| **Nest Host** | Top-level class in nest-based access hierarchy |
| **Descriptor** | String encoding of method/field type |
| **Opcode** | Bytecode instruction operation code |

---

**Last Updated**: December 16, 2025  
**Version**: 1.0.0  
**Maintainer**: Mamadou Cisse