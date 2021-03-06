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

package vocabularyup.model.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import vocabularyup.VocabularyApp;
import vocabularyup.exception.ArticleAlreadyExistException;
import vocabularyup.exception.VocabularyAlreadyExistException;
import vocabularyup.exception.VocabularyModelException;
import vocabularyup.exception.VocabularyNotFoundException;
import vocabularyup.util.dom.DomCheckHelper;
import vocabularyup.util.dom.DomCheckingException;

/**
 * Vocabulary contains articles. Class realize DOM model for storing data.
 *
 * TODO: create interface and factory for different implementations.
 * @author 111
 */
public class Vocabulary {
    public static final String VOCABULARY_ELEMENT = "vocabulary";
    public static final String VOCABULARY_NAME_ATTR = "name";

    private static final Logger log = Logger.getLogger("XMLVocabulary");

    private Document document;
    private Element  element;
    private List<Article> articles = new ArrayList<Article>();

    private Vocabulary(Document document, Element element) {
        this.document = document;
        this.element = element;
        try {
            List<Element> articlesEl = DomCheckHelper.getElementsByTagName(element, Article.ARTICLE_ELEMENT, 0, false);
            for (Element el : articlesEl) {
                try {
                    articles.add(Article.loadArticle(document, el));
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Error loading article", e);
                }
            }
            log.log(Level.INFO, "Loaded [" + articles.size() + "] articles for vocabulary [" + getName() + "]");
        } catch (DomCheckingException e) {
            log.log(Level.WARNING, "No articles in vocabulary [" + getName() + "]");
        }
    }

    public String getName() {
        return element.getAttribute(VOCABULARY_NAME_ATTR);
    }

    public void setName(String name) {
        element.setAttribute(VOCABULARY_NAME_ATTR, name);
    }

    /**
     * Save vocabulary to file. Delete old file and create new, write DOM structure after creating.
     * @throws vocabularyup.exception.VocabularyModelException Error transformation DOM to File.
     */
    public void save() throws VocabularyModelException {
        String fileName = VocabularyApp.APP_HOME_DIR + "/" + getName() + ".xml";
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        log.log(Level.INFO, "Save vocabulary to " + fileName);
        try {
            if (file.createNewFile()) {
                DOMSource source = new DOMSource(element);
                TransformerFactory tFactory = TransformerFactory.newInstance();
                tFactory.setAttribute("indent-number", 4);
                Transformer t = tFactory.newTransformer();
                FileOutputStream fos = new FileOutputStream(file);
                t.setOutputProperty(OutputKeys.INDENT, "yes");
                t.transform(source, new StreamResult(fos));
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error saving vocabulary", e);
            throw new VocabularyModelException("Error save vocabulary " + getName(), e);
        }
    }

    /**
     * Add new article to vocabulary.
     * <b>If article with the same source already exist, it'll be replaced by new article</b>
     * @param source source word.
     * @param translates translates of new word.
     * @param examples usages of this word.
     * @return Created new article.
     * @throws ArticleAlreadyExistException when article with the same {@code source} already exists in the vocabulary.
     */
    public Article addArticle(String source, List<String> translates, List<String> examples) throws ArticleAlreadyExistException {
        if (getArticle(source) != null) {
            throw new ArticleAlreadyExistException("Article with source [" + source + "] already exists");
        }
        Article article = new Article(document, source, translates);
        if (examples != null && examples.size() > 0) {
            article.addExamples(examples);
        }

        articles.add(article);
        element.appendChild(article.buildArticleElement());

        return article;
    }

    /**
     * Return vocabularie's articles.
     * @return list of articles or empty list if vocabulary hasn't articles.
     */
    public List<Article> getArticles() {
        return articles;
    }

    /**
     * Return article with specified source.
     * @param source source of article
     * @return article or {@code null}, if article with this sourse does not exists
     */
    public Article getArticle(String source) {
        for (Article a : articles) {
            if (source.equals(a.getSource())) {
                return a;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Create new vocabulary with name - {@code name}.<br/>
     * Vocabulary's file will be create in user's home directory.
     * @param name vocabulary's name.
     * @return created vocabulary.
     * @throws VocabularyAlreadyExistException vocabulary with name - {@code name} already exist
     * @throws VocabularyModelException DOM error.
     */
    public static Vocabulary newVocabulary(String name) 
    throws VocabularyAlreadyExistException, VocabularyModelException {
        File file = new File(VocabularyApp.APP_HOME_DIR + "/" + name + ".xml");
        if (file.exists()) {
            throw new VocabularyAlreadyExistException(name);
        }
        Vocabulary vocabulary = null;
        try { //создаем дом-документ.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement(VOCABULARY_ELEMENT);
            root.setAttribute(VOCABULARY_NAME_ATTR, name);
            vocabulary = new Vocabulary(document, root);
        } catch (ParserConfigurationException pce) {
            throw new VocabularyModelException(name, pce);
        }

        return vocabulary;
    }

    /**
     * Load vocabulary with name - {@code name}.
     * @param file Source file.
     * @return loaded vocabulary
     * @throws vocabularyup.exception.VocabularyNotFoundException vocabulary with name - {@code name} doesnot exist.
     * @throws vocabularyup.exception.VocabularyModelException parse error
     */
    public static Vocabulary loadVocabulary(File file)
    throws VocabularyNotFoundException, VocabularyModelException {
        if (file ==null || !file.exists()) {
            throw new VocabularyNotFoundException(file.getName());
        }
        Vocabulary vocabulary = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            NodeList list = document.getElementsByTagName(VOCABULARY_ELEMENT);
            if (list.getLength() != 1) {
                throw new VocabularyModelException("More than one vocabulary element");
            }
            Element root = (Element) list.item(0);
            vocabulary = new Vocabulary(document, root);
        } catch(ParserConfigurationException e) {
            throw new VocabularyModelException(file.getName(), e);
        } catch(SAXException e) {
            throw new VocabularyModelException(file.getName(), e);
        } catch (IOException e) {
            throw new VocabularyModelException(file.getName(), e);
        }
        return vocabulary;
    }


}
