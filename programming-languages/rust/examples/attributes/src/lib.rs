// A minimal custom attribute macro: keep the marked item and add one function.
// Real macros should parse syntax, handle collisions, and report useful errors.
use proc_macro::TokenStream;

#[proc_macro_attribute]
pub fn add_label(_args: TokenStream, item: TokenStream) -> TokenStream {
    let generated: TokenStream =
        "fn label() -> &'static str { \"generated at compile time\" }"
            .parse()
            .expect("fixed Rust tokens");
    let mut output = item;
    output.extend(generated);
    output
}
