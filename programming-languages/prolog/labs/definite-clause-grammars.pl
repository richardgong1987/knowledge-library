% Run from programming-languages/prolog/labs with: swipl -q -g main -t halt definite-clause-grammars.pl
:- use_module(library(dcg/basics)).

% Level 1: a greeting is the word hello followed by a name.
greeting --> [hello], who.

who --> [world].
who --> [prolog].

% Level 3: balanced parentheses, read as character codes.
parens --> [].
parens --> "(", parens, ")", parens.

% Level 4: n copies of a, then n copies of b.
anbn --> [].
anbn --> [a], anbn, [b].

% Level 5: + and - at the top, * below, so * binds tighter.
expr(V) --> term(T), expr_rest(T, V).

expr_rest(Acc, V) --> "+", term(T), { Acc1 is Acc + T }, expr_rest(Acc1, V).
expr_rest(Acc, V) --> "-", term(T), { Acc1 is Acc - T }, expr_rest(Acc1, V).
expr_rest(V, V) --> [].

term(V) --> factor(F), term_rest(F, V).

term_rest(Acc, V) --> "*", factor(F), { Acc1 is Acc * F }, term_rest(Acc1, V).
term_rest(V, V) --> [].

factor(V) --> integer(V).
factor(V) --> "(", expr(V), ")".

% Interview: left recursion calls itself before reading any input.
lexpr(V) --> lexpr(A), "-", integer(B), { V is A - B }.
lexpr(V) --> integer(V).

% Interview: right recursion terminates but groups 10-(2-3).
rexpr(V) --> integer(A), "-", rexpr(B), { V is A - B }.
rexpr(V) --> integer(V).

% Level 6: a tiny grammar of English.
sentence --> noun_phrase, verb_phrase.
noun_phrase --> determiner, noun.
verb_phrase --> verb, noun_phrase.

determiner --> [the].
determiner --> [a].
noun --> [cat].
noun --> [dog].
verb --> [sees].
verb --> [chases].

calc(Text, V) :-
    string_codes(Text, Codes),
    phrase(expr(V), Codes).

balanced(Text) :-
    string_codes(Text, Codes),
    phrase(parens, Codes).

main :-
    check_phrase,
    check_translation,
    check_parens,
    check_anbn,
    check_calc,
    check_recursion_direction,
    check_generation,
    check_mistakes,
    format("OK~n").

% Level 1: phrase/2 runs a grammar on a list, in either direction.
check_phrase :-
    assertion(phrase(greeting, [hello, world])),
    assertion(\+ phrase(greeting, [hello, there])),
    findall(G, phrase(greeting, G), Greetings),
    assertion(Greetings == [[hello, world], [hello, prolog]]),
    phrase(greeting, [hello, world, and, more], Rest),
    assertion(Rest == [and, more]).

% Level 2: the translation adds two list arguments.
check_translation :-
    clause(greeting(In, Out), Greeting),
    assertion(In-Greeting =@= [hello|A]-who(A, Out)),
    dcg_translate_rule((anbn --> [a], anbn, [b]), Clause),
    assertion(Clause =@= (anbn(S0, S) :- S0 = [a|S1], anbn(S1, S2), S2 = [b|S])),
    clause(anbn(Start, End), (Call, Unify)),
    assertion(Start-Call-Unify =@= [a|Mid]-anbn(Mid, Rest)-(Rest = [b|End])).

% Level 3: balanced parentheses; 5 balanced strings of 3 pairs.
check_parens :-
    assertion(balanced("(()())")),
    assertion(\+ balanced("(()")),
    assertion(\+ balanced(")(")),
    findall(A, (length(Cs, 6), phrase(parens, Cs), atom_codes(A, Cs)), Six),
    assertion(Six == ['()()()', '()(())', '(())()', '(()())', '((()))']).

% Level 4: a^n b^n, and the pumped string a^4 b^3 is rejected.
check_anbn :-
    assertion(phrase(anbn, [a, a, a, b, b, b])),
    assertion(\+ phrase(anbn, [a, a, a, a, b, b, b])),
    assertion(\+ phrase(anbn, [a, b, a, b])),
    findall(L, (between(0, 6, N), length(L, N), phrase(anbn, L)), Words),
    assertion(Words == [[], [a, b], [a, a, b, b], [a, a, a, b, b, b]]).

% Level 5: precedence, left associativity and parentheses.
check_calc :-
    calc("2+3*4", V1),
    assertion(V1 == 14),
    calc("(2+3)*4", V2),
    assertion(V2 == 20),
    calc("10-2-3", V3),
    assertion(V3 == 5),
    assertion(\+ calc("2+", _)).

% Interview: right recursion groups the wrong way; left recursion never stops.
check_recursion_direction :-
    string_codes("10-2-3", Minus),
    once(phrase(rexpr(Right), Minus)),
    assertion(Right == 11),
    call_with_inference_limit(phrase(lexpr(_), Minus), 100000, Limit),
    assertion(Limit == inference_limit_exceeded).

% Level 6: running the grammar backwards generates 2*2*2*2*2 sentences.
check_generation :-
    findall(S, phrase(sentence, S), Sentences),
    length(Sentences, Count),
    assertion(Count == 32),
    Sentences = [First, Second, Third|_],
    assertion(First == [the, cat, sees, the, cat]),
    assertion(Second == [the, cat, sees, the, dog]),
    assertion(Third == [the, cat, sees, a, cat]),
    assertion(phrase(sentence, [a, dog, chases, the, cat])).

% Common mistakes: a goal without {} becomes a nonterminal; built-in names are taken.
check_mistakes :-
    dcg_translate_rule((t(V) --> [X], V is X + 1), (_ :- Body)),
    assertion(sub_term(is(_, _, _, _), Body)),
    dcg_translate_rule((atom_length --> [x]), Clash),
    catch(assertz(Clash), error(ClashError, _), true),
    assertion(ClashError == permission_error(modify, static_procedure, atom_length/2)),
    catch(phrase(parens, "(())"), error(TypeError, _), true),
    assertion(TypeError == type_error(list, "(())")).
