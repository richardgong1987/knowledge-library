% Run from programming-languages/prolog/labs with:
%   swipl -q -g main -t halt lists.pl
% The my_ names keep these teaching versions apart from library(lists).

% Level 2: member. Either X is the head, or X is a member of the tail.
my_member(X, [X|_]).
my_member(X, [_|T]) :- my_member(X, T).

% Level 3: append. The empty list in front changes nothing;
% otherwise the first element of the result is the first element of the front.
my_append([], L, L).
my_append([H|T], L, [H|R]) :- my_append(T, L, R).

% Level 5: naive reverse calls append once per element.
naive_reverse([], []).
naive_reverse([H|T], R) :-
    naive_reverse(T, RT),
    my_append(RT, [H], R).

% The accumulator version moves one element per step.
reverse_acc(List, Reversed) :- reverse_acc(List, [], Reversed).

reverse_acc([], Acc, Acc).
reverse_acc([H|T], Acc, R) :- reverse_acc(T, [H|Acc], R).

% Exactly Limit calls succeed and Limit - 1 calls are not enough.
costs_exactly(Goal, Limit) :-
    call_with_inference_limit(Goal, Limit, Result),
    Result \== inference_limit_exceeded,
    Fewer is Limit - 1,
    call_with_inference_limit(Goal, Fewer, inference_limit_exceeded).

level1_cells :-
    [1, 2, 3] = [H|T],
    assertion(H-T == 1-[2, 3]),
    assertion([1, 2, 3] == '[|]'(1, '[|]'(2, '[|]'(3, [])))),
    assertion([a, b] == [a|[b|[]]]),
    functor([1, 2, 3], Name, Arity),
    assertion(Name-Arity == '[|]'-2),
    assertion(\+ [] == '[]'),     % SWI-Prolog 7+: [] is not the atom '[]'
    assertion(\+ atom([])),
    assertion(\+ is_list([a|b])),
    assertion(\+ "abc" = [_|_]). % double quotes make a string, not a list

level2_member :-
    assertion(my_member(b, [a, b, c])),
    findall(X, my_member(X, [a, b, c]), Xs),
    assertion(Xs == [a, b, c]),
    findall(X, member(X, [a, b, c]), Library),
    assertion(Library == Xs).

level3_append :-
    my_append([1], [2, 3], L),
    assertion(L == [1, 2, 3]),
    findall(X+Y, my_append(X, Y, [1, 2, 3]), Splits),
    assertion(Splits == [[]+[1, 2, 3], [1]+[2, 3], [1, 2]+[3], [1, 2, 3]+[]]),
    assertion(length(Splits, 4)),
    my_append(Front, [3], [1, 2, 3]),
    assertion(Front == [1, 2]),
    my_append(_, [Last], [1, 2, 3]),
    assertion(Last == 3).

level4_length :-
    length([a, b, c], N),
    assertion(N == 3),
    length(Fresh, 2),
    assertion(Fresh = [_, _]),
    findall(Len, limit(3, length(_, Len)), Lens),
    assertion(Lens == [0, 1, 2]).

level5_reverse :-
    numlist(1, 30, L),
    naive_reverse(L, R1),
    reverse_acc(L, R2),
    reverse(L, R3),
    assertion((R1 == R2, R2 == R3)),
    assertion(costs_exactly(naive_reverse(L, _), 496)),
    assertion(costs_exactly(reverse_acc(L, _), 32)),
    assertion(costs_exactly(naive_reverse([1, 2, 3], _), 10)).

level6_index_and_sort :-
    nth0(1, [a, b, c], Zero),
    nth1(1, [a, b, c], One),
    assertion(Zero-One == b-a),
    msort([b, a, c, a], M),
    assertion(M == [a, a, b, c]),
    sort([b, a, c, a], S),
    assertion(S == [a, b, c]),
    sort(0, @>=, [3, 1, 2, 3], Desc),
    assertion(Desc == [3, 3, 2, 1]),
    sort(2, @>=, [item(apple, 3), item(pear, 5), item(fig, 3)], ByCount),
    assertion(ByCount == [item(pear, 5), item(apple, 3), item(fig, 3)]),
    msort([b, 2, "s", f(x), 1.0, V, a, 1], Ordered),
    assertion(Ordered == [V, 1.0, 1, 2, "s", a, b, f(x)]).

% Underneath: three memory cells per list element; append shares its second list.
memory_layout :-
    term_size([1, 2, 3], Cells),
    assertion(Cells == 9),
    Back = [3, 4],
    my_append([1, 2], Back, Joined),
    Joined = [_, _|Rest],
    assertion(same_term(Rest, Back)).

main :-
    level1_cells,
    level2_member,
    level3_append,
    level4_length,
    level5_reverse,
    level6_index_and_sort,
    memory_layout,
    format("OK~n").
