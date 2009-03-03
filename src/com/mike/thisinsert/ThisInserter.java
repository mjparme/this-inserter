package com.mike.thisinsert;

import com.intellij.openapi.actionSystem.DataContext;
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

    private DataContext dataContext;
    private Project project;

    public ThisInserter(DataContext dataContext, Project project) {
        this.dataContext = dataContext;
        this.project = project;
    }

    public void run() {
        final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            final Document document = editor.getDocument();
            final PsiJavaFile javaFile = (PsiJavaFile) PsiUtil.getPsiFileInEditor(editor, project);
            if (javaFile != null) {
                final PsiElement element = javaFile.findElementAt(editor.getCaretModel().getOffset());

                PsiClass currentClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                if (currentClass != null) {
                    String topLevelClassName = currentClass.getName();
                    final List<PsiField> allFields = this.filterOutStaticFields(currentClass.getFields());
                    for (PsiField field : allFields) {
                        final Collection<PsiReference> references = ReferencesSearch.search(field).findAll();
                        for (PsiReference reference : references) {
                            final PsiElement referenceElement = reference.getElement();
                            if (!isElementInInnerClass(referenceElement, topLevelClassName) && !referenceElement.getText().startsWith("this.")) {
                                int offset = referenceElement.getTextOffset();
                                document.insertString(offset, "this.");
                                PsiDocumentManager.getInstance(project).commitDocument(document);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<PsiField> filterOutStaticFields(PsiField[] allFields) {
        List<PsiField> nonStatic = new ArrayList<PsiField>();
        for (PsiField psiField : Arrays.asList(allFields)) {
            if (!hasStaticModifier(psiField)) {
                nonStatic.add(psiField);
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
