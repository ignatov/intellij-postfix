public class Foo {
    void m(String s) {
        s.isEmpty() || s.contains("asas").<caret>
    }
}