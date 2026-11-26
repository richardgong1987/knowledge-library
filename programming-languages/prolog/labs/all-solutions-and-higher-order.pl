% Run from programming-languages/prolog/labs with: swipl -q -g main -t halt all-solutions-and-higher-order.pl
:- use_module(library(apply)).
:- use_module(library(aggregate)).
:- use_module(library(yall)).

% Who owns which pet. Ann owns two pets; Ann and Cy both own a cat.
owns(ann, cat).
owns(ann, dog).
owns(bob, fish).
owns(cy, cat).

age(ann, 31).
age(bob, 25).
age(cy, 40).

% Level 5: a goal with its last arguments missing, for call/N.
double(X, Y) :- Y is 2 * X.

% Level 7: a lambda in a file names the outer variables it shares in {}.
add_all(N, Xs, Ys) :- maplist({N}/[X, Y]>>(Y is X + N), Xs, Ys).

add_all_wrong(N, Xs, Ys) :- maplist([X, Y]>>(Y is X + N), Xs, Ys).

main :-
    check_findall,
    check_bagof,
    check_setof,
    check_aggregate_all,
    check_call,
    check_list_closures,
    check_lambdas,
    format("OK~n").

% Level 1: findall/3 keeps order and duplicates; no solutions give [].
check_findall :-
    findall(P, owns(_, P), Pets),
    assertion(Pets == [cat, dog, fish, cat]),
    findall(P, owns(zoe, P), None),
    assertion(None == []),
    findall(O-P, owns(O, P), Pairs),
    assertion(Pairs == [ann-cat, ann-dog, bob-fish, cy-cat]),
    % Underneath: the results are copies, so their variables are fresh.
    findall(X-_, member(X, [1, 2]), Copies),
    Copies = [1-Y1, 2-Y2],
    assertion((var(Y1), var(Y2), Y1 \== Y2)).

% Level 2: bagof/3 groups by the free variable O; ^ hides it.
check_bagof :-
    findall(O-Ps, bagof(P, owns(O, P), Ps), Groups),
    assertion(Groups == [ann-[cat, dog], bob-[fish], cy-[cat]]),
    bagof(P, O^owns(O, P), AllPets),
    assertion(AllPets == [cat, dog, fish, cat]),
    assertion(\+ bagof(P, owns(zoe, P), _)),
    catch(call(O^owns(O, cat)), error(Error, _), true),
    assertion(Error == existence_error(procedure, (^)/2)).

% Level 3: setof/3 sorts and removes duplicates.
check_setof :-
    setof(P, O^owns(O, P), Kinds),
    assertion(Kinds == [cat, dog, fish]),
    setof(O, owns(O, cat), CatOwners),
    assertion(CatOwners == [ann, cy]),
    setof(O, P^owns(O, P), Owners),
    assertion(Owners == [ann, bob, cy]).

% Level 4: aggregate_all/3 counts, sums and finds the maximum.
check_aggregate_all :-
    aggregate_all(count, owns(_, _), Count),
    assertion(Count == 4),
    aggregate_all(count, owns(zoe, _), Zero),
    assertion(Zero == 0),
    aggregate_all(sum(A), age(_, A), Sum),
    assertion(Sum == 96),
    aggregate_all(max(A), age(_, A), Max),
    assertion(Max == 40),
    aggregate_all(max(A, Who), age(Who, A), Oldest),
    assertion(Oldest == max(40, cy)),
    assertion(\+ aggregate_all(max(A), age(zoe, A), _)).

% Level 5: call/N appends arguments to a closure.
check_call :-
    call(double, 3, D),
    assertion(D == 6),
    Closure = double(4),
    call(Closure, D4),
    assertion(D4 == 8),
    call(plus(1), 2, P3),
    assertion(P3 == 3).

% Level 6: maplist, foldl, include, exclude and partition.
check_list_closures :-
    assertion(maplist(integer, [1, 2, 3])),
    maplist(double, [1, 2, 3], Doubles),
    assertion(Doubles == [2, 4, 6]),
    maplist(plus, [1, 2], [10, 20], Sums),
    assertion(Sums == [11, 22]),
    maplist(plus(1), [1, 2, 3], Next),
    assertion(Next == [2, 3, 4]),
    catch(maplist(double, _, [2, 4, 6]), error(Error, _), true),
    assertion(Error == instantiation_error),
    foldl(plus, [1, 2, 3], 0, Total),
    assertion(Total == 6),
    foldl([X, Y, S0, S]>>(S is S0 + X * Y), [1, 2, 3], [4, 5, 6], 0, Dot),
    assertion(Dot == 32),
    include([X]>>(X > 2), [1, 2, 3, 4], Big),
    assertion(Big == [3, 4]),
    exclude([X]>>(X > 2), [1, 2, 3, 4], Small),
    assertion(Small == [1, 2]),
    partition([X]>>(X > 2), [1, 2, 3, 4], In, Out),
    assertion(In-Out == [3, 4]-[1, 2]).

% Level 7: yall lambdas; outer variables must be declared in {} in a file.
check_lambdas :-
    maplist([X, Y]>>(Y is X * X), [1, 2, 3], Squares),
    assertion(Squares == [1, 4, 9]),
    add_all(10, [1, 2], Added),
    assertion(Added == [11, 12]),
    catch(add_all_wrong(10, [1, 2], _), error(Error, _), true),
    assertion(Error == instantiation_error),
    assertion(maplist([X]>>(X = Z), [1, 1])),
    assertion(var(Z)),
    maplist({Shared}/[X]>>(X = Shared), [1, 1]),
    assertion(Shared == 1),
    % Underneath: the compiled lambda helper receives only the {} variables.
    with_output_to(string(Wrong), listing(add_all_wrong)),
    assertion(sub_string(Wrong, _, _, _, "add_all_wrong(_, A, B)")),
    with_output_to(string(Right), listing(add_all)),
    assertion(sub_string(Right, _, _, _, "'(A),")).
