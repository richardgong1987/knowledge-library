use tiny_attributes::add_label;

// The macro leaves this function intact and generates fn label() beside it.
#[add_label]
fn greet() -> &'static str {
    "hello"
}

fn main() {
    assert_eq!(greet(), "hello");
    assert_eq!(label(), "generated at compile time");
    println!("{}: {}", greet(), label());
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn custom_attribute_generated_a_function() {
        assert_eq!(label(), "generated at compile time");
    }
}
