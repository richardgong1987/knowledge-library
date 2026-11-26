% Run from programming-languages/prolog/labs with: swipl -q -g main -t halt accumulators-and-difference-lists.pl
:- use_module(library(lists)).
:- use_module(library(apply)).

% Level 1: the sum is computed on the way back out of the recursion.
sum([], 0).
sum([X|Xs], S) :- sum(Xs, S0), S is S0 + X.

% Level 1: the accumulator carries the sum so far on the way in.
sum_acc(Xs, S) :- sum_acc(Xs, 0, S).

sum_acc([], S, S).
sum_acc([X|Xs], S0, S) :- S1 is S0 + X, sum_acc(Xs, S1, S).

% Level 2: length with an accumulator.
len(Xs, N) :- len(Xs, 0, N).

len([], N, N).
len([_|Xs], N0, N) :- N1 is N0 + 1, len(Xs, N1, N).

% Level 3: naive reverse appends at the end, again and again.
nrev([], []).
nrev([X|Xs], Rs) :- nrev(Xs, R0), append(R0, [X], Rs).

% Level 3: reverse with an accumulator pushes each element on the front.
rev(Xs, Rs) :- rev(Xs, [], Rs).

rev([], Rs, Rs).
rev([X|Xs], Acc, Rs) :- rev(Xs, [X|Acc], Rs).

% Count resolution steps: one for every call of a user-defined predicate.
steps(Goal, N) :- solve(Goal, 0, N).

solve(true, N, N) :- !.
solve((A, B), N0, N) :- !, solve(A, N0, N1), solve(B, N1, N).
solve(Goal, N, N) :- predicate_property(Goal, built_in), !, call(Goal).
solve(Goal, N0, N) :- clause(Goal, Body), N1 is N0 + 1, solve(Body, N1, N).

% Level 4: a difference list A-B is the list A without its tail B.
append_dl(A-B, B-C, A-C).

% Level 5: a queue is a difference list: take at the front, add at the hole.
empty_queue(Q-Q).
enqueue(X, Q-[X|T], Q-T).
dequeue(X, [X|Q]-T, Q-T).

% Underneath: does Goal succeed in a thread that may use only Bytes of stack?
fits_in_stack(Goal, Bytes) :-
    thread_create(catch(Goal, error(resource_error(_), _), fail), Id, [stack_limit(Bytes)]),
    thread_join(Id, Status),
    Status == true.

% Level 6: the step of an accumulator loop, as a goal for foldl/4.
add(X, S0, S) :- S is S0 + X.
push(X, Acc, [X|Acc]).

main :-
    check_sums,
    check_length,
    check_reverse_steps,
    check_difference_lists,
    check_queue,
    check_foldl,
    check_constant_stack,
    format("OK~n").

% Level 1: both sums give 6; only the accumulator version is a loop.
check_sums :-
    sum([1, 2, 3], S),
    assertion(S == 6),
    sum_acc([1, 2, 3], SA),
    assertion(SA == 6),
    numlist(1, 1000000, Million),
    sum_acc(Million, SM),
    assertion(SM =:= 1000000 * 1000001 // 2),
    catch(sum_acc([1, 2], _, _), error(Error, _), true),
    assertion(Error == instantiation_error).

% Level 2
check_length :-
    len([a, b, c], N),
    assertion(N == 3).

% Level 3: (n+1)(n+2)/2 resolution steps against n+2.
check_reverse_steps :-
    rev([1, 2, 3], R3),
    assertion(R3 == [3, 2, 1]),
    numlist(1, 30, L30),
    nrev(L30, Naive),
    rev(L30, Acc),
    assertion(Naive == Acc),
    steps(nrev(L30, _), Naive30),
    steps(rev(L30, _), Acc30),
    assertion(Naive30 =:= 31 * 32 // 2),
    assertion(Naive30 == 496),
    assertion(Acc30 == 32),
    numlist(1, 1000, L1000),
    steps(nrev(L1000, _), Naive1000),
    steps(rev(L1000, _), Acc1000),
    assertion(Naive1000 == 501501),
    assertion(Acc1000 == 1002).

% Level 4: appending difference lists is one unification.
check_difference_lists :-
    append_dl([1, 2|T1]-T1, [3, 4|T2]-T2, DL),
    assertion(DL = [1, 2, 3, 4|T2]-T2),
    DL = List-[],
    assertion(List == [1, 2, 3, 4]),
    steps(append_dl([a|U1]-U1, [b|U2]-U2, _), Steps),
    assertion(Steps == 1).

% Level 5: first in, first out.
check_queue :-
    empty_queue(Q0),
    enqueue(a, Q0, Q1),
    enqueue(b, Q1, Q2),
    dequeue(X1, Q2, Q3),
    enqueue(c, Q3, Q4),
    dequeue(X2, Q4, Q5),
    dequeue(X3, Q5, _),
    assertion([X1, X2, X3] == [a, b, c]),
    Q0 = Front0-Hole0,
    assertion(Front0 == Hole0),
    % Dequeuing from an empty queue succeeds with a "minus one" queue.
    empty_queue(E0),
    dequeue(Y, E0, E1),
    E1 = Front1-Hole1,
    assertion(Hole1 == [Y|Front1]).

% Level 6: foldl/4 is the accumulator loop, written once.
check_foldl :-
    foldl(add, [1, 2, 3], 0, Sum),
    assertion(Sum == 6),
    foldl(push, [1, 2, 3], [], Reversed),
    assertion(Reversed == [3, 2, 1]),
    foldl(plus, [1, 2, 3], 0, PlusSum),
    assertion(PlusSum == 6).

% Underneath: with the recursive call last, the stack does not grow with the list.
check_constant_stack :-
    numlist(1, 300000, Numbers),
    assertion(fits_in_stack(sum_acc(Numbers, _), 25000000)),
    assertion(\+ fits_in_stack(sum(Numbers, _), 25000000)).
