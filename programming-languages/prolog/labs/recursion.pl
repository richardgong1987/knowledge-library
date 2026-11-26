% Run from programming-languages/prolog/labs with:
%   swipl -q -g main -t halt recursion.pl

parent(pam, bob).
parent(tom, bob).
parent(tom, liz).
parent(bob, ann).
parent(bob, pat).
parent(pat, jim).

% Level 1: the base case first, then one step plus a smaller problem.
ancestor(X, Z) :- parent(X, Z).
ancestor(X, Z) :- parent(X, Y), ancestor(Y, Z).

% Level 2: Peano numbers. 0 is zero, s(N) is "one more than N".
nat(0).
nat(s(N)) :- nat(N).

add(0, Y, Y).
add(s(X), Y, s(Z)) :- add(X, Y, Z).

% Level 3: the same definition with the base case second.
nat_bad(s(N)) :- nat_bad(N).
nat_bad(0).

% Level 4: work after the recursive call keeps every frame alive.
sum_to(0, 0).
sum_to(N, S) :-
    N > 0,
    N1 is N - 1,
    sum_to(N1, S1),
    S is S1 + N.

% The accumulator version: the recursive call is the last goal.
sum_acc(0, S, S).
sum_acc(N, S0, S) :-
    N > 0,
    S1 is S0 + N,
    N1 is N - 1,
    sum_acc(N1, S1, S).

% Level 5: mutual recursion.
even(0).
even(s(N)) :- odd(N).

odd(s(N)) :- even(N).

level1_ancestor :-
    findall(A, ancestor(A, jim), Ancestors),
    assertion(Ancestors == [pat, pam, tom, bob]),
    findall(D, ancestor(tom, D), Descendants),
    assertion(Descendants == [bob, liz, ann, pat, jim]).

level2_peano :-
    add(s(s(0)), s(0), Sum),
    assertion(Sum == s(s(s(0)))),
    findall(X+Y, add(X, Y, s(s(0))), Splits),
    assertion(Splits == [0+s(s(0)), s(0)+s(0), s(s(0))+0]),
    once(findnsols(3, N, nat(N), Firsts)),
    assertion(Firsts == [0, s(0), s(s(0))]).

% A counting meta-interpreter: Steps is the number of resolution steps,
% one for every call of a predicate defined in this file.
solve(true, 0).
solve((A, B), Steps) :-
    solve(A, StepsA),
    solve(B, StepsB),
    Steps is StepsA + StepsB.
solve(Goal, Steps) :-
    Goal \= true,
    Goal \= (_, _),
    clause(Goal, Body),
    solve(Body, BodySteps),
    Steps is BodySteps + 1.

% Level 3: the measure of the first argument falls 2, 1, 0: exactly 3 calls.
level3_measure :-
    solve(add(s(s(0)), s(0), Sum), Steps),
    assertion(Sum-Steps == s(s(s(0)))-3).

level3_base_case_first :-
    assertion(nat_bad(s(s(0)))),  % checking a given number still works
    call_with_inference_limit(nat_bad(_), 10000, Result),
    assertion(Result == inference_limit_exceeded).

level4_last_call :-
    sum_to(4, S4),
    sum_acc(4, 0, A4),
    assertion(S4-A4 == 10-10),
    current_prolog_flag(stack_limit, Old),
    set_prolog_flag(stack_limit, 20_000_000),
    sum_acc(1_000_000, 0, Big),
    catch(sum_to(1_000_000, _), Error, true),
    set_prolog_flag(stack_limit, Old),
    assertion(Big == 500_000_500_000),
    assertion(Error = error(resource_error(stack), _)).

level5_mutual :-
    assertion(even(s(s(s(s(0)))))),
    assertion(odd(s(s(s(0))))),
    assertion(\+ even(s(0))).

% Level 6: induction. add(N, 0, N) holds for 0 and is carried from N to s(N).
induction_check :-
    once(findnsols(6, N, nat(N), Ns)),
    assertion(forall(member(M, Ns), add(M, 0, M))).

main :-
    level1_ancestor,
    level2_peano,
    level3_measure,
    level3_base_case_first,
    level4_last_call,
    level5_mutual,
    induction_check,
    format("OK~n").
