package com.mike.thisinsert;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;

import java.util.*;

/**
 * User: mjparmel
 * Date: Feb 27, 2009
 * Time: 3:04:37 PM
 */
public class ThisInserter implements Runnable {
    private static final Logger logger = Logger.getInstance(ThisInserter.class.getName());

    private Project project;
    private String topLevelClassName;

    private enum ReferenceType {
        MEMBER, METHOD
    }

    public ThisInserter(Project project) {
        this.project = project;
    }

    public void run() {
        Editor editor = FileEditorManager.getInstance(this.project).getSelectedTextEditor();
        if (editor != null) {
            PsiClass currentClass = this.getClassInCurrentEditor();
            if (currentClass != null) {
                this.topLevelClassName = currentClass.getName();
                this.addThis(currentClass, ReferenceType.MEMBER);
                this.addThis(currentClass, ReferenceType.METHOD);
            }
        }
    }

    private void addThis(PsiClass currentClass, ReferenceType referenceType) {
        PsiElement[] elements = null;
        if (ReferenceType.MEMBER.equals(referenceType)) {
            elements = currentClass.getFields();
        } else if (ReferenceType.METHOD.equals(referenceType)) {
            elements = currentClass.getMethods();
        }

        List<PsiElement> allElements = new ArrayList<PsiElement>();
        allElements.addAll(Arrays.asList(elements));
        this.filterOutStaticElements(allElements);

        final List<PsiReferenceExpression> modify = new ArrayList<PsiReferenceExpression>();
        for (PsiElement element : allElements) {
            final Collection<PsiReference> references = ReferencesSearch.search(element).findAll();
            this.filterOutInnerClassReferences(references);

            //Filter out some special cases, add the rest to a list to have "this." added to them
            for (PsiReference psiReference : references) {
                final PsiElement referenceElement = psiReference.getElement();
                final String referenceText = referenceElement.getText();
                if (referenceElement instanceof PsiReferenceExpression) {
                    PsiReferenceExpression referenceExpression = (PsiReferenceExpression) referenceElement;
                    //1) Filter out any "this()" calls in constructors, would result in it looking like "this.this()"
                    //2) Make sure the reference is not qualified, i.e. not something like "this.reference", "person.reference"
                    if (!referenceExpression.isQualified() && !"this".equals(referenceText)) {
                        modify.add(referenceExpression);
                    }
                }
            }
        }

        this.addThisToReferences(modify);
    }

    private void addThisToReferences(List<PsiReferenceExpression> references) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(this.project);
        PsiElementFactory factory = psiFacade.getElementFactory();

        for (PsiReferenceExpression referenceExpression : references) {
            final PsiExpression newExpression = factory
                    .createExpressionFromText("this." + referenceExpression.getText(), referenceExpression);
            referenceExpression.replace(newExpression);
        }
    }

    private PsiClass getClassInCurrentEditor() {
        PsiClass currentClass = null;
        final Editor editor = FileEditorManager.getInstance(this.project).getSelectedTextEditor();
        if (editor != null) {
            final PsiJavaFile javaFile = (PsiJavaFile) PsiUtil.getPsiFileInEditor(editor, this.project);
            if (javaFile != null) {
                final PsiElement element = javaFile.findElementAt(editor.getCaretModel().getOffset());
                //final PsiElement element = javaFile.findElementAt(0);
                currentClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            }
        }

        return currentClass;
    }

    private void filterOutInnerClassReferences(Collection<PsiReference> elements) {
        for (Iterator<PsiReference> iterator = elements.iterator(); iterator.hasNext();) {
            PsiReference psiElement = iterator.next();
            if (this.isReferenceInInnerClass(psiElement)) {
                iterator.remove();
            }
        }
    }

    private void filterOutStaticElements(List<PsiElement> elements) {
        for (Iterator<PsiElement> iterator = elements.iterator(); iterator.hasNext();) {
            PsiElement psiElement = iterator.next();
            if (this.hasStaticModifier(psiElement)) {
                iterator.remove();
            }
        }
    }

    private boolean hasStaticModifier(PsiElement element) {
        boolean hasStaticModifier = false;
        PsiElement[] children = element.getChildren();
        for (PsiElement child : children) {
            if (child instanceof PsiModifierList && child.getText().contains("static")) {
                hasStaticModifier = true;
                break;
            } else {
                hasStaticModifier = this.hasStaticModifier(child);
            }
        }

        return hasStaticModifier;
    }

    private boolean isReferenceInInnerClass(PsiReference reference) {
        boolean inInnerClass = false;

        //If this usage is in an inner class the parent class will be the inner class name
        //and not the top level class name
        PsiClass parentClass = PsiTreeUtil.getParentOfType(reference.getElement(), PsiClass.class);
        if (parentClass != null) {
            final String parentClassName = parentClass.getName();
            inInnerClass = !this.topLevelClassName.equals(parentClassName);
        }

        return inInnerClass;
    }
}
