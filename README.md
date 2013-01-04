Leiningen dependency (Clojars):

`[typed "0.1.5"]`

# Typed Clojure

Gradual typing in Clojure, as a library.

# Rationale

Static typing has well known benefits. For example, statically typed languages catch many common 
programming errors at the earliest time possible: compile time.
Types also serve as an excellent form of (machine checkable) documentation that
almost always augment existing hand-written documentation.

Languages without static type checking (dynamically typed) bring other benefits.
Without the strict rigidity of mandatory static typing, they can provide more flexible and forgiving
idioms that can help in rapid prototyping.
Often the benefits of static type checking are desired as the program grows.

This work adds static type checking (and some of its benefits) to Clojure, a dynamically typed language, 
while still preserving idioms that characterise the language.
It allows static and dynamically typed code to be mixed so the programmer can use whichever
is more appropriate.

(For a detailed treatment, see my Honours Dissertation, [A Practical Optional Type System for Clojure](https://github.com/downloads/frenchy64/papers/ambrose-honours.pdf))

# Screencasts

* [ep 1. Porting clojure.set](https://vimeo.com/55196903)
* [ep. 2 - Why conj is weird, annotating fns, polymorphic type variable scope](https://vimeo.com/55215849)
* [ep 3 - More wrestling](https://vimeo.com/55251041)
* [Java interop (treatment of nil/null)](https://vimeo.com/55280915)

# License

Typed Clojure is released under the same license as Clojure: Eclipse Public License v 1.0.

See `LICENSE`.

# Changelog

0.1.6-SNAPSHOT
- Ensure `Result` is not introduced when performing type inference on drest fn apps

0.1.5
- Better errors for Java methods and polymorphic function applications, borrow error messages from Typed Racket
- Change `ann-datatype`, `ann-protocol`, `ann-pprotocol` syntax to be flatter
  (ann-protocol pname
                method-name method-type ...)
  (ann-dataype dname
               [field-name :- field-type ...])
- Add `defprotocol>`

0.1.4
- Support Clojure 1.4.0+
- Better errors, print macro-expanded form from AST

0.1.3
  - Refactor typed.core into individual files
  - Add `method-type`
    - `(method-type 'java.io.File/getName)` prints the current Typed Clojure type for the getName method of File
  - Add types for some clojure.core coersion functions
  - Preliminary support for ClojureScript

0.1.2
  - Fix objects and filters being lost during polymorphic and dotted function applications
    - Add tests for (if (seq a) (first a) 0) filter example.
  - Can annotate datatypes outside current namespace
  - Improve type of `seq`, `next`, `conj`
  - tc-pr-env -> print-env
  - tc-pr-filters -> print-filterset
  - Alter APersistentMap
  - Check that local binding occurrences match with expected types
  - Heterogeneous maps are APersistentMap's instead of IPersistentMap's
  - Heterogeneous vectors are APersistentVector's instead of IPersistentVector's

0.1.1

- Ensure `ann-form` finally checks its expression is of the expected type
- Improve simplifying of intersections involving Java classes

# Quickstart

`(typed.core/ann v t)` gives var `v` the static type `t`.

`(typed.core/ann-form f t)` ensures form `f` is of the static type `t`.

`(typed.core/check-ns)` type checks the current namespace.

`(typed.core/cf t)` type checks the form `t`.

# Examples

(These don't completely type check yet)

* [typed.test.rbt](https://github.com/frenchy64/typed-clojure/blob/master/test/typed/test/rbt.clj) for examples of mutually recursive types and heterogenous maps
* [typed.test.core-logic](https://github.com/frenchy64/typed-clojure/blob/master/test/typed/test/core_logic.clj) for examples of typing (tightly coupled) datatypes and protocols
* [typed.test.example](https://github.com/frenchy64/typed-clojure/blob/master/test/typed/test/example.clj) for a few little examples of simple usage

# Contribution Guide

Contributors must complete a Clojure CA.

Those interested in hacking with Typed Clojure should watch the first three screencasts.
They encounter some common problems when hacking Typed Clojure and demonstrate some solutions.

Currently, the main namespace `typed.core` is split over many files, all
loaded by `typed/core.clj`.

The most interesting files include

- `typed/core.clj` 
  - loads most other files
  - definitions of the main user-facing macros and functions
- `typed/check.clj` 
  - the main checking algorithm (`check`)
  - (`typed/check_cljs.clj` for ClojureScript)
- `typed/infer.clj`
  - type variable inference algorithm (`infer`)
- `typed/subtype.clj`
  - subtyping (`subtype`)
- `typed/ann.clj`
  - base annotations on vars and methods
  - base type aliases
- `typed/alter.clj`
  - definitions of parameterised Java classes (like `clojure.lang.Seqable)

# Low hanging fruit (Contributions needed)

The two main activities crucial in these early versions of Typed Clojure are
porting existing untyped code to typed and typing core Clojure functions.
This process will reveal bugs in type checker and expressiveness limitations of Typed Clojure types.
For example, currently keyword parameters are not supported.

The screencasts porting `clojure.set` demonstrate some of the techniques involved, these are recommended watching.
Often if a core function is untyped, the type can be temporarily added via your own namespace
with `ann`,
and added to a future version of Typed Clojure.
This also applies to Clojure's `clojure.lang` Java classes (`alter-class`) and methods
(`override-method`, `non-nil-return`, `nilable-param`) (raw method calls
often result from inlining core functions).

Porting a namespace involves 

- annotating var `def`s from the current namespace with `ann`, conventionally above the `def`
- checking the namespace with `check-ns` (calling with zero arguments checks the current namespace)
- adding missing core Clojure annotations with `ann` temporarily in the current namespace
- wrapping troublesome top-level forms with `tc-ignore` so they skip type-checking

Core Clojure function annotations belong in the `typed/ann.clj` file.
They should be fully namespace qualified, like `(ann clojure.core/*ns* clojure.lang.Namespace)`.

# Future work

* Equality filters for occurrence typing
* Type check multimethods
* Rest type checking in fn definition
* Type check defprotocol definitions
* Unify AST with ClojureScript
* Namespace dependency management

# Limitations

## Namespace management

Typed dependencies NYI.

## Destructuring

Only map destructuring *without* options is supported.

Other forms of destructuring require equality filters.

## Dotted Functions

A dotted function contains a dotted variable in its function type.

eg. map's type: 
     `(All [c a b ...]
           [[a b ... b -> c] (U nil (Seqable a)) (U nil (Seqable b)) ... b -> (Seqable c)]))`

Currently Typed Clojure does not support *any* checking of use or definition of
dotted functions, only syntax to define its type.

## Rest Arguments

Currently cannot check the definition of functions with rest arguments,
but usage checking should work.

## Using `filter`

Not everything can be inferred from a `filter`. A common example is
`(filter identity coll)` does not work. The reason is `identity` only
gives negative information when its result is true: that the argument is *not*
`(U nil false)`.

This idiom must be converted to this syntax `(fn [a] a)` and then annotated with
positive propositions.

```clojure
;eg. 

(filter (ann-form (fn [a] a)
                  [(U nil Number) -> (U nil Number) :filters {:then (is Number 0)}])
        [1 nil 2])
; :- (Seqable Number)
```

Positive information infers just fine, like `(filter number? coll)`.
The above idiom is useful when you are filtering something like a `(Seqable (U nil x))` and there is no
predicate to test for `x`, so you can only test if something isn't `nil`.

# Usage

## Type Syntax

Rough grammar.

```
Type :=  nil
     |   true
     |   false
     |   (U Type*)
     |   (I Type+)
     |   FunctionIntersection
     |   (Value CONSTANT-VALUE)
     |   (Rec [Symbol] Type)
     |   (All [Symbol+] Type)
     |   (All [Symbol* Symbol ...] Type)
     |   (HMap {Keyword Type*})        ;eg (HMap {:a (Value 1), :b nil})
     |   '{Keyword Type*}              ;eg '{:a (Value 1), :b nil}
     |   (Vector* Type*)
     |   '[Type*]
     |   (Seq* Type*)
     |   (List* Type*)
     |   Symbol  ;class/protocol/free resolvable in context

FunctionIntersection :=  ArityType
                     |   (Fn ArityType+)

ArityType :=   [FixedArgs -> Type]
           |   [FixedArgs RestArgs * -> Type]
           |   [FixedArgs DottedType ... Symbol -> Type]

FixedArgs := Type*
RestArgs := Type
DottedType := Type
```

### Special constants

`nil`, `true` and `false` resolve to the respective singleton types for those values

### Intersections

`(I Type+)` creates an intersection of types.

### Unions

`(U Type*)` creates a union of types.

### Functions

A function type is an ordered intersection of arity types.

There is a vector sugar for functions of one arity.

### Heterogeneous Maps

`(HMap {:a (Value 1)})` is a IPersistentMap type that contains at least an `:a`
key with value `(Value 1)`.

### Heterogeneous Vectors

`(Vector* (Value 1) (Value 2))` is a IPersistentVector of length 2, essentially 
representing the value `[1 2]`.

### Polymorphism

The binding form `All` introduces a number of free variables inside a scope.

Optionally scopes a dotted variable by adding `...` after the last symbol in the binder.

eg. The identity function: `(All [x] [x -> x])`
eg. Introducing dotted variables: `(All [x y ...] [x y ... y -> x])

### Recursive Types

`Rec` introduces a recursive type. It takes a vector of one symbol and a type.
The symbol is scoped to represent the entire type in the type argument.

```clojure
; Type for {:op :if
            :test {:op :var, :var #'A}
            :then {:op :nil}
            :else {:op :false}}
(Rec [x] 
     (U (HMap {:op (Value :if)
               :test x
               :then x
               :else x})
        (HMap {:op (Value :var)
               :var clojure.lang.Var})
        (HMap {:op (Value :nil)})
        (HMap {:op (Value :false)})))))
```

## Anonymous Functions

`typed.core/fn>` defines a typed anonymous function.

```clojure
eg. (fn [a b] (+ a b))
=>
(fn> [[a :- Number]
       [b :- Number]]
   (+ a b))
```

## Annotating vars

`typed.core/ann` annotates vars. Var does not have to exist at usage.

If definition isn't type checked, it is assumed correct anyway for checking usages.

All used vars must be annotated when type checking.

## Annotating datatypes

`typed.core/ann-datatype` annotates datatypes. 

Takes a name and a vector of fieldname/type type entries.

```clojure
(ann-datatype Pair [[lhs :- Term]
                    [rhs :- Term]])

(deftype Pair [lhs rhs]
  ...)
```

## Annotating Protocols

`typed.core/ann-protocol` annotates protocols.

Takes a name and a optionally a :methods keyword argument mapping
method names to expected types.

Protocol definitions should use `typed.core/defprotocol>` (identical syntax to `defprotocol`).

```clojure
(ann-protocol IUnifyWithLVar
              :methods
              {unify-with-lvar [Term LVar ISubstitutions -> (U ISubstitutions Fail)]})

(defprotocol> IUnifyWithLVar
  (unify-with-lvar [v u s]))
```

## Type Aliases

`typed.core/def-alias` defines a type alias.

```clojure
(def-alias Term (I IUnifyTerms 
                   IUnifyWithNil
                   IUnifyWithObject
                   IUnifyWithLVar
                   IUnifyWithSequential
                   IUnifyWithMap
                   IUnifyWithSet
                   IReifyTerm
                   IWalkTerm
                   IOccursCheckTerm
                   IBuildTerm))
```

## Ignoring code

`typed.core/tc-ignore` tells the type checker to ignore any forms in the body.

```clojure
(tc-ignore
(defprotocol IUnifyTerms
  (unify-terms [u v s]))
)
```

## Declarations

`typed.core/declare-types`, `typed.core/declare-names` and `typed.core/declare-protocols` are similar
to `declare` in that they allow you to use types before they are defined.

```clojure
(declare-datatypes Substitutions)
(declare-protocols LVar)
(declare-names MyAlias)
```

## Checking typed namespaces

`typed.core/check-ns` checks the namespace that its symbol argument represents.

```clojure
(check-ns 'my.ns)
```

## Debugging

`typed.core/print-env` prints the current environment.

```clojure
(let [a 1]
  (print-env "Env:")
  a)
; Prints: "Env:" {:env {a (Value 1)},  ....}
```

`typed.core/cf` (pronounced "check form") can be used at the REPL to return the type of a form.

```clojure
(cf 1)
;=> [(Value 1) {:then [top-filter], :else [bot-filter]} empty-object]
```

## Macros & Macro Definitions

Macro definitions are ignored. The type checker operates on the macroexpanded form from
the Compiler's analysis phase.
