% Run from programming-languages/prolog/labs with:
%   swipl -q -g main -t halt resolution-and-backtracking.pl

parent(pam, bob).
parent(tom, bob).
parent(tom, liz).
parent(bob, ann).
parent(bob, pat).
parent(pat, jim).

% Level 1: one rule to resolve against.
grandparent(X, Z) :-
    parent(X, Y),
    parent(Y, Z).

% Level 3: the Byrd box. Each port is recorded as control passes it.
:- dynamic seen/2.

boxed(Goal) :-
    (   assertz(seen(call, Goal))
    ;   assertz(seen(fail, Goal)), fail
    ),
    call(Goal),
    (   assertz(seen(exit, Goal))
    ;   assertz(seen(redo, Goal)), fail
    ).

grandparent_traced(X, Z) :-
    boxed(parent(X, Y)),
    boxed(parent(Y, Z)).

% Level 4: the same logic with the goals in the other order.
grandparent_swapped(X, Z) :-
    parent(Y, Z),
    parent(X, Y).

edge(a, b).
edge(b, c).
edge(c, d).

% Right recursion: the base case first, the recursive call last.
path(X, Z) :- edge(X, Z).
path(X, Z) :- edge(X, Y), path(Y, Z).

% Left recursion: the first goal calls path_left again with the same X.
path_left(X, Z) :- path_left(X, Y), edge(Y, Z).
path_left(X, Z) :- edge(X, Z).

% Interview: moving the base case first is not enough.
path_left_base_first(X, Z) :- edge(X, Z).
path_left_base_first(X, Z) :- path_left_base_first(X, Y), edge(Y, Z).

% Level 5: tabling remembers answers, so left recursion terminates.
:- table path_tabled/2.
path_tabled(X, Z) :- path_tabled(X, Y), edge(Y, Z).
path_tabled(X, Z) :- edge(X, Z).

ports(Goal, Events) :-
    retractall(seen(_, _)),
    forall(Goal, true),
    findall(Port-G, seen(Port, G), Events).

level1_and_2_search_order :-
    findall(W, grandparent(tom, W), Ws),
    assertion(Ws == [ann, pat]).

level3_byrd_box :-
    ports(boxed(parent(tom, _)), Simple),
    assertion(Simple =@= [call-parent(tom, _), exit-parent(tom, bob),
                          redo-parent(tom, bob), exit-parent(tom, liz),
                          redo-parent(tom, liz), fail-parent(tom, _)]),
    ports(grandparent_traced(tom, _), Events),
    assertion(Events =@= [call-parent(tom, _),   exit-parent(tom, bob),
                          call-parent(bob, _),   exit-parent(bob, ann),
                          redo-parent(bob, ann), exit-parent(bob, pat),
                          redo-parent(bob, pat), fail-parent(bob, _),
                          redo-parent(tom, bob), exit-parent(tom, liz),
                          call-parent(liz, _),   fail-parent(liz, _),
                          redo-parent(tom, liz), fail-parent(tom, _)]),
    assertion(length(Events, 14)).

level3_choice_points :-
    call_with_inference_limit(parent(tom, liz), 1000, Last),
    assertion(Last == !),         % no choice point left
    call_with_inference_limit(parent(tom, _), 1000, First),
    assertion(First == true).     % a choice point remains for liz

level4_goal_order :-
    findall(A-B, grandparent(A, B), InOrder),
    assertion(InOrder == [pam-ann, pam-pat, tom-ann, tom-pat, bob-jim]),
    findall(A-B, grandparent_swapped(A, B), Swapped),
    assertion(Swapped == [pam-ann, tom-ann, pam-pat, tom-pat, bob-jim]),
    msort(InOrder, Sorted),
    assertion(msort(Swapped, Sorted)).

level4_left_recursion :-
    findall(W, path(a, W), Ws),
    assertion(Ws == [b, c, d]),
    call_with_inference_limit(path_left(a, d), 100000, Result),
    assertion(Result == inference_limit_exceeded).

base_case_first_is_not_enough :-
    once(path_left_base_first(a, First)),
    assertion(First == b),        % answers do come out ...
    call_with_inference_limit(path_left_base_first(a, e), 100000, Result),
    assertion(Result == inference_limit_exceeded). % ... but "no" never does

tabling_fixes_left_recursion :-
    findall(W, path_tabled(a, W), Ws),
    msort(Ws, Sorted),            % tabled answers come in no fixed order
    assertion(Sorted == [b, c, d]),
    assertion(\+ path_tabled(a, e)).

main :-
    level1_and_2_search_order,
    level3_byrd_box,
    level3_choice_points,
    level4_goal_order,
    level4_left_recursion,
    base_case_first_is_not_enough,
    tabling_fixes_left_recursion,
    format("OK~n").
