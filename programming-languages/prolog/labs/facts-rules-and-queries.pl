% Run from programming-languages/prolog/labs with:
%   swipl -q -g main -t halt facts-rules-and-queries.pl

% Level 1: facts. The family Ivan Bratko uses to open his book.
parent(pam, bob).
parent(tom, bob).
parent(tom, liz).
parent(bob, ann).
parent(bob, pat).
parent(pat, jim).

% Level 3: a rule. X is a grandparent of Z if X is a parent of some Y
% and that Y is a parent of Z.
grandparent(X, Z) :-
    parent(X, Y),
    parent(Y, Z).

% Level 4: two people are siblings if they share a parent and differ.
sibling(X, Y) :-
    parent(P, X),
    parent(P, Y),
    dif(X, Y).

level1_facts :-
    assertion(parent(tom, bob)),
    assertion(\+ parent(bob, tom)),
    assertion(\+ parent(liz, _)),
    findall(C, parent(tom, C), Children),
    assertion(Children == [bob, liz]).

level2_terms :-
    assertion(atom(tom)),
    assertion(atom('Tom Smith')),
    assertion(integer(42)),
    assertion(float(3.14)),
    assertion(var(_)),
    assertion(compound(parent(tom, bob))),
    functor(parent(tom, bob), Name, Arity),
    assertion(Name-Arity == parent-2).

level3_rules :-
    findall(W, grandparent(tom, W), TomsGrandchildren),
    assertion(TomsGrandchildren == [ann, pat]),
    findall(G, grandparent(G, jim), JimsGrandparents),
    assertion(JimsGrandparents == [bob]),
    findall(A-B, grandparent(A, B), Pairs),
    assertion(Pairs == [pam-ann, pam-pat, tom-ann, tom-pat, bob-jim]),
    assertion(length(Pairs, 5)).

level4_siblings :-
    findall(X-Y, sibling(X, Y), Pairs),
    assertion(Pairs == [bob-liz, liz-bob, ann-pat, pat-ann]).

% Underneath: a clause is itself a term, and a fact is a rule whose body is true.
clauses_are_terms :-
    clause(parent(pam, bob), FactBody),
    assertion(FactBody == true),
    clause(grandparent(_, _), RuleBody),
    assertion(RuleBody = (parent(_, _), parent(_, _))),
    Rule = (grandparent(X, Z) :- parent(X, Y), parent(Y, Z)),
    functor(Rule, Neck, 2),
    assertion(Neck == (:-)).

% Common mistakes: calling a predicate that does not exist raises an error.
unknown_predicate :-
    catch(parnet(tom, bob), Error, true),
    assertion(Error = error(existence_error(procedure, parnet/2), _)).

main :-
    level1_facts,
    level2_terms,
    level3_rules,
    level4_siblings,
    clauses_are_terms,
    unknown_predicate,
    format("OK~n").
