% Run from programming-languages/prolog/labs with: swipl -q -g main -t halt cut-and-negation.pl

% Level 1: the cut commits to the first color it meets.
color(red).
color(green).
color(blue).

first_color(C) :- color(C), !.

% Level 1: without cuts, every clause whose test passes gives an answer.
grade_all(Score, a) :- Score >= 90.
grade_all(Score, b) :- Score >= 80.
grade_all(_, c).

% With cuts, the first clause whose test passes is the only answer.
grade(Score, a) :- Score >= 90, !.
grade(Score, b) :- Score >= 80, !.
grade(_, c).

% Level 2: a green cut. Every clause states its full condition.
grade_green(Score, a) :- Score >= 90, !.
grade_green(Score, b) :- Score >= 80, Score < 90, !.
grade_green(Score, c) :- Score < 80.

% Level 2: bind the output after the cut, so a given output cannot skip the test.
grade_safe(Score, Grade) :- Score >= 90, !, Grade = a.
grade_safe(Score, Grade) :- Score >= 80, !, Grade = b.
grade_safe(_, c).

% Level 3: if-then-else says the same with no cut in sight.
grade_ite(Score, Grade) :-
    (   Score >= 90 -> Grade = a
    ;   Score >= 80 -> Grade = b
    ;   Grade = c
    ).

% Level 4: what cannot be proved is taken to be false.
bird(tweety).
bird(pingu).
penguin(pingu).

flies(B) :- bird(B), \+ penguin(B).

% Level 5: the same two goals in the other order.
flies_wrong(B) :- \+ penguin(B), bird(B).

% Level 6: dif/2 is a constraint, so it may come first.
parent(ann, bob).
parent(ann, cy).

sibling(X, Y) :- dif(X, Y), parent(P, X), parent(P, Y).

sibling_naf(X, Y) :- \+ X = Y, parent(P, X), parent(P, Y).

main :-
    check_cut,
    check_red_and_green_cuts,
    check_if_then_else,
    check_negation_as_failure,
    check_floundering,
    check_dif,
    check_vm_code,
    format("OK~n").

% Level 1: one answer instead of three.
check_cut :-
    findall(C, color(C), Colors),
    assertion(Colors == [red, green, blue]),
    findall(C, first_color(C), First),
    assertion(First == [red]),
    findall(G, grade_all(95, G), All),
    assertion(All == [a, b, c]),
    findall(G, grade(95, G), Cut95),
    assertion(Cut95 == [a]),
    findall(G, grade(85, G), Cut85),
    assertion(Cut85 == [b]).

% Level 2: the red cut gives a wrong yes; the green and safe versions do not.
check_red_and_green_cuts :-
    assertion(grade(95, c)),
    findall(S-G, (member(S, [95, 85, 70]), grade_green(S, G)), Green),
    assertion(Green == [95-a, 85-b, 70-c]),
    findall(S-G, (member(S, [95, 85, 70]), grade_safe(S, G)), Safe),
    assertion(Safe == Green),
    assertion(\+ grade_green(95, c)),
    assertion(\+ grade_safe(95, c)).

% Level 3: if-then-else and once/1 commit to the first solution of the condition.
check_if_then_else :-
    findall(S-G, (member(S, [95, 85, 70]), grade_ite(S, G)), Ite),
    assertion(Ite == [95-a, 85-b, 70-c]),
    assertion(\+ grade_ite(95, c)),
    once(color(Once)),
    assertion(Once == red),
    findall(X-Y, (member(X, [1, 2, 3]), X > 1 -> Y = X ; Y = none), Commit),
    assertion(Commit == [2-2]),
    findall(X, (member(X, [1, 2, 3]) *-> true ; X = none), Soft),
    assertion(Soft == [1, 2, 3]),
    assertion(forall(member(X, [1, 2, 3]), X > 0)).

% Level 4: negation as failure and the closed world.
check_negation_as_failure :-
    assertion(flies(tweety)),
    assertion(\+ flies(pingu)),
    findall(B, flies(B), Flyers),
    assertion(Flyers == [tweety]),
    assertion(\+ bird(sam)).

% Level 5: with B unbound, \+ penguin(B) asks "is there no penguin at all?".
check_floundering :-
    assertion(\+ flies_wrong(_)),
    assertion(flies_wrong(tweety)),
    assertion(\+ \+ penguin(_)).

% Level 6: dif/2 gives the same answers in either goal order; \+ X = a does not.
check_dif :-
    findall(X, (dif(X, a), member(X, [a, b, c])), DifFirst),
    findall(X, (member(X, [a, b, c]), dif(X, a)), DifLast),
    assertion(DifFirst == [b, c]),
    assertion(DifLast == [b, c]),
    findall(X, (\+ X = a, member(X, [a, b, c])), NafFirst),
    findall(X, (member(X, [a, b, c]), \+ X = a), NafLast),
    assertion(NafFirst == []),
    assertion(NafLast == [b, c]),
    findall(X-Y, sibling(X, Y), Siblings),
    assertion(Siblings == [bob-cy, cy-bob]),
    assertion(\+ sibling_naf(_, _)),
    assertion(sibling_naf(bob, cy)),
    % Underneath: dif/2 waits on every variable it needs.
    dif(f(P, b), f(a, Q)),
    copy_term([P, Q], [P1, Q1], Residual),
    assertion(Residual == [dif(f(P1, Q1), f(a, b))]),
    assertion(\+ (P = a, Q = b)),
    P = c,
    assertion(var(Q)).

% Underneath: the virtual-machine code of cut, \+ and if-then-else.
check_vm_code :-
    with_output_to(string(CutCode), vm_list(first_color)),
    assertion(sub_string(CutCode, _, _, _, "i_cut")),
    with_output_to(string(NotCode), vm_list(flies)),
    assertion(sub_string(NotCode, _, _, _, "c_not(1,'L1')")),
    assertion(sub_string(NotCode, _, _, _, "c_cut(1)")),
    assertion(sub_string(NotCode, _, _, _, "c_fail")),
    with_output_to(string(IteCode), vm_list(grade_ite)),
    assertion(sub_string(IteCode, _, _, _, "c_ifthenelse")).
