// Items: arg, cast, for, not, par, var
public class Foo {
    void m() {
        int foo = 2;
        int i = foo<caret> + 2 /**/ ;
    }
}