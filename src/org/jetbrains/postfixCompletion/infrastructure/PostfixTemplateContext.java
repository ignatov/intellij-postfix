package org.jetbrains.postfixCompletion.infrastructure;

import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PostfixTemplateContext {
  @NotNull public final PsiJavaCodeReferenceElement postfixReference;
  @NotNull public final PostfixExecutionContext executionContext;

  @NotNull private final PsiElement myExpression;
  @Nullable private List<PrefixExpressionContext> myExpressionContexts;

  public PostfixTemplateContext(
    @NotNull PsiJavaCodeReferenceElement reference, @NotNull PsiElement expression,
    @NotNull PostfixExecutionContext executionContext) {
    postfixReference = reference;
    myExpression = expression;
    this.executionContext = executionContext;
  }

  @NotNull public final PrefixExpressionContext innerExpression() {
    return expressions().get(0);
  }

  @NotNull public final PrefixExpressionContext outerExpression() {
    List<PrefixExpressionContext> expressions = expressions();
    return expressions.get(expressions.size() - 1);
  }

  @NotNull public final List<PrefixExpressionContext> expressions() {
    if (myExpressionContexts == null) {
      List<PrefixExpressionContext> contexts = buildExpressionContexts(postfixReference, myExpression);
      myExpressionContexts = Collections.unmodifiableList(contexts);
    }

    return myExpressionContexts;
  }

  @NotNull protected List<PrefixExpressionContext> buildExpressionContexts(
      @NotNull PsiElement reference, @NotNull PsiElement expression) {
    List<PrefixExpressionContext> contexts = new ArrayList<PrefixExpressionContext>();
    int referenceEndRange = reference.getTextRange().getEndOffset();

    // build expression contexts
    for (PsiElement node = expression; node != null; node = node.getParent()) {
      if (node instanceof PsiStatement) break;

      // handle only expressions, except 'reference'
      if ((node instanceof PsiExpression ||
           node instanceof PsiJavaCodeReferenceElement) && node != reference) {
        int endOffset = node.getTextRange().getEndOffset();
        if (endOffset > referenceEndRange) break; // stop when 'a.var + b'

        PrefixExpressionContext context = buildExpressionContext(node);
        contexts.add(context);

        if (context.canBeStatement) break;
      }
    }

    return contexts;
  }

  @NotNull protected PrefixExpressionContext buildExpressionContext(@NotNull PsiElement expression) {
    return new PrefixExpressionContext(this, expression);
  }

  @NotNull public abstract PrefixExpressionContext fixExpression(@NotNull PrefixExpressionContext context);

  @Nullable public PsiStatement getContainingStatement(@NotNull PrefixExpressionContext context) {
    // look for expression-statement parent
    PsiElement element = context.expression.getParent();

    // escape from '.postfix' reference-expression
    if (element == postfixReference) {
      // sometimes IDEA's code completion breaks expression in the middle into statement
      if (element instanceof PsiReferenceExpression) {
        // check we are invoked from code completion
        String referenceName = ((PsiReferenceExpression) element).getReferenceName();
        if (referenceName != null && referenceName.endsWith(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)) {
          PsiElement referenceParent = element.getParent(); // find separated expression-statement
          if (referenceParent instanceof PsiExpressionStatement) {
            PsiElement nextSibling = referenceParent.getNextSibling();
            if (nextSibling instanceof PsiExpressionStatement) { // find next expression-statement
              PsiExpression brokenExpression = ((PsiExpressionStatement) nextSibling).getExpression();
              // check next expression is likely broken invocation expression
              if (brokenExpression instanceof PsiParenthesizedExpression) return null; // foo;();
              if (brokenExpression instanceof PsiMethodCallExpression) return null;    // fo;o();
            }
          }
        }
      }

      element = element.getParent();
    }

    if (element instanceof PsiExpressionStatement) {
      return (PsiStatement) element;
    }

    return null;
  }

  @Nullable public String shouldFixPrefixMatcher() {
    return null;
  }
}