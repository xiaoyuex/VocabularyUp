/*
 *  Copyright 2009 Pokidov Dmitry.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package vocabularyup.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import vocabularyup.VocabularyApp;
import vocabularyup.exception.VocabularyNotFoundException;
import vocabularyup.model.xml.Article;
import vocabularyup.model.xml.Vocabulary;
import vocabularyup.util.ui.GridBagHelper;

/**
 * Dialog for change {@link vocabularyup.model.xml.Article}
 * @author dooman
 */
public class EditArticleDialog extends JDialog {
    private static final Logger log = Logger.getLogger(EditArticleDialog.class.getName());

    public static final String TRANSLATE_DELIMETER = ";";
    public static final String EXAMPLE_DELIMETER = "\n";

    private class EditAction implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateDialog()) {
                    String source = sourceEdit.getText();
                    String[] translates = translateEdit.getText().split(TRANSLATE_DELIMETER);
                    List<String> translatesList = new ArrayList<String>();
                    for (String t : translates) {
                        translatesList.add(t.trim());
                    }
                    String[] examples = examplesEdit.getText().split(EXAMPLE_DELIMETER);
                    List<String> examplesList = new ArrayList<String>();
                    for (String example : examples) {
                        examplesList.add(example.trim());
                    }
                    try {
                        if (article == null) {
                            VocabularyApp.getInstance().addArticle(source, translatesList, examplesList);
                        } else {
                            VocabularyApp.getInstance().changeArticle(article, source, translatesList, examplesList);
                        }
                        EditArticleDialog.this.dispose();
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, "Error add/change article", ex);
                        JOptionPane.showMessageDialog(EditArticleDialog.this,
                            "Error occured while add/change article: " + ex.getMessage(),
                            "Add article error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(EditArticleDialog.this,
                            "Please, fill source and select vocabulary",
                            "Add article error", JOptionPane.ERROR_MESSAGE);
                }
            }
    }

    private Article article = null;

    //UI
    private JTextField sourceEdit = new JTextField();
    private JTextField translateEdit = new JTextField();
    private JTextArea examplesEdit = new JTextArea();
    private JComboBox vocabulariesEdit = 
            new JComboBox(new Vector(VocabularyApp.getInstance().getVocabularies()));
    private JButton okButton = new JButton("Ok");
    private JButton cancelButton = new JButton("Cancel");
    //UI

    /**
     * Create dialog for add new article.
     */
    public EditArticleDialog() {
        this(null);
    }

    /**
     * Create new dialog for edit existing article.
     * @param article article for edit.
     */
    public EditArticleDialog(Article article) {
        super();
        this.article = article;
        setPreferredSize(new Dimension(400, 300));
        setLocationRelativeTo(null);
        setTitle("Add Article");
        init();
        initUI();
    }

    /**
     * Validate fields for article.
     * Source and translates must be not empty and vocabulary should be selected.
     * If fields are not correct, error message will be show at dialog's foter.
     * @return {@code true}, if dialog's fields are correct.
     */
    protected boolean validateDialog() {
        return !sourceEdit.getText().isEmpty() &&
                            vocabulariesEdit.getSelectedItem() != null;
    }

    protected void initUI() {

        JPanel rootPanel = new JPanel();
        GridBagHelper layout = new GridBagHelper(rootPanel);

        JLabel sourceLabel = new JLabel("Source(*):");
        layout.addLabelComponent(sourceLabel);

        layout.addFieldComponent(sourceEdit);

        JLabel translateLabel = new JLabel("Translates:");
        layout.addLabelComponent(translateLabel);

        layout.addFieldComponent(translateEdit);

        JLabel examplesLabel = new JLabel("Examples:");
        layout.addLabelComponent(examplesLabel);

        examplesEdit.setFocusTraversalKeysEnabled(true);
        JScrollPane examplesPane = new JScrollPane(examplesEdit);
        layout.addComponent(examplesPane, GridBagHelper.MULTIROW_FIELD_CONSTRAINT);

        layout.addLabelComponent(new JLabel("Vocabulary(*):"));
        layout.addFieldComponent(vocabulariesEdit);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(okButton);
        buttonPanel.add(Box.createHorizontalStrut(5));
        buttonPanel.add(cancelButton);

        layout.addFieldComponent(buttonPanel);

        add(rootPanel);
    }

    protected void init() {
        vocabulariesEdit.setSelectedItem(VocabularyApp.getInstance().getCurrentVocabulary());
        if (article == null) {
            vocabulariesEdit.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    try {
                        VocabularyApp.getInstance().setCurrentVocabulary((Vocabulary) e.getItem());
                    } catch (VocabularyNotFoundException ex) {
                        Logger.getLogger(EditArticleDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        } else {
            vocabulariesEdit.setEnabled(false);

            sourceEdit.setText(article.getSource());

            StringBuilder translates = new StringBuilder();
            Iterator<String> translatesIt = article.getTranslates().iterator();
            while (translatesIt.hasNext()) {
                translates.append(translatesIt.next());
                if (translatesIt.hasNext()) {
                    translates.append(TRANSLATE_DELIMETER);
                }
            }
            translateEdit.setText(translates.toString());

            StringBuilder examples = new StringBuilder();
            Iterator<String> examplesIt = article.getExamples().iterator();
            while (examplesIt.hasNext()) {
                examples.append(examplesIt.next());
                if (examplesIt.hasNext()) {
                    examples.append(EXAMPLE_DELIMETER);
                }
            }
            examplesEdit.setText(examples.toString());
        }

        okButton.setMnemonic(KeyEvent.VK_O);
        cancelButton.setMnemonic(KeyEvent.VK_C);
        okButton.addActionListener(new EditAction());

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditArticleDialog.this.dispose();
            }
        });
    }
}
