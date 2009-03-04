package com.mike.thisinsert;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: mjparmel
 * Date: Feb 27, 2009
 * Time: 3:04:37 PM
 */
public class ThisInserter implements Runnable {
    private static final Logger logger = Logger.getInstance(ThisInserter.class.getName());

    private Project project;

    private enum ReferenceType {
        FIELD, METHOD
    }

    public ThisInserter(Project project) {
        this.project = project;
    }

    public void run() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            final Document document = editor.getDocument();
            PsiClass currentClass = this.getClassInCurrentEditor();
            if (currentClass != null) {
                String topLevelClassName = currentClass.getName();
                addThis(document, currentClass, topLevelClassName, ReferenceType.FIELD);
                addThis(document, currentClass, topLevelClassName, ReferenceType.METHOD);
            }
        }
    }

    private void addThis(Document document, PsiClass currentClass, String topLevelClassName, ReferenceType referenceType) {
        List<PsiElement> allElements = null;
        if (ReferenceType.FIELD.equals(referenceType)) {
            allElements = this.filterOutStaticFields(currentClass.getFields());
        } else if (ReferenceType.METHOD.equals(referenceType)) {
            allElements = this.filterOutStaticFields(currentClass.getMethods());
        }

        if (allElements != null) {
            for (PsiElement element : allElements) {
                final Collection<PsiReference> references = ReferencesSearch.search(element).findAll();
                this.addThisToReferences(document, topLevelClassName, references);
            }
        }
    }

    private void addThisToReferences(Document document, String topLevelClassName, Collection<PsiReference> references) {
        for (PsiReference reference : references) {
            final PsiElement element = reference.getElement();
            if (!isElementInInnerClass(element, topLevelClassName) && !element.getText().startsWith("this.")) {
                int offset = element.getTextOffset();
                document.insertString(offset, "this.");
                PsiDocumentManager.getInstance(project).commitDocument(document);
            }
        }
    }

    private PsiClass getClassInCurrentEditor() {
        PsiClass currentClass = null;
        final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            final PsiJavaFile javaFile = (PsiJavaFile) PsiUtil.getPsiFileInEditor(editor, project);
            if (javaFile != null) {
                final PsiElement element = javaFile.findElementAt(editor.getCaretModel().getOffset());
                currentClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            }
        }

        return currentClass;
    }

    private List<PsiElement> filterOutStaticFields(PsiElement[] elements) {
        List<PsiElement> nonStatic = new ArrayList<PsiElement>();
        for (PsiElement psiElement : Arrays.asList(elements)) {
            if (!hasStaticModifier(psiElement)) {
                nonStatic.add(psiElement);
            }
        }

        return nonStatic;
    }

    private boolean hasStaticModifier(PsiElement element) {
        boolean hasStaticModifier = false;
        PsiElement[] children = element.getChildren();
        for (PsiElement child : children) {
            if (child instanceof PsiModifierList && child.getText().contains("static")) {
                hasStaticModifier = true;
                break;
            } else {
                hasStaticModifier = hasStaticModifier(child);
            }
        }

        return hasStaticModifier;
    }

    private boolean isElementInInnerClass(PsiElement element, String topLevelClassName) {
        boolean inInnerClass = false;

        //If this usage is in an inner class the parent class will be the inner class name
        //and not the top level class name
        PsiClass parentClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (parentClass != null) {
            final String parentClassName = parentClass.getName();
            inInnerClass = !topLevelClassName.equals(parentClassName);
        }

        return inInnerClass;
    }
}
